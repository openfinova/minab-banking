package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.DisbursementMethod;
import com.openfinova.banking.loan.api.entity.DisbursementStatus;
import com.openfinova.banking.loan.entity.LoanDisbursement;
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
 * Repository for LoanDisbursement entities.
 */
public interface LoanDisbursementRepository extends JpaRepository<LoanDisbursement, UUID> {

    /**
     * Find loan disbursement by disbursement reference.
     *
     * @param disbursementReference the unique disbursement reference
     * @return optional containing the disbursement if found
     */
    Optional<LoanDisbursement> findByDisbursementReference(String disbursementReference);

    /**
     * Find all disbursements for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of disbursements for the loan account
     */
    @Query("""
            SELECT ld FROM LoanDisbursement ld
            WHERE ld.loanAccount.id = :loanAccountId
            ORDER BY ld.disbursementDate DESC
            """)
    List<LoanDisbursement> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find disbursements by loan account and status.
     *
     * @param loanAccountId the loan account ID
     * @param status the disbursement status
     * @return list of disbursements matching both criteria
     */
    @Query("""
            SELECT ld FROM LoanDisbursement ld
            WHERE ld.loanAccount.id = :loanAccountId
            AND ld.status = :status
            ORDER BY ld.disbursementDate DESC
            """)
    List<LoanDisbursement> findByLoanAccountIdAndStatus(@Param("loanAccountId") UUID loanAccountId,
            @Param("status") DisbursementStatus status);

    /**
     * Find disbursements by status.
     *
     * @param status the disbursement status
     * @param pageable pagination information
     * @return page of disbursements with the specified status
     */
    Page<LoanDisbursement> findByStatus(DisbursementStatus status, Pageable pageable);

    Page<LoanDisbursement> findByLoanAccount_IdAndStatus(UUID loanAccountId, DisbursementStatus status,
            Pageable pageable);

    long countByLoanAccount_IdAndStatus(UUID loanAccountId, DisbursementStatus status);

    /**
     * Find disbursements by disbursement method.
     *
     * @param disbursementMethod the disbursement method
     * @param pageable pagination information
     * @return page of disbursements using the specified method
     */
    Page<LoanDisbursement> findByDisbursementMethod(DisbursementMethod disbursementMethod, Pageable pageable);

    /**
     * Find disbursements by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of disbursements in the date range
     */
    @Query("""
            SELECT ld FROM LoanDisbursement ld
            WHERE ld.disbursementDate BETWEEN :startDate AND :endDate
            ORDER BY ld.disbursementDate DESC
            """)
    Page<LoanDisbursement> findByDisbursementDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    Page<LoanDisbursement> findByLoanAccount_IdAndDisbursementDateBetween(UUID loanAccountId, LocalDate startDate,
            LocalDate endDate, Pageable pageable);

    /**
     * Find pending disbursements.
     *
     * @param pageable pagination information
     * @return page of pending disbursements
     */
    @Query("""
            SELECT ld FROM LoanDisbursement ld
            WHERE ld.status = 'PENDING'
            ORDER BY ld.disbursementDate ASC
            """)
    Page<LoanDisbursement> findPendingDisbursements(Pageable pageable);

    /**
     * Find completed disbursements for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of completed disbursements
     */
    @Query("""
            SELECT ld FROM LoanDisbursement ld
            WHERE ld.loanAccount.id = :loanAccountId
            AND ld.status = 'COMPLETED'
            ORDER BY ld.disbursementDate DESC
            """)
    List<LoanDisbursement> findCompletedDisbursementsByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Count disbursements for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of disbursements
     */
    @Query("""
            SELECT COUNT(ld) FROM LoanDisbursement ld
            WHERE ld.loanAccount.id = :loanAccountId
            """)
    long countByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum disbursement amounts for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return total disbursement amount
     */
    @Query("""
            SELECT COALESCE(SUM(ld.disbursementAmount), 0)
            FROM LoanDisbursement ld
            WHERE ld.loanAccount.id = :loanAccountId
            AND ld.status = 'COMPLETED'
            """)
    BigDecimal sumDisbursementsByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum disbursements by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @return total disbursement amount in the date range
     */
    @Query("""
            SELECT COALESCE(SUM(ld.disbursementAmount), 0)
            FROM LoanDisbursement ld
            WHERE ld.disbursementDate BETWEEN :startDate AND :endDate
            AND ld.status = 'COMPLETED'
            """)
    BigDecimal sumDisbursementsByDateRange(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Check if loan account has completed disbursements.
     *
     * @param loanAccountId the loan account ID
     * @return true if there are completed disbursements
     */
    @Query("""
            SELECT COUNT(ld) > 0 FROM LoanDisbursement ld
            WHERE ld.loanAccount.id = :loanAccountId
            AND ld.status = 'COMPLETED'
            """)
    boolean hasCompletedDisbursements(@Param("loanAccountId") UUID loanAccountId);
}
