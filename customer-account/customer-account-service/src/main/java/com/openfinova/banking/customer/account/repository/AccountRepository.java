package com.openfinova.banking.customer.account.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.customer.account.api.entity.AccountProductType;
import com.openfinova.banking.customer.account.api.entity.AccountStatus;
import com.openfinova.banking.customer.account.entity.Account;
import com.openfinova.banking.customer.account.entity.AccountSearchCriteria;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Find account by ID with pessimistic write lock (SELECT FOR UPDATE).
     * Use when checking balance and creating a reservation in the same transaction to avoid dual-authorization race.
     * Lock timeout 3000 ms to avoid indefinite wait and thread pool exhaustion.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") UUID id);

    // Essential finder methods

    /**
     * Find account by account number.
     *
     * @param accountNumber the unique account number
     * @return optional containing the account if found
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Find account by IBAN.
     *
     * @param iban the International Bank Account Number
     * @return optional containing the account if found
     */
    Optional<Account> findByIban(String iban);

    /**
     * Find account by account number with all relationships eagerly loaded.
     *
     * @param accountNumber the unique account number
     * @return optional containing the account with relationships if found
     */
    @Query("""
            SELECT a FROM Account a
            LEFT JOIN FETCH a.glAccountMappings
            LEFT JOIN FETCH a.relationships
            LEFT JOIN FETCH a.limits
            LEFT JOIN FETCH a.holds
            LEFT JOIN FETCH a.interestRates
            WHERE a.accountNumber = :accountNumber
            """)
    Optional<Account> findByAccountNumberWithRelationships(@Param("accountNumber") String accountNumber);

    /**
     * Find account by IBAN with all relationships eagerly loaded.
     *
     * @param iban the International Bank Account Number
     * @return optional containing the account with relationships if found
     */
    @Query("""
            SELECT a FROM Account a
            LEFT JOIN FETCH a.glAccountMappings
            LEFT JOIN FETCH a.relationships
            LEFT JOIN FETCH a.limits
            LEFT JOIN FETCH a.holds
            LEFT JOIN FETCH a.interestRates
            WHERE a.iban = :iban
            """)
    Optional<Account> findByIbanWithRelationships(@Param("iban") String iban);

    /**
     * Find all accounts by primary user profile ID with relationships.
     *
     * @param primaryUserProfileId the user profile ID
     * @return list of accounts for the user with relationships loaded
     */
    @Query("""
            SELECT DISTINCT a FROM Account a
            LEFT JOIN FETCH a.glAccountMappings
            LEFT JOIN FETCH a.relationships
            LEFT JOIN FETCH a.limits
            LEFT JOIN FETCH a.holds
            WHERE a.primaryUserProfileId = :primaryUserProfileId
            """)
    List<Account> findAllByPrimaryUserProfileIdWithRelationships(
            @Param("primaryUserProfileId") UUID primaryUserProfileId);

    /**
     * Find accounts by primary user profile ID.
     *
     * @param primaryUserProfileId the user profile ID
     * @return list of accounts for the primary user
     */
    List<Account> findByPrimaryUserProfileId(UUID primaryUserProfileId);

    /**
     * Find all accounts by primary user profile ID.
     *
     * @param primaryUserProfileId the user profile ID
     * @return list of accounts for the user
     */
    List<Account> findAllByPrimaryUserProfileId(UUID primaryUserProfileId);

    // Status and product type filtering methods with pagination

    /**
     * Find accounts by status with pagination.
     *
     * @param status the account status
     * @param pageable pagination information
     * @return page of accounts with the specified status
     */
    Page<Account> findByStatus(AccountStatus status, Pageable pageable);

    /**
     * Find accounts by product type with pagination.
     *
     * @param productType the account product type
     * @param pageable pagination information
     * @return page of accounts with the specified product type
     */
    Page<Account> findByProductType(AccountProductType productType, Pageable pageable);

    /**
     * Find accounts by status and product type with pagination.
     *
     * @param status the account status
     * @param productType the account product type
     * @param pageable pagination information
     * @return page of accounts matching both criteria
     */
    Page<Account> findByStatusAndProductType(AccountStatus status, AccountProductType productType, Pageable pageable);

    /**
     * Find accounts by primary user profile ID and status.
     *
     * @param primaryUserProfileId the user profile ID
     * @param status the account status
     * @return list of accounts matching both criteria
     */
    List<Account> findByPrimaryUserProfileIdAndStatus(UUID primaryUserProfileId, AccountStatus status);

    /**
     * Find accounts by primary user profile ID and product type.
     *
     * @param primaryUserProfileId the user profile ID
     * @param productType the account product type
     * @return list of accounts matching both criteria
     */
    List<Account> findByPrimaryUserProfileIdAndProductType(UUID primaryUserProfileId, AccountProductType productType);

    // Batch operations for account status updates

    /**
     * Update account status for multiple accounts by IDs.
     *
     * @param accountIds list of account IDs to update
     * @param newStatus the new status to set
     * @param closureReason reason for closure (if applicable)
     * @return number of accounts updated
     */
    @Modifying
    @Query("""
            UPDATE Account a
            SET a.status = :newStatus, a.closureReason = :closureReason, a.closedAt = CURRENT_TIMESTAMP
            WHERE a.id IN :accountIds AND a.status != :newStatus
            """)
    int updateAccountStatusBatch(@Param("accountIds") List<UUID> accountIds,
            @Param("newStatus") AccountStatus newStatus, @Param("closureReason") String closureReason);

    /**
     * Update account status for accounts by primary user profile ID.
     *
     * @param primaryUserProfileId the user profile ID
     * @param newStatus the new status to set
     * @param closureReason reason for closure (if applicable)
     * @return number of accounts updated
     */
    @Modifying
    @Query("""
            UPDATE Account a
            SET a.status = :newStatus, a.closureReason = :closureReason, a.closedAt = CURRENT_TIMESTAMP
            WHERE a.primaryUserProfileId = :primaryUserProfileId AND a.status != :newStatus
            """)
    int updateAccountStatusByUser(@Param("primaryUserProfileId") UUID primaryUserProfileId,
            @Param("newStatus") AccountStatus newStatus, @Param("closureReason") String closureReason);

    // Complex business queries with @Query annotations

    /**
     * Find active accounts with available balance greater than specified amount.
     *
     * @param minBalance minimum available balance
     * @param pageable pagination information
     * @return page of accounts with sufficient balance
     */
    @Query("""
            SELECT a FROM Account a
            WHERE a.status = 'ACTIVE'
            AND a.availableBalance >= :minBalance
            """)
    Page<Account> findActiveAccountsWithMinBalance(@Param("minBalance") java.math.BigDecimal minBalance,
            Pageable pageable);

    /**
     * Find accounts by currency with pagination.
     *
     * @param currency the currency code
     * @param pageable pagination information
     * @return page of accounts in the specified currency
     */
    Page<Account> findByCurrency(String currency, Pageable pageable);

    /**
     * Find dormant accounts (no transactions in specified days).
     *
     * @param cutoffDate
     * @param pageable pagination information
     * @return page of potentially dormant accounts
     */
    @Query("""
            SELECT a FROM Account a
            WHERE a.status = 'ACTIVE'
            AND a.updatedAt < :cutoffDate
            """)
    Page<Account> findDormantAccounts(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

    /**
     * Find accounts created within a date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of accounts created in the date range
     */
    @Query("""
            SELECT a FROM Account a
            WHERE a.createdAt BETWEEN :startDate AND :endDate
            """)
    Page<Account> findAccountsCreatedBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, Pageable pageable);

    /**
     * Count accounts by status.
     *
     * @param status the account status
     * @return count of accounts with the specified status
     */
    long countByStatus(AccountStatus status);

    /**
     * Count accounts by product type.
     *
     * @param productType the account product type
     * @return count of accounts with the specified product type
     */
    long countByProductType(AccountProductType productType);

    /**
     * Find accounts with zero balance.
     *
     * @param pageable pagination information
     * @return page of accounts with zero ledger balance
     */
    @Query("""
            SELECT a FROM Account a
            WHERE a.ledgerBalance = 0
            """)
    Page<Account> findAccountsWithZeroBalance(Pageable pageable);

    /**
     * Find accounts with negative available balance (overdrafts).
     *
     * @param pageable pagination information
     * @return page of accounts with negative available balance
     */
    @Query("""
            SELECT a FROM Account a
            WHERE a.availableBalance < 0
            """)
    Page<Account> findOverdraftAccounts(Pageable pageable);

    // Additional methods for enhanced AccountService implementation

    /**
     * Find accounts for dormancy check based on cutoff date.
     *
     * @param cutoffDate the cutoff date for inactivity
     * @return list of accounts that may be dormant
     */
    @Query("""
            SELECT a FROM Account a
            WHERE a.status = 'ACTIVE'
            AND a.updatedAt < :cutoffDate
            """)
    List<Account> findAccountsForDormancyCheck(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find accounts by status (non-paginated).
     *
     * @param status the account status
     * @return list of accounts with the specified status
     */
    List<Account> findByStatus(AccountStatus status);

    /**
     * Find accounts by product type (non-paginated).
     *
     * @param productType the account product type
     * @return list of accounts with the specified product type
     */
    List<Account> findByProductType(AccountProductType productType);

    /**
     * Find accounts for a user through relationships.
     *
     * @param userProfileId the user profile ID
     * @return list of accounts where the user has any relationship
     */
    @Query("""
            SELECT DISTINCT a FROM Account a
            JOIN a.relationships r
            WHERE r.userProfileId = :userProfileId
            AND r.status = 'ACTIVE'
            """)
    List<Account> findAccountsForUser(@Param("userProfileId") UUID userProfileId);

    /**
     * Find accounts eligible for interest calculation.
     *
     * @return list of accounts eligible for interest
     */
    @Query("""
            SELECT a FROM Account a
            WHERE a.status = 'ACTIVE'
            AND a.ledgerBalance > 0
            """)
    List<Account> findAccountsEligibleForInterest();

    /**
     * Find accounts with complex filtering criteria.
     *
     * @param criteria the search criteria
     * @param pageable pagination information
     * @return page of accounts matching the criteria
     */
    @Query("""
            SELECT a FROM Account a
            WHERE (:#{#criteria.status} IS NULL OR a.status = :#{#criteria.status})
            AND (:#{#criteria.productType} IS NULL OR a.productType = :#{#criteria.productType})
            AND (:#{#criteria.currency} IS NULL OR a.currency = :#{#criteria.currency})
            AND (:#{#criteria.primaryUserProfileId} IS NULL OR a.primaryUserProfileId = :#{#criteria.primaryUserProfileId})
            """)
    Page<Account> findAccountsWithCriteria(@Param("criteria") AccountSearchCriteria criteria, Pageable pageable);
}
