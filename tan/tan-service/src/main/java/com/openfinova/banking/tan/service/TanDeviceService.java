package com.openfinova.banking.tan.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.setup.api.DateTimeService;
import com.openfinova.banking.tan.config.TanProperties;
import com.openfinova.banking.tan.crypto.TanCodeService;
import com.openfinova.banking.tan.dto.AttestationNonceResponse;
import com.openfinova.banking.tan.dto.ConfirmDeviceRequest;
import com.openfinova.banking.tan.dto.EnrollDeviceRequest;
import com.openfinova.banking.tan.dto.EnrollDeviceResponse;
import com.openfinova.banking.tan.dto.EnrollmentQrResponse;
import com.openfinova.banking.tan.dto.TanDeviceResponse;
import com.openfinova.banking.tan.entity.TanDevice;
import com.openfinova.banking.tan.entity.TanDeviceStatus;
import com.openfinova.banking.tan.exception.TanDeviceLimitExceededException;
import com.openfinova.banking.tan.repository.TanDeviceRepository;
import com.openfinova.banking.tan.service.TanEnrollmentTokenService.EnrollmentTokenClaims;

/**
 * Manages TAN device enrollment, confirmation, listing, and revocation.
 *
 * Enrollment is a two-channel process: the mobile app registers a device-generated secret
 * after attestation, then the user confirms possession on a trusted banking channel before
 * the device becomes {@link TanDeviceStatus#ACTIVE} and can authorize payments.
 *
 * Enrollment flow
 *   User with Gold ACR requests an enrollment QR ({@link #createEnrollmentQr}).
 *   Mobile app scans QR, obtains an attestation nonce ({@link #issueAttestationNonce}),
 *   completes platform attestation, and submits its secret ({@link #enrollDevice}).
 *   User confirms on the banking channel with a code derived from the secret
 *   ({@link #confirmDevice}).
 *
 * Per-user device count is capped by configuration; {@link TanDeviceStatus#REVOKED} devices
 * are retained for audit but excluded from listings and the active limit check.
 *
 * @see TanDevice
 * @see TanAttestationService
 * @see TanEnrollmentTokenService
 */
@Service
public class TanDeviceService {

    /** Validity window for the enrollment QR and signed enrollment token. */
    private static final Duration ENROLLMENT_QR_TTL = Duration.ofMinutes(15);

    private final TanDeviceRepository tanDeviceRepository;
    private final TanEnrollmentTokenService enrollmentTokenService;
    private final TanAttestationService attestationService;
    private final TanCodeService tanCodeService;
    private final TanNotificationHelper notificationHelper;
    private final TanProperties properties;
    private final DateTimeService dateTimeService;

    public TanDeviceService(TanDeviceRepository tanDeviceRepository, TanEnrollmentTokenService enrollmentTokenService,
            TanAttestationService attestationService, TanCodeService tanCodeService,
            TanNotificationHelper notificationHelper, TanProperties properties, DateTimeService dateTimeService) {
        this.tanDeviceRepository = tanDeviceRepository;
        this.enrollmentTokenService = enrollmentTokenService;
        this.attestationService = attestationService;
        this.tanCodeService = tanCodeService;
        this.notificationHelper = notificationHelper;
        this.properties = properties;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Issues a signed enrollment QR for the authenticated user. Requires Gold ACR (step-up MFA)
     * and checks the per-user device limit before creating a short-lived enrollment token.
     *
     * @param principal authenticated user enrolling a new TAN device
     * @return QR URL containing the enrollment token and its expiry
     */
    @Transactional
    @PreAuthorize("hasAuthority('mfa:manage:own')")
    public EnrollmentQrResponse createEnrollmentQr(BankingPrincipal principal) {
        requireGoldAcr(principal);
        enforceDeviceLimit(principal.userId());
        String nonce = UUID.randomUUID().toString();
        String token = enrollmentTokenService.createEnrollmentToken(principal.userId(), nonce);
        String baseUrl = properties.getBaseUrl().replaceAll("/$", "");
        String qrPayload = baseUrl + "/enroll?token=" + token;
        Instant expiresAt = dateTimeService.instant().plus(ENROLLMENT_QR_TTL);
        return new EnrollmentQrResponse(qrPayload, expiresAt);
    }

    /**
     * Issues a server attestation nonce for the enrollment token carried in the scanned QR.
     * Called by the mobile app before platform attestation and {@link #enrollDevice}.
     *
     * @param enrollmentToken signed token from the enrollment QR
     * @return nonce and expiry for Play Integrity / App Attest
     */
    public AttestationNonceResponse issueAttestationNonce(String enrollmentToken) {
        EnrollmentTokenClaims claims = enrollmentTokenService.parseEnrollmentToken(enrollmentToken);
        TanAttestationService.AttestationNonce nonce = attestationService.issueNonce(claims.jti());
        return new AttestationNonceResponse(nonce.nonce(), nonce.expiresAt());
    }

    /**
     * Registers a new device in {@link TanDeviceStatus#PENDING_ENROLLMENT} after attestation
     * and secret submission from the mobile app.
     *
     * The {@code tanSecret} must be a Base64-encoded 32-byte key generated on the device.
     * Consumes the enrollment token so it cannot be reused. Returns a confirmation challenge
     * the user must satisfy via {@link #confirmDevice} on a trusted channel.
     *
     * @param request enrollment token, attestation verdict, device metadata, and encoded secret
     * @return new device id and confirmation input for the second-factor step
     * @throws TanDeviceLimitExceededException if the user already has the maximum allowed devices
     */
    @Transactional
    public EnrollDeviceResponse enrollDevice(EnrollDeviceRequest request) {
        EnrollmentTokenClaims claims = enrollmentTokenService.parseEnrollmentToken(request.enrollmentToken());
        enforceDeviceLimit(claims.userId());
        attestationService.verifyAttestation(claims.jti(), request.attestationToken());

        byte[] secretBytes = Base64.getDecoder().decode(request.tanSecret());
        if (secretBytes.length != 32) {
            throw new IllegalArgumentException("tanSecret must be 32 bytes");
        }

        String confirmationInput = UUID.randomUUID().toString();
        TanDevice device = new TanDevice();
        device.setId(UUID.randomUUID());
        device.setUserId(claims.userId());
        device.setDeviceName(request.deviceName());
        device.setStatus(TanDeviceStatus.PENDING_ENROLLMENT);
        device.setTanSecret(Base64.getEncoder().encodeToString(secretBytes));
        device.setEnrollmentNonce(confirmationInput);
        device.setPlatformDeviceId(request.platformDeviceId());
        tanDeviceRepository.save(device);

        enrollmentTokenService.consumeEnrollmentToken(claims.jti());
        return new EnrollDeviceResponse(device.getId(), confirmationInput);
    }

    /**
     * Completes enrollment by verifying a confirmation code derived from the device secret and
     * enrollment nonce. Transitions the device to {@link TanDeviceStatus#ACTIVE}.
     *
     * @param principal authenticated owner of the pending device
     * @param deviceId device returned from {@link #enrollDevice}
     * @param request confirmation code entered by the user on the banking channel
     * @throws IllegalArgumentException if the confirmation code does not match
     * @throws IllegalStateException if the device is not {@code PENDING_ENROLLMENT}
     */
    @Transactional
    @PreAuthorize("hasAuthority('mfa:manage:own')")
    public void confirmDevice(BankingPrincipal principal, UUID deviceId, ConfirmDeviceRequest request) {
        TanDevice device = tanDeviceRepository.findByIdAndUserId(deviceId, principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("TAN device not found"));
        if (device.getStatus() != TanDeviceStatus.PENDING_ENROLLMENT) {
            throw new IllegalStateException("Device is not pending enrollment");
        }
        byte[] secretBytes = Base64.getDecoder().decode(device.getTanSecret());
        if (!tanCodeService
                .verifyConfirmationCode(secretBytes, request.confirmationCode(), device.getEnrollmentNonce())) {
            throw new IllegalArgumentException("Invalid confirmation code");
        }
        device.setStatus(TanDeviceStatus.ACTIVE);
        device.setEnrolledAt(dateTimeService.instant());
        tanDeviceRepository.save(device);
        notificationHelper.notifyDeviceEnrolled(principal.userId(), device.getDeviceName());
    }

    /**
     * Lists all non-revoked devices for the authenticated user, including those still pending
     * enrollment confirmation.
     *
     * @param principal device owner or TAN app caller with {@code tan:generate}
     * @return device metadata safe for API exposure (no secrets)
     */
    @PreAuthorize("hasAuthority('mfa:manage:own') or hasAuthority('tan:generate')")
    @Transactional(readOnly = true)
    public List<TanDeviceResponse> listDevices(BankingPrincipal principal) {
        return tanDeviceRepository.findByUserIdAndStatusNot(principal.userId(), TanDeviceStatus.REVOKED).stream()
                .map(this::toResponse).toList();
    }

    /**
     * Permanently deregisters a device. Requires Gold ACR. Idempotent when already
     * {@link TanDeviceStatus#REVOKED}; revoked devices cannot authorize payments or be reactivated.
     *
     * @param principal authenticated device owner
     * @param deviceId device to revoke
     */
    @Transactional
    @PreAuthorize("hasAuthority('mfa:manage:own')")
    public void revokeDevice(BankingPrincipal principal, UUID deviceId) {
        requireGoldAcr(principal);
        TanDevice device = tanDeviceRepository.findByIdAndUserId(deviceId, principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("TAN device not found"));
        if (device.getStatus() == TanDeviceStatus.REVOKED) {
            return;
        }
        device.setStatus(TanDeviceStatus.REVOKED);
        tanDeviceRepository.save(device);
        notificationHelper.notifyDeviceRevoked(principal.userId(), device.getDeviceName());
    }

    /** Rejects enrollment when non-revoked devices for the user reach {@code tan.device.max-per-user}. */
    private void enforceDeviceLimit(UUID userId) {
        int max = properties.getDevice().getMaxPerUser();
        long activeCount = tanDeviceRepository.countByUserIdAndStatusNot(userId, TanDeviceStatus.REVOKED);
        if (activeCount >= max) {
            throw new TanDeviceLimitExceededException(
                    "Maximum of %d TAN devices allowed. Revoke an existing device before enrolling a new one."
                            .formatted(max));
        }
    }

    /** Ensures the caller completed step-up MFA (Gold ACR) for sensitive device operations. */
    private static void requireGoldAcr(BankingPrincipal principal) {
        if (!principal.hasGoldAcr()) {
            throw new IllegalStateException("Gold ACR (MFA) required for this operation");
        }
    }

    private TanDeviceResponse toResponse(TanDevice device) {
        return new TanDeviceResponse(
                device.getId(),
                device.getDeviceName(),
                device.getStatus(),
                device.getEnrolledAt(),
                device.getLastUsedAt());
    }
}
