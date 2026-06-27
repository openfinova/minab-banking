package com.openfinova.banking.tan.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.customer.account.api.CustomerAccountService;
import com.openfinova.banking.customer.account.api.dto.AccountPayeeInfo;
import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.setup.api.DateTimeService;
import com.openfinova.banking.tan.config.TanProperties;
import com.openfinova.banking.tan.crypto.TanCodeService;
import com.openfinova.banking.tan.crypto.TanQrSigningService;
import com.openfinova.banking.tan.dto.PaymentQrResponse;
import com.openfinova.banking.tan.dto.PendingTransactionResponse;
import com.openfinova.banking.tan.dto.VerifyTanRequest;
import com.openfinova.banking.tan.dto.VerifyTanResponse;
import com.openfinova.banking.tan.entity.TanDevice;
import com.openfinova.banking.tan.entity.TanDeviceStatus;
import com.openfinova.banking.tan.entity.TanPendingAuthorization;
import com.openfinova.banking.tan.entity.TanPendingAuthorizationStatus;
import com.openfinova.banking.tan.exception.TanCodeAlreadyUsedException;
import com.openfinova.banking.tan.repository.TanDeviceRepository;
import com.openfinova.banking.tan.repository.TanPendingAuthorizationRepository;
import com.openfinova.banking.tp.api.TransactionProcessingService;
import com.openfinova.banking.tp.api.dto.TransactionResponse;
import com.openfinova.banking.tp.api.entity.TransactionStatus;

/**
 * Orchestrates Strong Customer Authentication (SCA) for payments via TAN.
 *
 * When a user initiates a payment that requires SCA, this service creates a
 * {@link TanPendingAuthorization} snapshot, exposes it to the TAN mobile app, validates
 * submitted TAN codes, and signals transaction processing (TP) to continue once SCA succeeds.
 *
 * Payment authorization flow
 *   Banking channel requests a signed payment QR ({@link #buildPaymentQr}) for an
 *   {@code INITIATED} TP transaction.
 *   The TAN app scans the QR and loads the pending payment details ({@link #getPendingTransaction}).
 *   The user confirms on the device; the channel submits the TAN ({@link #verifyTan}).
 *   On success, the pending row becomes {@code SCA_VERIFIED} and TP may process the payment.
 *
 * Cross-module dependencies: reads payment state from {@link TransactionProcessingService},
 * resolves payee display data from {@link CustomerAccountService}, and exposes SCA status to
 * other modules via {@link com.openfinova.banking.tan.api.TanService#isScaVerified}.
 *
 * @see TanPendingAuthorization
 * @see TanDevice
 * @see com.openfinova.banking.tan.TanServiceImpl
 */
@Service
public class TanAuthorizationService {

    private final TanPendingAuthorizationRepository pendingAuthorizationRepository;
    private final TanDeviceRepository tanDeviceRepository;
    private final TransactionProcessingService transactionProcessingService;
    private final CustomerAccountService customerAccountService;
    private final TanQrSigningService qrSigningService;
    private final TanCodeService tanCodeService;
    private final TanProperties properties;
    private final DateTimeService dateTimeService;

    public TanAuthorizationService(TanPendingAuthorizationRepository pendingAuthorizationRepository,
            TanDeviceRepository tanDeviceRepository, TransactionProcessingService transactionProcessingService,
            CustomerAccountService customerAccountService, TanQrSigningService qrSigningService,
            TanCodeService tanCodeService, TanProperties properties, DateTimeService dateTimeService) {
        this.pendingAuthorizationRepository = pendingAuthorizationRepository;
        this.tanDeviceRepository = tanDeviceRepository;
        this.transactionProcessingService = transactionProcessingService;
        this.customerAccountService = customerAccountService;
        this.qrSigningService = qrSigningService;
        this.tanCodeService = tanCodeService;
        this.properties = properties;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Creates or reuses a pending SCA record for the transaction and returns a signed QR payload
     * the user scans with the TAN app. The MAC prevents tampering with {@code txnId} in the QR URL.
     *
     * @param principal authenticated payer
     * @param txnId TP transaction awaiting SCA
     * @return QR URL and authorization expiry
     */
    @Transactional
    @PreAuthorize("hasAuthority('payment:initiate') or hasAuthority('payment:initiate:own')")
    public PaymentQrResponse buildPaymentQr(BankingPrincipal principal, UUID txnId) {
        TanPendingAuthorization pending = ensurePendingAuthorization(principal.userId(), txnId);
        String mac = qrSigningService.signTransactionId(txnId);
        String baseUrl = properties.getBaseUrl().replaceAll("/$", "");
        String qrPayload = baseUrl + "/authorize?txnId=" + txnId + "&mac=" + mac;
        return new PaymentQrResponse(qrPayload, pending.getExpiresAt());
    }

    /**
     * Returns payment details for display on the TAN app after the user scans the payment QR.
     * Expired pending authorizations are marked {@code EXPIRED} before the response is built.
     *
     * @param principal TAN app user (must match the pending authorization's {@code userId})
     * @param txnId transaction identifier from the scanned QR
     * @return amount, payee, and expiry for TAN code generation on the device
     * @throws IllegalStateException if the authorization is no longer {@code PENDING}
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('tan:generate')")
    public PendingTransactionResponse getPendingTransaction(BankingPrincipal principal, UUID txnId) {
        TanPendingAuthorization pending = loadPendingForUser(principal.userId(), txnId);
        expireIfNeeded(pending);
        if (pending.getStatus() != TanPendingAuthorizationStatus.PENDING) {
            throw new IllegalStateException("Transaction is not awaiting TAN authorization");
        }
        return toPendingResponse(pending);
    }

    /**
     * Verifies a TAN code for a pending payment and completes SCA when valid.
     *
     * Uses the user's most recently enrolled active device secret. The TAN must match the
     * pending {@code amount} and normalized {@code payeeIban}; replay of the same code is rejected.
     * Idempotent when the authorization is already {@code SCA_VERIFIED}. On first success, triggers
     * TP to process the payment if it is still {@code INITIATED}.
     *
     * @param principal authenticated payer submitting the TAN from the banking channel
     * @param request transaction id and TAN code from the user
     * @return verification outcome and timestamp
     * @throws TanCodeAlreadyUsedException if this TAN was already consumed for the transaction
     * @throws IllegalStateException if no active device exists or the authorization window closed
     */
    @Transactional
    @PreAuthorize("hasAuthority('payment:initiate') or hasAuthority('payment:initiate:own')")
    public VerifyTanResponse verifyTan(BankingPrincipal principal, VerifyTanRequest request) {
        TanPendingAuthorization pending = loadPendingForUser(principal.userId(), request.txnId());
        expireIfNeeded(pending);
        if (pending.getStatus() == TanPendingAuthorizationStatus.SCA_VERIFIED) {
            return new VerifyTanResponse(true, pending.getVerifiedAt());
        }
        if (pending.getStatus() != TanPendingAuthorizationStatus.PENDING) {
            throw new IllegalStateException("Transaction authorization expired or invalid");
        }

        TanDevice device = tanDeviceRepository
                .findFirstByUserIdAndStatusOrderByEnrolledAtDesc(principal.userId(), TanDeviceStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("No active TAN device enrolled"));

        byte[] secretBytes = Base64.getDecoder().decode(device.getTanSecret());
        String normalizedIban = TanCodeService.normalizeIban(pending.getPayeeIban());
        try {
            tanCodeService.verifyAndRecordReplay(
                    request.txnId(),
                    secretBytes,
                    request.tanCode(),
                    pending.getAmount(),
                    normalizedIban);
        } catch (TanCodeAlreadyUsedException e) {
            throw e;
        }

        Instant verifiedAt = dateTimeService.instant();
        pending.setStatus(TanPendingAuthorizationStatus.SCA_VERIFIED);
        pending.setVerifiedAt(verifiedAt);
        pending.setTanDeviceId(device.getId());
        pendingAuthorizationRepository.save(pending);

        device.setLastUsedAt(verifiedAt);
        tanDeviceRepository.save(device);

        if (TransactionStatus.INITIATED.name()
                .equals(transactionProcessingService.getTransactionStatus(request.txnId()))) {
            transactionProcessingService.processTransaction(request.txnId());
        }

        return new VerifyTanResponse(true, verifiedAt);
    }

    /**
     * Whether SCA has been satisfied for the given TP transaction. Used by cross-module callers
     * (via {@link com.openfinova.banking.tan.api.TanService}) before proceeding with payment
     * processing that requires a verified TAN.
     *
     * @param transactionId TP transaction identifier
     * @return {@code true} when a {@link TanPendingAuthorization} exists in {@code SCA_VERIFIED} state
     */
    @Transactional(readOnly = true)
    public boolean isScaVerified(UUID transactionId) {
        return pendingAuthorizationRepository.findByTransactionId(transactionId)
                .map(p -> p.getStatus() == TanPendingAuthorizationStatus.SCA_VERIFIED).orElse(false);
    }

    /**
     * Returns an existing pending authorization or creates one from the TP transaction snapshot.
     * Only {@code INITIATED} transactions are eligible; a transaction may not be registered for
     * a different user than the one who first opened SCA.
     */
    private TanPendingAuthorization ensurePendingAuthorization(UUID userId, UUID txnId) {
        TanPendingAuthorization existing = pendingAuthorizationRepository.findByTransactionId(txnId).orElse(null);
        if (existing != null) {
            expireIfNeeded(existing);
            if (existing.getUserId().equals(userId)) {
                return existing;
            }
            throw new IllegalStateException("Transaction already registered for SCA");
        }

        TransactionResponse txn = transactionProcessingService.getTransactionById(txnId);
        if (txn.getStatus() != TransactionStatus.INITIATED) {
            throw new IllegalStateException("Transaction is not eligible for SCA");
        }

        String payeeIban = "UNKNOWN";
        String payeeName = "Unknown payee";
        if (txn.getDestinationAccountId() != null) {
            AccountPayeeInfo payee = customerAccountService.getAccountPayeeInfo(txn.getDestinationAccountId())
                    .orElse(null);
            if (payee != null) {
                payeeIban = payee.iban() != null ? payee.iban() : payeeIban;
                payeeName = payee.displayName() != null ? payee.displayName() : payeeName;
            }
        }

        Duration ttl = Duration.ofMinutes(properties.getAuthorization().getPendingTtlMinutes());
        TanPendingAuthorization pending = new TanPendingAuthorization();
        pending.setId(UUID.randomUUID());
        pending.setTransactionId(txnId);
        pending.setUserId(userId);
        pending.setAmount(txn.getAmount());
        pending.setCurrency(txn.getCurrency());
        pending.setPayeeIban(payeeIban);
        pending.setPayeeName(payeeName);
        pending.setDescription(txn.getDescription());
        pending.setStatus(TanPendingAuthorizationStatus.PENDING);
        pending.setExpiresAt(dateTimeService.instant().plus(ttl));
        return pendingAuthorizationRepository.save(pending);
    }

    private TanPendingAuthorization loadPendingForUser(UUID userId, UUID txnId) {
        return pendingAuthorizationRepository.findByTransactionIdAndUserId(txnId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pending TAN authorization not found"));
    }

    /** Lazily transitions {@code PENDING} rows past {@code expiresAt} to {@code EXPIRED}. */
    private void expireIfNeeded(TanPendingAuthorization pending) {
        if (pending.getStatus() == TanPendingAuthorizationStatus.PENDING
                && pending.getExpiresAt().isBefore(dateTimeService.instant())) {
            pending.setStatus(TanPendingAuthorizationStatus.EXPIRED);
            pendingAuthorizationRepository.save(pending);
        }
    }

    private PendingTransactionResponse toPendingResponse(TanPendingAuthorization pending) {
        return new PendingTransactionResponse(
                pending.getTransactionId(),
                pending.getAmount(),
                pending.getCurrency(),
                pending.getPayeeIban(),
                pending.getPayeeName(),
                pending.getDescription(),
                pending.getExpiresAt());
    }
}
