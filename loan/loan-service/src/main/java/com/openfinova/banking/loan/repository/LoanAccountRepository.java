package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.DelinquencyBucket;
import com.openfinova.banking.loan.api.entity.LoanStatus;
import com.openfinova.banking.loan.entity.LoanAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LoanAccount entities.
 */
public interface LoanAccountRepository extends JpaRepository<LoanAccount, UUID> {

    /**
     * Find loan account by loan account number.
     *
     * @param loanAccountNumber the unique loan account number
     * @return optional containing the loan account if found
     */
    Optional<LoanAccount> findByLoanAccountNumber(String loanAccountNumber);

    /**
     * Loads a loan account by number. Collections use {@link org.hibernate.annotations.BatchSize}
     * on {@link com.openfinova.banking.loan.entity.LoanAccount} when accessed (avoids multi-bag fetch).
     */
    default Optional<LoanAccount> findByLoanAccountNumberWithRelationships(String loanAccountNumber) {
        return findByLoanAccountNumber(loanAccountNumber);
    }

    /**
     * Find all loan accounts by customer ID.
     *
     * @param customerId the customer ID
     * @return list of loan accounts for the customer
     */
    List<LoanAccount> findByCustomerId(UUID customerId);

    /**
     * Find loan accounts by customer ID with pagination.
     *
     * @param customerId the customer ID
     * @param pageable pagination information
     * @return page of loan accounts for the customer
     */
    Page<LoanAccount> findByCustomerId(UUID customerId, Pageable pageable);

    /**
     * Find loan accounts by application ID.
     *
     * @param applicationId the application ID
     * @return optional containing the loan account if found
     */
    Optional<LoanAccount> findByApplicationId(UUID applicationId);

    /**
     * Find loan accounts by product ID.
     *
     * @param productId the product ID
     * @param pageable pagination information
     * @return page of loan accounts for the product
     */
    Page<LoanAccount> findByProductId(UUID productId, Pageable pageable);

    /**
     * Find loan accounts by status.
     *
     * @param status the loan status
     * @param pageable pagination information
     * @return page of loan accounts with the specified status
     */
    Page<LoanAccount> findByStatus(LoanStatus status, Pageable pageable);

    /**
     * Find loan accounts by customer and status.
     *
     * @param customerId the customer ID
     * @param status the loan status
     * @return list of loan accounts matching both criteria
     */
    List<LoanAccount> findByCustomerIdAndStatus(UUID customerId, LoanStatus status);

    /**
     * Find active loan accounts by customer.
     *
     * @param customerId the customer ID
     * @return list of active loan accounts
     */
    @Query("""
            SELECT la FROM LoanAccount la
            WHERE la.customerId = :customerId
            AND la.status = 'ACTIVE'
            """)
    List<LoanAccount> findActiveAccountsByCustomer(@Param("customerId") UUID customerId);

    /**
     * Find delinquent loan accounts (days past due > 0).
     *
     * @param pageable pagination information
     * @return page of delinquent loan accounts
     */
    @Query("""
            SELECT la FROM LoanAccount la
            WHERE la.daysPastDue > 0
            AND la.status = 'ACTIVE'
            ORDER BY la.daysPastDue DESC
            """)
    Page<LoanAccount> findDelinquentAccounts(Pageable pageable);

    /**
     * Find loan accounts by delinquency bucket.
     *
     * @param delinquencyBucket the delinquency bucket (e.g., "0-30", "31-60")
     * @param pageable pagination information
     * @return page of loan accounts in the specified bucket
     */
    Page<LoanAccount> findByDelinquencyBucket(DelinquencyBucket delinquencyBucket, Pageable pageable);

    /**
     * Find loan accounts with days past due greater than specified threshold.
     *
     * @param daysPastDue minimum days past due
     * @param pageable pagination information
     * @return page of loan accounts exceeding the threshold
     */
    @Query("""
            SELECT la FROM LoanAccount la
            WHERE la.daysPastDue >= :daysPastDue
            AND la.status = 'ACTIVE'
            ORDER BY la.daysPastDue DESC
            """)
    Page<LoanAccount> findByDaysPastDueGreaterThanEqual(@Param("daysPastDue") Integer daysPastDue, Pageable pageable);

    /**
     * Find restructured loan accounts.
     *
     * @param pageable pagination information
     * @return page of restructured loan accounts
     */
    @Query("""
            SELECT la FROM LoanAccount la
            WHERE la.isRestructured = true
            """)
    Page<LoanAccount> findRestructuredAccounts(Pageable pageable);

    /**
     * Find loan accounts maturing within a date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of loan accounts maturing in the date range
     */
    @Query("""
            SELECT la FROM LoanAccount la
            WHERE la.maturityDate BETWEEN :startDate AND :endDate
            AND la.status = 'ACTIVE'
            ORDER BY la.maturityDate ASC
            """)
    Page<LoanAccount> findAccountsMaturingBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find loan accounts disbursed within a date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of loan accounts disbursed in the date range
     */
    @Query("""
            SELECT la FROM LoanAccount la
            WHERE la.disbursementDate BETWEEN :startDate AND :endDate
            ORDER BY la.disbursementDate DESC
            """)
    Page<LoanAccount> findAccountsDisbursedBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find loan accounts by currency.
     *
     * @param currency the currency code
     * @param pageable pagination information
     * @return page of loan accounts in the specified currency
     */
    Page<LoanAccount> findByCurrency(String currency, Pageable pageable);

    /**
     * Find top-up loan accounts.
     *
     * @param pageable pagination information
     * @return page of top-up loan accounts
     */
    @Query("""
            SELECT la FROM LoanAccount la
            WHERE la.isTopUp = true
            """)
    Page<LoanAccount> findTopUpAccounts(Pageable pageable);

    /**
     * Find loan accounts by original loan ID (for top-ups).
     *
     * @param originalLoanId the original loan ID
     * @return list of top-up loan accounts
     */
    List<LoanAccount> findByOriginalLoanId(UUID originalLoanId);

    /**
     * Count loan accounts by status.
     *
     * @param status the loan status
     * @return count of loan accounts with the specified status
     */
    long countByStatus(LoanStatus status);

    /**
     * Count loan accounts by customer.
     *
     * @param customerId the customer ID
     * @return count of loan accounts for the customer
     */
    long countByCustomerId(UUID customerId);

    /**
     * Count active loan accounts by customer.
     *
     * @param customerId the customer ID
     * @return count of active loan accounts
     */
    @Query("""
            SELECT COUNT(la) FROM LoanAccount la
            WHERE la.customerId = :customerId
            AND la.status = 'ACTIVE'
            """)
    long countActiveAccountsByCustomer(@Param("customerId") UUID customerId);

    /**
     * Sum outstanding principal for active loans by customer.
     *
     * @param customerId the customer ID
     * @return total outstanding principal
     */
    @Query("""
            SELECT COALESCE(SUM(la.outstandingPrincipal), 0)
            FROM LoanAccount la
            WHERE la.customerId = :customerId
            AND la.status = 'ACTIVE'
            """)
    BigDecimal sumOutstandingPrincipalByCustomer(@Param("customerId") UUID customerId);

    /**
     * Sum total outstanding (principal + interest + fees + penalties) by customer.
     *
     * @param customerId the customer ID
     * @return total outstanding amount
     */
    @Query("""
            SELECT COALESCE(SUM(la.outstandingPrincipal + la.outstandingInterest +
                               la.outstandingFees + la.outstandingPenalties), 0)
            FROM LoanAccount la
            WHERE la.customerId = :customerId
            AND la.status = 'ACTIVE'
            """)
    BigDecimal sumTotalOutstandingByCustomer(@Param("customerId") UUID customerId);

    /**
     * Update loan account status.
     *
     * @param loanAccountId the loan account ID
     * @param newStatus the new status
     * @return number of accounts updated
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE LoanAccount la
            SET la.status = :newStatus, la.updatedAt = :updatedAt
            WHERE la.id = :loanAccountId
            """)
    int updateLoanAccountStatus(@Param("loanAccountId") UUID loanAccountId, @Param("newStatus") LoanStatus newStatus,
            @Param("updatedAt") Instant updatedAt);

    /**
     * Update days past due and delinquency bucket.
     *
     * @param loanAccountId the loan account ID
     * @param daysPastDue days past due
     * @param delinquencyBucket delinquency bucket
     * @return number of accounts updated
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE LoanAccount la
            SET la.daysPastDue = :daysPastDue,
                la.delinquencyBucket = :delinquencyBucket,
                la.updatedAt = :updatedAt
            WHERE la.id = :loanAccountId
            """)
    int updateDelinquencyStatus(@Param("loanAccountId") UUID loanAccountId, @Param("daysPastDue") Integer daysPastDue,
            @Param("delinquencyBucket") DelinquencyBucket delinquencyBucket, @Param("updatedAt") Instant updatedAt);

    /**
     * Find loan accounts requiring interest accrual.
     *
     * @return list of active loan accounts
     */
    @Query("""
            SELECT la FROM LoanAccount la
            WHERE la.status = 'ACTIVE'
            AND la.outstandingPrincipal > 0
            """)
    List<LoanAccount> findAccountsForInterestAccrual();

    /**
     * Find loan accounts with outstanding balance greater than specified amount.
     *
     * @param minOutstanding minimum outstanding balance
     * @param pageable pagination information
     * @return page of loan accounts
     */
    @Query("""
            SELECT la FROM LoanAccount la
            WHERE (la.outstandingPrincipal + la.outstandingInterest +
                   la.outstandingFees + la.outstandingPenalties) >= :minOutstanding
            AND la.status = 'ACTIVE'
            ORDER BY (la.outstandingPrincipal + la.outstandingInterest +
                      la.outstandingFees + la.outstandingPenalties) DESC
            """)
    Page<LoanAccount> findAccountsWithMinOutstanding(@Param("minOutstanding") BigDecimal minOutstanding,
            Pageable pageable);

    /**
     * Find loan accounts by maturity date.
     *
     * @param maturityDate the maturity date
     * @return list of loan accounts maturing on the date
     */
    @Query("""
            SELECT la FROM LoanAccount la
            WHERE la.maturityDate = :maturityDate
            AND la.status = 'ACTIVE'
            """)
    List<LoanAccount> findByMaturityDate(@Param("maturityDate") LocalDate maturityDate);

    /**
     * Find closed loan accounts before a specific date.
     *
     * @param cutoffDate the cutoff date
     * @return list of closed loan accounts
     */
    @Query("""
            SELECT la FROM LoanAccount la
            WHERE la.status = 'CLOSED'
            AND la.closedDate < :cutoffDate
            """)
    List<LoanAccount> findClosedLoansBeforeDate(@Param("cutoffDate") LocalDate cutoffDate);
}
