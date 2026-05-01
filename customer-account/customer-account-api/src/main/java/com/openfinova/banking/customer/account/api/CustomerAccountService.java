package com.openfinova.banking.customer.account.api;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.openfinova.banking.customer.account.api.entity.GLAccountMappingType;

/**
 * Facade interface for Customer Account operations.
 * This interface defines the contract for account operations used by other modules.
 * Implementation resides in customer-account-service module following the Module Contract Pattern.
 *
 * Note: Implementation types are kept internal to the customer-account module.
 * Callers should depend on this interface and let Spring inject the concrete implementation.
 *
 * Used by:
 * - TP module: Account validation, balance checks, GL account resolution
 * - GL module: Account existence checks
 */
public interface CustomerAccountService {

    /**
     * Resolves the primary user profile (owner) ID for an account.
     * Used by: TP module (tier resolution for fee/FX discount).
     *
     * @param accountId the customer account ID
     * @return the primary user profile ID if the account exists, empty otherwise
     */
    Optional<UUID> getPrimaryUserProfileIdForAccount(UUID accountId);

    /**
     * Checks whether a customer account exists.
     * Used by: TP module (transaction validation)
     *
     * @param accountId the customer account ID
     * @return true if the account exists
     */
    boolean accountExists(UUID accountId);

    /**
     * Checks whether a customer account is in ACTIVE status.
     * Used by: TP module (transaction validation)
     *
     * @param accountId the customer account ID
     * @return true if the account is active
     */
    boolean isAccountActive(UUID accountId);

    /**
     * Checks whether a customer account is eligible for the given transaction type.
     * An account is eligible if it exists, is active, and its product type supports
     * the transaction type.
     * Used by: TP module (transaction validation)
     *
     * @param accountId the customer account ID
     * @param transactionType the transaction type (enum name as string)
     * @return true if the account is eligible for the transaction type
     */
    boolean isAccountEligibleForTransaction(UUID accountId, Object transactionType);

    /**
     * Checks whether a customer account has sufficient funds for a debit of the given amount.
     * Uses available balance (ledger minus holds/reservations) plus the account's effective
     * overdraft limit. True when (availableBalance + overdraftLimit) >= amount.
     * Used by: TP module (debit transaction validation)
     *
     * @param accountId the customer account ID
     * @param amount the debit amount to check
     * @return true if the account can cover the amount (within available + overdraft)
     */
    boolean hasSufficientBalance(UUID accountId, BigDecimal amount);

    /**
     * Same as {@link #hasSufficientBalance(UUID, BigDecimal)} but acquires a pessimistic lock on the account row.
     * Call from within the same transaction that will create the balance reservation so the lock is held until commit.
     * Uses available balance + overdraft limit for the check.
     * Used by: TP module (atomic reserve to prevent dual-authorization race).
     * Uses READ_COMMITTED + SELECT FOR UPDATE with lock timeout (e.g. 3s); do not use SERIALIZABLE.
     *
     * @param accountId the customer account ID
     * @param amount the debit amount to check
     * @return true if the account can cover the amount (within available + overdraft)
     */
    boolean hasSufficientBalanceUnderLock(UUID accountId, BigDecimal amount);

    /**
     * Resolves the GL account ID mapped to a customer account for a given mapping type.
     * Used by: TP module (GL posting, fee collection, interest posting)
     *
     * @param accountId the customer account ID
     * @param mappingType the GL account mapping type
     * @return the GL account UUID, or null if no mapping exists
     */
    UUID getGLAccountIdForType(UUID accountId, GLAccountMappingType mappingType);
}
