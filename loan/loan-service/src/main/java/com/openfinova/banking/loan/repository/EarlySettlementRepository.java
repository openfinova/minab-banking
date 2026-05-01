package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.SettlementStatus;
import com.openfinova.banking.loan.entity.EarlySettlement;
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
 * Repository for EarlySettlement entities.
 */
public interface EarlySettlementRepository extends JpaRepository<EarlySettlement, UUID> {

    /**
     * Find all early settlements for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of early settlements for the loan account
     */
    @Query("""
            SELECT es FROM EarlySettlement es
            WHERE es.loanAccount.id = :loanAccountId
            ORDER BY es.quoteDate DESC
            """)
    List<EarlySettlement> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find early settlements by loan account and status.
     *
     * @param loanAccountId the loan account ID
     * @param status the settlement status
     * @return list of early settlements matching both criteria
     */
    @Query("""
            SELECT es FROM EarlySettlement es
            WHERE es.loanAccount.id = :loanAccountId
            AND es.status = :status
            ORDER BY es.quoteDate DESC
            """)
    List<EarlySettlement> findByLoanAccountIdAndStatus(@Param("loanAccountId") UUID loanAccountId,
            @Param("status") SettlementStatus status);

    /**
     * Find early settlements by status.
     *
     * @param status the settlement status
     * @param pageable pagination information
     * @return page of early settlements with the specified status
     */
    Page<EarlySettlement> findByStatus(SettlementStatus status, Pageable pageable);

    Page<EarlySettlement> findByLoanAccount_IdAndStatus(UUID loanAccountId, SettlementStatus status, Pageable pageable);

    long countByLoanAccount_IdAndStatus(UUID loanAccountId, SettlementStatus status);

    /**
     * Find early settlements by quote date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of early settlements in the date range
     */
    @Query("""
            SELECT es FROM EarlySettlement es
            WHERE es.quoteDate BETWEEN :startDate AND :endDate
            ORDER BY es.quoteDate DESC
            """)
    Page<EarlySettlement> findByQuoteDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find pending early settlements.
     *
     * @param pageable pagination information
     * @return page of pending early settlements
     */
    @Query("""
            SELECT es FROM EarlySettlement es
            WHERE es.status = 'PENDING_APPROVAL'
            ORDER BY es.quoteDate ASC
            """)
    Page<EarlySettlement> findPendingSettlements(Pageable pageable);

    /**
     * Find approved early settlements.
     *
     * @param pageable pagination information
     * @return page of approved early settlements
     */
    @Query("""
            SELECT es FROM EarlySettlement es
            WHERE es.status = 'APPROVED'
            ORDER BY es.quoteDate DESC
            """)
    Page<EarlySettlement> findApprovedSettlements(Pageable pageable);

    /**
     * Find completed early settlements.
     *
     * @param pageable pagination information
     * @return page of completed early settlements
     */
    @Query("""
            SELECT es FROM EarlySettlement es
            WHERE es.status = 'COMPLETED'
            ORDER BY es.settledDate DESC
            """)
    Page<EarlySettlement> findCompletedSettlements(Pageable pageable);

    /**
     * Find latest early settlement for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return optional containing the latest early settlement if found
     */
    @Query("""
            SELECT es FROM EarlySettlement es
            WHERE es.loanAccount.id = :loanAccountId
            ORDER BY es.quoteDate DESC, es.createdAt DESC
            LIMIT 1
            """)
    Optional<EarlySettlement> findLatestSettlementByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find latest early settlement for a loan account (alternative method name).
     *
     * @param loanAccountId the loan account ID
     * @return optional containing the latest early settlement if found
     */
    @Query("""
            SELECT es FROM EarlySettlement es
            WHERE es.loanAccount.id = :loanAccountId
            ORDER BY es.createdAt DESC
            LIMIT 1
            """)
    Optional<EarlySettlement> findTopByLoanAccountIdOrderByCreatedAtDesc(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find active early settlement quote for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return optional containing the active quote if found
     */
    @Query("""
            SELECT es FROM EarlySettlement es
            WHERE es.loanAccount.id = :loanAccountId
            AND es.status IN ('PENDING_APPROVAL', 'APPROVED')
            AND es.validUntil >= CURRENT_DATE
            ORDER BY es.quoteDate DESC
            LIMIT 1
            """)
    Optional<EarlySettlement> findActiveQuoteByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Count early settlements for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of early settlements
     */
    @Query("""
            SELECT COUNT(es) FROM EarlySettlement es
            WHERE es.loanAccount.id = :loanAccountId
            """)
    long countByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum settlement amounts by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @return total settlement amount in the date range
     */
    @Query("""
            SELECT COALESCE(SUM(es.settlementAmount), 0)
            FROM EarlySettlement es
            WHERE es.settledDate BETWEEN :startDate AND :endDate
            AND es.status = 'COMPLETED'
            """)
    BigDecimal sumSettlementsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Check if loan account has active early settlement quote.
     *
     * @param loanAccountId the loan account ID
     * @return true if there is an active quote
     */
    @Query("""
            SELECT COUNT(es) > 0 FROM EarlySettlement es
            WHERE es.loanAccount.id = :loanAccountId
            AND es.status IN ('PENDING_APPROVAL', 'APPROVED')
            AND es.validUntil >= CURRENT_DATE
            """)
    boolean hasActiveQuote(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Check if loan account has early settlement with specific status.
     *
     * @param loanAccountId the loan account ID
     * @param status the settlement status
     * @return true if there is a settlement with the specified status
     */
    @Query("""
            SELECT COUNT(es) > 0 FROM EarlySettlement es
            WHERE es.loanAccount.id = :loanAccountId
            AND es.status = :status
            """)
    boolean existsByLoanAccountIdAndStatus(@Param("loanAccountId") UUID loanAccountId,
            @Param("status") SettlementStatus status);
}
