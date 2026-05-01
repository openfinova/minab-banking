package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.ProvisionStage;
import com.openfinova.banking.loan.entity.LoanProvision;
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
 * Repository for LoanProvision entities.
 */
public interface LoanProvisionRepository extends JpaRepository<LoanProvision, UUID> {

    /**
     * Find all provisions for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of provisions for the loan account
     */
    @Query("""
            SELECT lp FROM LoanProvision lp
            WHERE lp.loanAccount.id = :loanAccountId
            ORDER BY lp.provisionDate DESC
            """)
    List<LoanProvision> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find provisions by loan account and provision stage.
     *
     * @param loanAccountId the loan account ID
     * @param provisionStage the provision stage
     * @return list of provisions matching both criteria
     */
    @Query("""
            SELECT lp FROM LoanProvision lp
            WHERE lp.loanAccount.id = :loanAccountId
            AND lp.provisionStage = :provisionStage
            ORDER BY lp.provisionDate DESC
            """)
    List<LoanProvision> findByLoanAccountIdAndProvisionStage(@Param("loanAccountId") UUID loanAccountId,
            @Param("provisionStage") ProvisionStage provisionStage);

    /**
     * Find provisions by provision stage.
     *
     * @param provisionStage the provision stage
     * @param pageable pagination information
     * @return page of provisions in the specified stage
     */
    Page<LoanProvision> findByProvisionStage(ProvisionStage provisionStage, Pageable pageable);

    /**
     * Find provisions by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of provisions in the date range
     */
    @Query("""
            SELECT lp FROM LoanProvision lp
            WHERE lp.provisionDate BETWEEN :startDate AND :endDate
            ORDER BY lp.provisionDate DESC
            """)
    Page<LoanProvision> findByProvisionDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find latest provision for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return optional containing the latest provision if found
     */
    @Query("""
            SELECT lp FROM LoanProvision lp
            WHERE lp.loanAccount.id = :loanAccountId
            ORDER BY lp.provisionDate DESC, lp.createdAt DESC
            LIMIT 1
            """)
    Optional<LoanProvision> findLatestProvisionByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum provision amounts for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return total provision amount
     */
    @Query("""
            SELECT COALESCE(SUM(lp.provisionAmount), 0)
            FROM LoanProvision lp
            WHERE lp.loanAccount.id = :loanAccountId
            """)
    BigDecimal sumProvisionsByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum provisions by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @return total provision amount in the date range
     */
    @Query("""
            SELECT COALESCE(SUM(lp.provisionAmount), 0)
            FROM LoanProvision lp
            WHERE lp.provisionDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumProvisionsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Count provisions for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of provisions
     */
    @Query("""
            SELECT COUNT(lp) FROM LoanProvision lp
            WHERE lp.loanAccount.id = :loanAccountId
            """)
    long countByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);
}
