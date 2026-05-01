package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.LoanFeeType;
import com.openfinova.banking.loan.entity.LoanFee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for LoanFee entities.
 */
public interface LoanFeeRepository extends JpaRepository<LoanFee, UUID> {

    /**
     * Find all fees for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of fees for the loan account
     */
    @Query("""
            SELECT lf FROM LoanFee lf
            WHERE lf.loanAccount.id = :loanAccountId
            ORDER BY lf.chargeDate DESC
            """)
    List<LoanFee> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find fees by loan account and fee type.
     *
     * @param loanAccountId the loan account ID
     * @param feeType the fee type
     * @return list of fees matching both criteria
     */
    @Query("""
            SELECT lf FROM LoanFee lf
            WHERE lf.loanAccount.id = :loanAccountId
            AND lf.feeType = :feeType
            ORDER BY lf.chargeDate DESC
            """)
    List<LoanFee> findByLoanAccountIdAndFeeType(@Param("loanAccountId") UUID loanAccountId,
            @Param("feeType") LoanFeeType feeType);

    /**
     * Find fees by fee type.
     *
     * @param feeType the fee type
     * @param pageable pagination information
     * @return page of fees of the specified type
     */
    Page<LoanFee> findByFeeType(LoanFeeType feeType, Pageable pageable);

    /**
     * Find fees by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of fees in the date range
     */
    @Query("""
            SELECT lf FROM LoanFee lf
            WHERE lf.chargeDate BETWEEN :startDate AND :endDate
            ORDER BY lf.chargeDate DESC
            """)
    Page<LoanFee> findByChargedDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * Find waived fees for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of waived fees
     */
    @Query("""
            SELECT lf FROM LoanFee lf
            WHERE lf.loanAccount.id = :loanAccountId
            AND lf.isWaived = true
            ORDER BY lf.chargeDate DESC
            """)
    List<LoanFee> findWaivedFeesByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find non-waived fees for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of non-waived fees
     */
    @Query("""
            SELECT lf FROM LoanFee lf
            WHERE lf.loanAccount.id = :loanAccountId
            AND lf.isWaived = false
            ORDER BY lf.chargeDate DESC
            """)
    List<LoanFee> findNonWaivedFeesByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Count fees for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of fees
     */
    @Query("""
            SELECT COUNT(lf) FROM LoanFee lf
            WHERE lf.loanAccount.id = :loanAccountId
            """)
    long countByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum fee amounts for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return total fee amount
     */
    @Query("""
            SELECT COALESCE(SUM(lf.feeAmount), 0)
            FROM LoanFee lf
            WHERE lf.loanAccount.id = :loanAccountId
            AND lf.isWaived = false
            """)
    BigDecimal sumFeesByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum fees by type for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @param feeType the fee type
     * @return total fee amount
     */
    @Query("""
            SELECT COALESCE(SUM(lf.feeAmount), 0)
            FROM LoanFee lf
            WHERE lf.loanAccount.id = :loanAccountId
            AND lf.feeType = :feeType
            AND lf.isWaived = false
            """)
    BigDecimal sumFeesByLoanAccountAndType(@Param("loanAccountId") UUID loanAccountId,
            @Param("feeType") LoanFeeType feeType);

    /**
     * Sum fees by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @return total fee amount in the date range
     */
    @Query("""
            SELECT COALESCE(SUM(lf.feeAmount), 0)
            FROM LoanFee lf
            WHERE lf.chargeDate BETWEEN :startDate AND :endDate
            AND lf.isWaived = false
            """)
    BigDecimal sumFeesByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
