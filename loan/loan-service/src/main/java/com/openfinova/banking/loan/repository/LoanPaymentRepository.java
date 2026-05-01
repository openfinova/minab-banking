package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.PaymentMethod;
import com.openfinova.banking.loan.api.entity.PaymentType;
import com.openfinova.banking.loan.entity.LoanPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LoanPayment entities.
 */
public interface LoanPaymentRepository extends JpaRepository<LoanPayment, UUID> {

    /**
     * Find loan payment by payment reference.
     *
     * @param paymentReference the unique payment reference
     * @return optional containing the payment if found
     */
    Optional<LoanPayment> findByPaymentReference(String paymentReference);

    /**
     * Find loan payment by transaction reference.
     *
     * @param transactionReference the external transaction reference
     * @return optional containing the payment if found
     */
    Optional<LoanPayment> findByTransactionReference(String transactionReference);

    /**
     * Find all payments for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @param pageable pagination information
     * @return page of payments for the loan account
     */
    @Query("""
            SELECT lp FROM LoanPayment lp
            WHERE lp.loanAccount.id = :loanAccountId
            ORDER BY lp.paymentDate DESC
            """)
    Page<LoanPayment> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId, Pageable pageable);

    /**
     * Find all payments for a loan account (non-paginated).
     *
     * @param loanAccountId the loan account ID
     * @return list of payments for the loan account
     */
    @Query("""
            SELECT lp FROM LoanPayment lp
            WHERE lp.loanAccount.id = :loanAccountId
            ORDER BY lp.paymentDate DESC
            """)
    List<LoanPayment> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find payments by loan account and date range.
     *
     * @param loanAccountId the loan account ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of payments in the date range
     */
    @Query("""
            SELECT lp FROM LoanPayment lp
            WHERE lp.loanAccount.id = :loanAccountId
            AND lp.paymentDate BETWEEN :startDate AND :endDate
            ORDER BY lp.paymentDate DESC
            """)
    Page<LoanPayment> findByLoanAccountIdAndPaymentDateBetween(@Param("loanAccountId") UUID loanAccountId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find payments by payment type.
     *
     * @param paymentType the payment type
     * @param pageable pagination information
     * @return page of payments of the specified type
     */
    @Query("""
            SELECT lp FROM LoanPayment lp
            WHERE lp.paymentType = :paymentType
            ORDER BY lp.paymentDate DESC
            """)
    Page<LoanPayment> findByPaymentType(@Param("paymentType") PaymentType paymentType, Pageable pageable);

    /**
     * Find payments by payment method.
     *
     * @param paymentMethod the payment method
     * @param pageable pagination information
     * @return page of payments using the specified method
     */
    @Query("""
            SELECT lp FROM LoanPayment lp
            WHERE lp.paymentMethod = :paymentMethod
            ORDER BY lp.paymentDate DESC
            """)
    Page<LoanPayment> findByPaymentMethod(@Param("paymentMethod") PaymentMethod paymentMethod, Pageable pageable);

    /**
     * Find payments by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of payments in the date range
     */
    @Query("""
            SELECT lp FROM LoanPayment lp
            WHERE lp.paymentDate BETWEEN :startDate AND :endDate
            ORDER BY lp.paymentDate DESC
            """)
    Page<LoanPayment> findByPaymentDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find reversed payments.
     *
     * @param pageable pagination information
     * @return page of reversed payments
     */
    @Query("""
            SELECT lp FROM LoanPayment lp
            WHERE lp.isReversed = true
            ORDER BY lp.reversedAt DESC
            """)
    Page<LoanPayment> findReversedPayments(Pageable pageable);

    /**
     * Find non-reversed payments for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of non-reversed payments
     */
    @Query("""
            SELECT lp FROM LoanPayment lp
            WHERE lp.loanAccount.id = :loanAccountId
            AND lp.isReversed = false
            ORDER BY lp.paymentDate DESC
            """)
    List<LoanPayment> findNonReversedPaymentsByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find payments by currency.
     *
     * @param currency the currency code
     * @param pageable pagination information
     * @return page of payments in the specified currency
     */
    Page<LoanPayment> findByCurrency(String currency, Pageable pageable);

    /**
     * Find payments with amount greater than specified threshold.
     *
     * @param minAmount minimum payment amount
     * @param pageable pagination information
     * @return page of high-value payments
     */
    @Query("""
            SELECT lp FROM LoanPayment lp
            WHERE lp.paymentAmount >= :minAmount
            ORDER BY lp.paymentAmount DESC
            """)
    Page<LoanPayment> findByPaymentAmountGreaterThanEqual(@Param("minAmount") BigDecimal minAmount, Pageable pageable);

    /**
     * Count payments for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of payments
     */
    @Query("""
            SELECT COUNT(lp) FROM LoanPayment lp
            WHERE lp.loanAccount.id = :loanAccountId
            """)
    long countByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Count non-reversed payments for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of non-reversed payments
     */
    @Query("""
            SELECT COUNT(lp) FROM LoanPayment lp
            WHERE lp.loanAccount.id = :loanAccountId
            AND lp.isReversed = false
            """)
    long countNonReversedPaymentsByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum total payments for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return total payment amount
     */
    @Query("""
            SELECT COALESCE(SUM(lp.paymentAmount), 0)
            FROM LoanPayment lp
            WHERE lp.loanAccount.id = :loanAccountId
            AND lp.isReversed = false
            """)
    BigDecimal sumPaymentsByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum principal paid for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return total principal paid
     */
    @Query("""
            SELECT COALESCE(SUM(lp.principalPaid), 0)
            FROM LoanPayment lp
            WHERE lp.loanAccount.id = :loanAccountId
            AND lp.isReversed = false
            """)
    BigDecimal sumPrincipalPaidByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum interest paid for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return total interest paid
     */
    @Query("""
            SELECT COALESCE(SUM(lp.interestPaid), 0)
            FROM LoanPayment lp
            WHERE lp.loanAccount.id = :loanAccountId
            AND lp.isReversed = false
            """)
    BigDecimal sumInterestPaidByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum payments by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @return total payment amount in the date range
     */
    @Query("""
            SELECT COALESCE(SUM(lp.paymentAmount), 0)
            FROM LoanPayment lp
            WHERE lp.paymentDate BETWEEN :startDate AND :endDate
            AND lp.isReversed = false
            """)
    BigDecimal sumPaymentsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find last payment for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return optional containing the last payment if found
     */
    @Query("""
            SELECT lp FROM LoanPayment lp
            WHERE lp.loanAccount.id = :loanAccountId
            AND lp.isReversed = false
            ORDER BY lp.paymentDate DESC, lp.createdAt DESC
            LIMIT 1
            """)
    Optional<LoanPayment> findLastPaymentByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Check if loan account has any payments.
     *
     * @param loanAccountId the loan account ID
     * @return true if there are payments
     */
    @Query("""
            SELECT COUNT(lp) > 0 FROM LoanPayment lp
            WHERE lp.loanAccount.id = :loanAccountId
            AND lp.isReversed = false
            """)
    boolean hasPayments(@Param("loanAccountId") UUID loanAccountId);
}
