package com.openfinova.banking.tp.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.openfinova.banking.tp.api.dto.BalanceReservationResponse;
import com.openfinova.banking.tp.api.dto.FeeCalculationResult;
import com.openfinova.banking.tp.api.dto.TransactionRequestDTO;
import com.openfinova.banking.tp.api.dto.TransactionResponse;
import com.openfinova.banking.tp.api.dto.VelocityLimitStatus;
import com.openfinova.banking.tp.api.entity.CustomerTier;

/**
 * Facade interface for Transaction Processing operations.
 * This interface defines the contract for TP operations used by other modules.
 * Implementation resides in transaction-processing-service module following the Module Contract Pattern.
 *
 * Note: Implementation types are kept internal to the TP module.
 * Callers should depend on this interface and let Spring inject the concrete implementation.
 *
 * Used by:
 * - Account module: Available balance calculation (reservations), fee inquiries
 * - GL module: Transaction status checks, transaction details for posting
 */
public interface TransactionProcessingService {

    // ── Transaction Lifecycle ──────────────────────────────────────────────

    /**
     * Initiates a new transaction with validation and idempotency checks.
     * Used by: Account module (transaction initiation)
     *
     * @param request the transaction request DTO
     * @return the initiated transaction response
     */
    TransactionResponse initiateTransaction(TransactionRequestDTO request);

    /**
     * Processes a transaction through the complete workflow.
     * Used by: Account module (transaction processing)
     *
     * @param transactionId the transaction ID
     * @return the processed transaction response
     */
    TransactionResponse processTransaction(UUID transactionId);

    /**
     * Completes a transaction after successful processing.
     * Used by: GL module (transaction finalization)
     *
     * @param transactionId the transaction ID
     * @return the completed transaction response
     */
    TransactionResponse completeTransaction(UUID transactionId);

    /**
     * Marks a transaction as failed with reason and error code.
     * Used by: Account module (error handling), GL module (posting failures)
     *
     * @param transactionId the transaction ID
     * @param reason the failure reason
     * @param errorCode the error code
     * @return the failed transaction response
     */
    TransactionResponse failTransaction(UUID transactionId, String reason, String errorCode);

    /**
     * Retrieves a transaction by its unique identifier.
     * Used by: Account module (transaction inquiry), GL module (posting lookup)
     *
     * @param id the transaction ID
     * @return the transaction response
     */
    TransactionResponse getTransactionById(UUID id);

    /**
     * Gets the current status of a transaction.
     * Used by: Account module (status checks), GL module (posting validation)
     *
     * @param transactionId the transaction ID
     * @return the transaction status (enum name as string)
     */
    String getTransactionStatus(UUID transactionId);

    /**
     * Finds an existing transaction by idempotency key.
     * Used by: Account module (duplicate detection)
     *
     * @param idempotencyKey the idempotency key
     * @return optional transaction response
     */
    Optional<TransactionResponse> findExistingTransaction(String idempotencyKey);

    // ── Balance Reservations ───────────────────────────────────────────────

    /**
     * Reserves balance for a transaction.
     * Used by: Account module (fund reservation)
     *
     * @param transactionId the transaction ID
     * @param accountId the account ID
     * @param amount the amount to reserve
     * @param reservationType the reservation type (enum name as string)
     * @return the reservation ID
     */
    UUID reserveBalanceForTransaction(UUID transactionId, UUID accountId, BigDecimal amount, String reservationType);

    /**
     * Releases a balance reservation.
     * Used by: Account module (reservation cancellation)
     *
     * @param reservationId the reservation ID
     */
    void releaseReservation(UUID reservationId);

    /**
     * Confirms a balance reservation, converting it to a posting.
     * Used by: GL module (transaction finalization)
     *
     * @param reservationId the reservation ID
     */
    void confirmReservation(UUID reservationId);

    /**
     * Gets the total reserved amount for an account.
     * Used by: Account module (available balance calculation)
     *
     * @param accountId the account ID
     * @return the total reserved amount
     */
    BigDecimal getTotalReservedAmount(UUID accountId);

    /**
     * Gets all active reservations for an account.
     * Used by: Account module (reservation inquiry)
     *
     * @param accountId the account ID
     * @return list of active reservation responses
     */
    List<BalanceReservationResponse> getActiveReservations(UUID accountId);

    // ── Velocity Limits ────────────────────────────────────────────────────

    /**
     * Checks if a transaction is within velocity limits.
     * Used by: Account module (transaction validation)
     *
     * @param accountId the account ID
     * @param transactionType the transaction type (enum name as string)
     * @param amount the amount
     * @param currency the currency code
     * @return true if within limits
     */
    boolean checkLimits(UUID accountId, String transactionType, BigDecimal amount, String currency);

    /**
     * Increments usage counters after a successful transaction.
     * Used by: Account module (limit tracking)
     *
     * @param accountId the account ID
     * @param transactionType the transaction type (enum name as string)
     * @param amount the amount
     * @param currency the currency code
     */
    void incrementUsage(UUID accountId, String transactionType, BigDecimal amount, String currency);

    /**
     * Gets the current velocity limit status for an account and transaction type.
     * Used by: Account module (limit inquiry)
     *
     * @param accountId the account ID
     * @param transactionType the transaction type (enum name as string)
     * @return the velocity limit status
     */
    VelocityLimitStatus getCurrentLimitStatus(UUID accountId, String transactionType);

    /**
     * Gets the remaining limit amount for a specific period.
     * Used by: Account module (available limit inquiry)
     *
     * @param accountId the account ID
     * @param transactionType the transaction type (enum name as string)
     * @param limitPeriod the limit period (enum name as string)
     * @return the remaining limit amount
     */
    BigDecimal getRemainingLimit(UUID accountId, String transactionType, String limitPeriod);

    // ── Fee Management ─────────────────────────────────────────────────────

    /**
     * Calculates the total fee amount for a transaction.
     * Used by: Account module (fee preview)
     *
     * @param transactionId the transaction ID to calculate fees for
     * @return the total calculated fee amount
     */
    BigDecimal calculateFees(UUID transactionId);

    /**
     * Calculates detailed fees with component breakdown for a transaction.
     * Used by: Account module (detailed fee inquiry)
     *
     * @param transactionId the transaction ID to calculate fees for
     * @return the detailed fee calculation result
     */
    FeeCalculationResult calculateDetailedFees(UUID transactionId);

    /**
     * Evaluates customer tier eligibility for fee discounts.
     * Used by: Account module (tier evaluation)
     *
     * @param customerId the customer ID
     * @return the resolved customer tier
     */
    CustomerTier evaluateTierEligibility(UUID customerId);
}
