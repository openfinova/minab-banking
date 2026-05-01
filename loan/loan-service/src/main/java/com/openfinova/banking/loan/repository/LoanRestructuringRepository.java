package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.RestructuringType;
import com.openfinova.banking.loan.entity.LoanRestructuring;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LoanRestructuring entities.
 */
public interface LoanRestructuringRepository extends JpaRepository<LoanRestructuring, UUID> {

    /**
     * Find all restructurings for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of restructurings for the loan account
     */
    @Query("""
            SELECT lr FROM LoanRestructuring lr
            WHERE lr.loanAccount.id = :loanAccountId
            ORDER BY lr.restructuringDate DESC
            """)
    List<LoanRestructuring> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find restructurings by loan account and restructuring type.
     *
     * @param loanAccountId the loan account ID
     * @param restructuringType the restructuring type
     * @return list of restructurings matching both criteria
     */
    @Query("""
            SELECT lr FROM LoanRestructuring lr
            WHERE lr.loanAccount.id = :loanAccountId
            AND lr.restructuringType = :restructuringType
            ORDER BY lr.restructuringDate DESC
            """)
    List<LoanRestructuring> findByLoanAccountIdAndRestructuringType(@Param("loanAccountId") UUID loanAccountId,
            @Param("restructuringType") RestructuringType restructuringType);

    /**
     * Find restructurings by restructuring type.
     *
     * @param restructuringType the restructuring type
     * @param pageable pagination information
     * @return page of restructurings of the specified type
     */
    Page<LoanRestructuring> findByRestructuringType(RestructuringType restructuringType, Pageable pageable);

    /**
     * Find restructurings by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of restructurings in the date range
     */
    @Query("""
            SELECT lr FROM LoanRestructuring lr
            WHERE lr.restructuringDate BETWEEN :startDate AND :endDate
            ORDER BY lr.restructuringDate DESC
            """)
    Page<LoanRestructuring> findByRestructuringDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find latest restructuring for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return optional containing the latest restructuring if found
     */
    @Query("""
            SELECT lr FROM LoanRestructuring lr
            WHERE lr.loanAccount.id = :loanAccountId
            ORDER BY lr.restructuringDate DESC, lr.createdAt DESC
            LIMIT 1
            """)
    Optional<LoanRestructuring> findLatestRestructuringByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find latest restructuring for a loan account (alternative method name).
     *
     * @param loanAccountId the loan account ID
     * @return optional containing the latest restructuring if found
     */
    @Query("""
            SELECT lr FROM LoanRestructuring lr
            WHERE lr.loanAccount.id = :loanAccountId
            ORDER BY lr.restructuringDate DESC
            LIMIT 1
            """)
    Optional<LoanRestructuring> findTopByLoanAccountIdOrderByRestructuringDateDesc(
            @Param("loanAccountId") UUID loanAccountId);

    /**
     * Count restructurings for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of restructurings
     */
    @Query("""
            SELECT COUNT(lr) FROM LoanRestructuring lr
            WHERE lr.loanAccount.id = :loanAccountId
            """)
    long countByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Check if loan account has been restructured.
     *
     * @param loanAccountId the loan account ID
     * @return true if there are restructurings
     */
    @Query("""
            SELECT COUNT(lr) > 0 FROM LoanRestructuring lr
            WHERE lr.loanAccount.id = :loanAccountId
            """)
    boolean hasRestructurings(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Check if loan account has been restructured (alternative method name).
     *
     * @param loanAccountId the loan account ID
     * @return true if there are restructurings
     */
    @Query("""
            SELECT COUNT(lr) > 0 FROM LoanRestructuring lr
            WHERE lr.loanAccount.id = :loanAccountId
            """)
    boolean existsByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);
}
