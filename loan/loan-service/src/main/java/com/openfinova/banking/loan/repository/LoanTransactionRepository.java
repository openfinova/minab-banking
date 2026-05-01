package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.LoanTransactionType;
import com.openfinova.banking.loan.entity.LoanTransaction;
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
 * Repository for LoanTransaction entities.
 */
public interface LoanTransactionRepository extends JpaRepository<LoanTransaction, UUID> {

    /**
     * Find loan transaction by transaction reference.
     *
     * @param transactionReference the unique transaction reference
     * @return optional containing the transaction if found
     */
    Optional<LoanTransaction> findByTransactionReference(String transactionReference);

    /**
     * Find all transactions for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @param pageable pagination information
     * @return page of transactions for the loan account
     */
    @Query("""
            SELECT lt FROM LoanTransaction lt
            WHERE lt.loanAccount.id = :loanAccountId
            ORDER BY lt.transactionDate DESC, lt.createdAt DESC
            """)
    Page<LoanTransaction> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId, Pageable pageable);

    /**
     * Find all transactions for a loan account (non-paginated).
     *
     * @param loanAccountId the loan account ID
     * @return list of transactions for the loan account
     */
    @Query("""
            SELECT lt FROM LoanTransaction lt
            WHERE lt.loanAccount.id = :loanAccountId
            ORDER BY lt.transactionDate DESC, lt.createdAt DESC
            """)
    List<LoanTransaction> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find transactions by loan account and transaction type.
     *
     * @param loanAccountId the loan account ID
     * @param transactionType the transaction type
     * @param pageable pagination information
     * @return page of transactions matching both criteria
     */
    @Query("""
            SELECT lt FROM LoanTransaction lt
            WHERE lt.loanAccount.id = :loanAccountId
            AND lt.transactionType = :transactionType
            ORDER BY lt.transactionDate DESC
            """)
    Page<LoanTransaction> findByLoanAccountIdAndTransactionType(@Param("loanAccountId") UUID loanAccountId,
            @Param("transactionType") LoanTransactionType transactionType, Pageable pageable);

    /**
     * Find transactions by loan account and date range.
     *
     * @param loanAccountId the loan account ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of transactions in the date range
     */
    @Query("""
            SELECT lt FROM LoanTransaction lt
            WHERE lt.loanAccount.id = :loanAccountId
            AND lt.transactionDate BETWEEN :startDate AND :endDate
            ORDER BY lt.transactionDate DESC
            """)
    Page<LoanTransaction> findByLoanAccountIdAndTransactionDateBetween(@Param("loanAccountId") UUID loanAccountId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find transactions by transaction type.
     *
     * @param transactionType the transaction type
     * @param pageable pagination information
     * @return page of transactions of the specified type
     */
    @Query("""
            SELECT lt FROM LoanTransaction lt
            WHERE lt.transactionType = :transactionType
            ORDER BY lt.transactionDate DESC
            """)
    Page<LoanTransaction> findByTransactionType(@Param("transactionType") LoanTransactionType transactionType,
            Pageable pageable);

    /**
     * Find transactions by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of transactions in the date range
     */
    @Query("""
            SELECT lt FROM LoanTransaction lt
            WHERE lt.transactionDate BETWEEN :startDate AND :endDate
            ORDER BY lt.transactionDate DESC
            """)
    Page<LoanTransaction> findByTransactionDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find reversed transactions.
     *
     * @param pageable pagination information
     * @return page of reversed transactions
     */
    @Query("""
            SELECT lt FROM LoanTransaction lt
            WHERE lt.isReversed = true
            ORDER BY lt.reversedAt DESC
            """)
    Page<LoanTransaction> findReversedTransactions(Pageable pageable);

    /**
     * Find non-reversed transactions for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of non-reversed transactions
     */
    @Query("""
            SELECT lt FROM LoanTransaction lt
            WHERE lt.loanAccount.id = :loanAccountId
            AND lt.isReversed = false
            ORDER BY lt.transactionDate DESC
            """)
    List<LoanTransaction> findNonReversedTransactionsByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find transactions with amount greater than specified threshold.
     *
     * @param minAmount minimum transaction amount
     * @param pageable pagination information
     * @return page of high-value transactions
     */
    @Query("""
            SELECT lt FROM LoanTransaction lt
            WHERE lt.amount >= :minAmount
            ORDER BY lt.amount DESC
            """)
    Page<LoanTransaction> findByAmountGreaterThanEqual(@Param("minAmount") BigDecimal minAmount, Pageable pageable);

    /**
     * Count transactions for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of transactions
     */
    @Query("""
            SELECT COUNT(lt) FROM LoanTransaction lt
            WHERE lt.loanAccount.id = :loanAccountId
            """)
    long countByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Count transactions by type for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @param transactionType the transaction type
     * @return count of transactions
     */
    @Query("""
            SELECT COUNT(lt) FROM LoanTransaction lt
            WHERE lt.loanAccount.id = :loanAccountId
            AND lt.transactionType = :transactionType
            """)
    long countByLoanAccountIdAndTransactionType(@Param("loanAccountId") UUID loanAccountId,
            @Param("transactionType") LoanTransactionType transactionType);

    /**
     * Sum transaction amounts for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return total transaction amount
     */
    @Query("""
            SELECT COALESCE(SUM(lt.amount), 0)
            FROM LoanTransaction lt
            WHERE lt.loanAccount.id = :loanAccountId
            AND lt.isReversed = false
            """)
    BigDecimal sumTransactionsByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum transaction amounts by type for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @param transactionType the transaction type
     * @return total transaction amount
     */
    @Query("""
            SELECT COALESCE(SUM(lt.amount), 0)
            FROM LoanTransaction lt
            WHERE lt.loanAccount.id = :loanAccountId
            AND lt.transactionType = :transactionType
            AND lt.isReversed = false
            """)
    BigDecimal sumTransactionsByLoanAccountAndType(@Param("loanAccountId") UUID loanAccountId,
            @Param("transactionType") LoanTransactionType transactionType);

    /**
     * Sum transaction amounts by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @return total transaction amount in the date range
     */
    @Query("""
            SELECT COALESCE(SUM(lt.amount), 0)
            FROM LoanTransaction lt
            WHERE lt.transactionDate BETWEEN :startDate AND :endDate
            AND lt.isReversed = false
            """)
    BigDecimal sumTransactionsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find last transaction for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return optional containing the last transaction if found
     */
    @Query("""
            SELECT lt FROM LoanTransaction lt
            WHERE lt.loanAccount.id = :loanAccountId
            AND lt.isReversed = false
            ORDER BY lt.transactionDate DESC, lt.createdAt DESC
            LIMIT 1
            """)
    Optional<LoanTransaction> findLastTransactionByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Check if loan account has any transactions.
     *
     * @param loanAccountId the loan account ID
     * @return true if there are transactions
     */
    @Query("""
            SELECT COUNT(lt) > 0 FROM LoanTransaction lt
            WHERE lt.loanAccount.id = :loanAccountId
            AND lt.isReversed = false
            """)
    boolean hasTransactions(@Param("loanAccountId") UUID loanAccountId);
}
