package com.openfinova.banking.tp.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.customer.account.api.CustomerAccountService;
import com.openfinova.banking.setup.api.DateTimeService;
import com.openfinova.banking.tp.api.dto.BalanceReservationRequest;
import com.openfinova.banking.tp.api.dto.ReservationModificationRequest;
import com.openfinova.banking.tp.api.entity.ReservationStatus;
import com.openfinova.banking.tp.api.entity.ReservationType;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.entity.BalanceReservation;
import com.openfinova.banking.tp.entity.Transaction;
import com.openfinova.banking.tp.repository.BalanceReservationRepository;

/**
 * Implementation of BalanceReservationService with enhanced reservation management functionality.
 * Provides comprehensive reservation operations including modification, extension, and batch processing.
 */
@Service
@Transactional
public class BalanceReservationService {

    private static final Logger logger = LoggerFactory.getLogger(BalanceReservationService.class);

    private final BalanceReservationRepository reservationRepository;
    private final DateTimeService dateTimeService;
    private final CustomerAccountService customerAccountService;

    public BalanceReservationService(BalanceReservationRepository reservationRepository,
            DateTimeService dateTimeService, CustomerAccountService customerAccountService) {
        this.reservationRepository = reservationRepository;
        this.dateTimeService = dateTimeService;
        this.customerAccountService = customerAccountService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UUID reserveBalanceForTransaction(Transaction transaction, UUID accountId, BigDecimal amount,
            ReservationType type) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null for transaction-integrated reservations");
        }

        // Idempotency: same transactionId must not create duplicate reservations (e.g. client retry)
        Optional<BalanceReservation> existing = reservationRepository.findByTransaction_Id(transaction.getId());
        if (existing.isPresent()) {
            logger.debug("Reservation already exists for transaction {}, returning existing id", transaction.getId());
            return existing.get().getId();
        }

        // Atomic: lock account row then check balance (prevents dual-authorization race)
        if (!customerAccountService.hasSufficientBalanceUnderLock(accountId, amount)) {
            throw new IllegalArgumentException(
                    "Insufficient balance in account " + accountId + " for amount " + amount);
        }

        logger.debug(
                "Creating transaction-integrated balance reservation for transaction {} on account {} with amount {} and type {}",
                transaction.getId(),
                accountId,
                amount,
                type);

        String currency = transaction.getRequest().getCurrency();
        String reservationKey = transaction.getIdempotencyKey();
        String reservationReference = transaction.getIdempotencyKey();
        LocalDateTime expiresAt = calculateExpirationTime(transaction);

        BalanceReservation reservation = new BalanceReservation();
        reservation.setTransaction(transaction);
        reservation.setAccountId(accountId);
        reservation.setReservedAmount(amount);
        reservation.setOriginalAmount(amount);
        reservation.setCurrency(currency);
        reservation.setReservationType(type);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setExpiresAt(expiresAt);
        reservation.setReservationKey(reservationKey);
        reservation.setReservationReference(reservationReference);

        if (transaction.getRequest().getMetadata() != null) {
            reservation.setReservationMetadata(transaction.getRequest().getMetadata());
        }

        BalanceReservation saved = reservationRepository.save(reservation);
        logger.info(
                "Created transaction-integrated balance reservation {} for transaction {} on account {}",
                saved.getId(),
                transaction.getId(),
                accountId);

        return saved.getId();
    }

    /**
     * Releases a reservation (marks RELEASED). Idempotent: if already RELEASED or EXPIRED, no-op
     * so concurrent scheduler jobs (failTimedOutTransactions vs releaseExpiredReservations) do not
     * conflict when touching the same row.
     */
    public void releaseReservation(UUID reservationId) {
        logger.debug("Releasing reservation {}", reservationId);

        BalanceReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (!reservation.getStatus().isHoldingFunds()) {
            logger.debug("Reservation {} already in terminal state {}", reservationId, reservation.getStatus());
            return;
        }

        reservation.release("Manual release");
        reservationRepository.save(reservation);
        logger.info("Released reservation {}", reservationId);
    }

    public void confirmReservation(UUID reservationId) {
        logger.debug("Confirming reservation {}", reservationId);

        BalanceReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        reservation.convertToPosting();
        reservationRepository.save(reservation);

        logger.info("Confirmed reservation {}", reservationId);
    }

    public UUID modifyReservation(UUID reservationId, BigDecimal newAmount, String reason) {
        logger.debug("Modifying reservation {} to new amount {} with reason: {}", reservationId, newAmount, reason);

        BalanceReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (!reservation.getStatus().isHoldingFunds()) {
            throw new IllegalArgumentException("Cannot modify reservation that is not active");
        }

        if (reservation.hasExpired()) {
            throw new IllegalArgumentException("Cannot modify expired reservation");
        }

        // Validate new amount is positive
        if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("New amount must be greater than zero");
        }

        // Validate balance if increasing the reservation amount
        BigDecimal currentAmount = reservation.getReservedAmount();
        if (newAmount.compareTo(currentAmount) > 0) {
            BigDecimal additionalAmount = newAmount.subtract(currentAmount);

            if (!customerAccountService.hasSufficientBalance(reservation.getAccountId(), additionalAmount)) {
                throw new IllegalArgumentException(
                        String.format("Insufficient balance to increase reservation by %s", additionalAmount));
            }
        }

        reservation.setReservedAmount(newAmount);
        reservation.setReleaseReason(reason);

        BalanceReservation saved = reservationRepository.save(reservation);
        logger.info("Modified reservation {} to amount {}", reservationId, newAmount);

        return saved.getId();
    }

    public UUID extendReservation(UUID reservationId, LocalDateTime newExpirationTime, String reason) {
        logger.debug(
                "Extending reservation {} to new expiration {} with reason: {}",
                reservationId,
                newExpirationTime,
                reason);

        BalanceReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (!reservation.getStatus().isHoldingFunds()) {
            throw new IllegalArgumentException("Cannot extend reservation that is not active");
        }

        // Validate new expiration time is in the future
        if (newExpirationTime.isBefore(dateTimeService.now())) {
            throw new IllegalArgumentException("New expiration time must be in the future");
        }

        reservation.setExpiresAt(newExpirationTime);
        reservation.setReleaseReason(reason);

        BalanceReservation saved = reservationRepository.save(reservation);
        logger.info("Extended reservation {} to expiration {}", reservationId, newExpirationTime);

        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<BalanceReservation> getActiveReservations(UUID accountId) {
        logger.debug("Retrieving active reservations for account {}", accountId);

        List<BalanceReservation> reservations = reservationRepository.findActiveReservationsByAccountId(accountId);

        logger.debug("Found {} active reservations for account {}", reservations.size(), accountId);
        return reservations;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalReservedAmount(UUID accountId) {
        logger.debug("Calculating total reserved amount for account {}", accountId);

        BigDecimal totalReserved = reservationRepository.getTotalReservedAmount(accountId, dateTimeService.now());

        logger.debug("Total reserved amount for account {}: {}", accountId, totalReserved);
        return totalReserved != null ? totalReserved : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BalanceReservation getReservationDetails(UUID reservationId) {
        logger.debug("Retrieving reservation details for {}", reservationId);

        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
    }

    @Transactional(readOnly = true)
    public boolean isReservationExpired(UUID reservationId) {
        logger.debug("Checking if reservation {} is expired", reservationId);

        BalanceReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        boolean expired = reservation.hasExpired();
        logger.debug("Reservation {} expired status: {}", reservationId, expired);

        return expired;
    }

    public void expireReservations(UUID accountId) {
        logger.debug("Expiring reservations for account {}", accountId);

        List<BalanceReservation> expiredReservations = reservationRepository
                .findExpiredReservations(accountId, dateTimeService.now());

        int expiredCount = 0;
        for (BalanceReservation reservation : expiredReservations) {
            reservation.markExpired();
            reservationRepository.save(reservation);
            expiredCount++;
        }

        logger.info("Expired {} reservations for account {}", expiredCount, accountId);
    }

    /**
     * Marks all expired reservations (ACTIVE with expiresAt &lt;= now) as EXPIRED across all accounts.
     * Frees locked funds. Called by TPOperationsScheduler. Does not fail the related transaction;
     * use failTimedOutTransactions for that.
     */
    public int releaseAllExpiredReservations() {
        LocalDateTime now = dateTimeService.now();
        List<BalanceReservation> expired = reservationRepository.findAllExpiredReservations(now);
        for (BalanceReservation reservation : expired) {
            reservation.markExpired();
            reservationRepository.save(reservation);
        }
        if (!expired.isEmpty()) {
            logger.info("Released {} expired reservation(s) as of {}", expired.size(), now);
        }
        return expired.size();
    }

    public List<UUID> reserveBalanceBatch(List<BalanceReservationRequest> requests) {
        logger.debug("Processing batch reservation request with {} items", requests.size());

        List<UUID> reservationIds = new ArrayList<>();

        for (BalanceReservationRequest request : requests) {
            try {
                // Create reservation from request
                BalanceReservation reservation = createReservationFromRequest(request);
                BalanceReservation saved = reservationRepository.save(reservation);
                reservationIds.add(saved.getId());

                logger.debug("Created batch reservation {} for account {}", saved.getId(), request.getAccountId());
            } catch (Exception e) {
                logger.error(
                        "Failed to create reservation for account {} in batch: {}",
                        request.getAccountId(),
                        e.getMessage());
                throw new RuntimeException("Batch reservation failed for account: " + request.getAccountId(), e);
            }
        }

        logger.info("Successfully created {} reservations in batch", reservationIds.size());
        return reservationIds;
    }

    public void releaseReservationBatch(List<UUID> reservationIds) {
        logger.debug("Processing batch release request for {} reservations", reservationIds.size());

        List<BalanceReservation> reservations = reservationRepository.findByIdIn(reservationIds);

        int releasedCount = 0;
        for (BalanceReservation reservation : reservations) {
            if (reservation.getStatus().isHoldingFunds()) {
                reservation.release("Batch release");
                reservationRepository.save(reservation);
                releasedCount++;
            }
        }

        logger.info("Released {} reservations in batch", releasedCount);
    }

    public void confirmReservationBatch(List<UUID> reservationIds) {
        logger.debug("Processing batch confirmation request for {} reservations", reservationIds.size());

        List<BalanceReservation> reservations = reservationRepository.findByIdIn(reservationIds);

        int confirmedCount = 0;
        for (BalanceReservation reservation : reservations) {
            if (reservation.getStatus().isHoldingFunds()) {
                reservation.convertToPosting();
                reservationRepository.save(reservation);
                confirmedCount++;
            }
        }

        logger.info("Confirmed {} reservations in batch", confirmedCount);
    }

    public List<UUID> modifyReservationBatch(List<ReservationModificationRequest> modifications) {
        logger.debug("Processing batch modification request for {} reservations", modifications.size());

        List<UUID> modifiedIds = new ArrayList<>();

        for (ReservationModificationRequest modification : modifications) {
            try {
                UUID modifiedId = modifyReservation(
                        modification.getReservationId(),
                        modification.getNewAmount(),
                        modification.getReason());

                // Handle expiration time modification if provided
                if (modification.getNewExpirationTime() != null) {
                    extendReservation(modifiedId, modification.getNewExpirationTime(), modification.getReason());
                }

                modifiedIds.add(modifiedId);

                logger.debug("Modified reservation {} in batch", modification.getReservationId());
            } catch (Exception e) {
                logger.error(
                        "Failed to modify reservation {} in batch: {}",
                        modification.getReservationId(),
                        e.getMessage());
                throw new RuntimeException(
                        "Batch modification failed for reservation: " + modification.getReservationId(),
                        e);
            }
        }

        logger.info("Successfully modified {} reservations in batch", modifiedIds.size());
        return modifiedIds;
    }

    // Helper methods

    private BalanceReservation createReservationFromRequest(BalanceReservationRequest request) {
        BalanceReservation reservation = new BalanceReservation();
        reservation.setAccountId(request.getAccountId());
        reservation.setReservedAmount(request.getAmount());
        reservation.setOriginalAmount(request.getAmount());
        reservation.setCurrency(request.getCurrency());
        reservation.setReservationType(request.getType());
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setReservationKey(request.getReservationKey());
        reservation.setReservationReference(request.getReservationReference());
        reservation.setReservationMetadata(request.getMetadata());

        // Set expiration time - use provided or default to 24 hours
        LocalDateTime expiresAt = request.getExpiresAt() != null ? request.getExpiresAt()
                : dateTimeService.now().plusHours(24);
        reservation.setExpiresAt(expiresAt);

        return reservation;
    }

    /**
     * Calculates the expiration time for a reservation based on transaction context.
     * Uses transaction-specific timeout if provided, otherwise defaults based on transaction type.
     *
     * @param transaction The transaction for which to calculate expiration time
     * @return The calculated expiration time
     */
    private LocalDateTime calculateExpirationTime(Transaction transaction) {
        // Use transaction-specific timeout if provided
        if (transaction.getRequest().getRequestedReservationTimeout() != null) {
            return dateTimeService.now().plusMinutes(transaction.getRequest().getRequestedReservationTimeout());
        }

        // Default based on transaction type
        TransactionType type = transaction.getRequest().getTransactionType();
        return switch (type) {
            case P2P -> dateTimeService.now().plusHours(1); // 1 hour for P2P transfers
            case CASH_OUT -> dateTimeService.now().plusMinutes(30); // 30 minutes for cash operations
            case DEPOSIT, CASH_IN -> dateTimeService.now().plusHours(2); // 2 hours for deposits
            case TRANSFER -> dateTimeService.now().plusHours(4); // 4 hours for transfers
            default -> dateTimeService.now().plusHours(24); // 24 hours default
        };
    }
}