package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.entity.InterestAccrual;
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
 * Repository for InterestAccrual entities.
 */
public interface InterestAccrualRepository extends JpaRepository<InterestAccrual, UUID> {

    boolean existsByLoanAccount_IdAndAccrualDate(UUID loanAccountId, LocalDate accrualDate);

    /**
     * Find all interest accruals for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of interest accruals for the loan account
     */
    @Query("""
            SELECT ia FROM InterestAccrual ia
            WHERE ia.loanAccount.id = :loanAccountId
            ORDER BY ia.accrualDate DESC
            """)
    List<InterestAccrual> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find interest accruals by loan account and date range.
     *
     * @param loanAccountId the loan account ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of interest accruals in the date range
     */
    @Query("""
            SELECT ia FROM InterestAccrual ia
            WHERE ia.loanAccount.id = :loanAccountId
            AND ia.accrualDate BETWEEN :startDate AND :endDate
            ORDER BY ia.accrualDate DESC
            """)
    List<InterestAccrual> findByLoanAccountIdAndAccrualDateBetween(@Param("loanAccountId") UUID loanAccountId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find interest accruals by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of interest accruals in the date range
     */
    @Query("""
            SELECT ia FROM InterestAccrual ia
            WHERE ia.accrualDate BETWEEN :startDate AND :endDate
            ORDER BY ia.accrualDate DESC
            """)
    Page<InterestAccrual> findByAccrualDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find interest accruals for a specific date.
     *
     * @param accrualDate the accrual date
     * @param pageable pagination information
     * @return page of interest accruals for the date
     */
    @Query("""
            SELECT ia FROM InterestAccrual ia
            WHERE ia.accrualDate = :accrualDate
            ORDER BY ia.loanAccount.id
            """)
    Page<InterestAccrual> findByAccrualDate(@Param("accrualDate") LocalDate accrualDate, Pageable pageable);

    /**
     * Sum interest accrued for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return total interest accrued
     */
    @Query("""
            SELECT COALESCE(SUM(ia.accrualAmount), 0)
            FROM InterestAccrual ia
            WHERE ia.loanAccount.id = :loanAccountId
            """)
    BigDecimal sumInterestAccruedByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum interest accrued by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @return total interest accrued in the date range
     */
    @Query("""
            SELECT COALESCE(SUM(ia.accrualAmount), 0)
            FROM InterestAccrual ia
            WHERE ia.accrualDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumInterestAccruedByDateRange(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count interest accruals for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of interest accruals
     */
    @Query("""
            SELECT COUNT(ia) FROM InterestAccrual ia
            WHERE ia.loanAccount.id = :loanAccountId
            """)
    long countByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);
}
