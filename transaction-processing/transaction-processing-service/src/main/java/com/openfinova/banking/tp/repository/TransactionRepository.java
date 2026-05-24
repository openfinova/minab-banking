package com.openfinova.banking.tp.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.tp.api.dto.TransactionSummary;
import com.openfinova.banking.tp.api.entity.TransactionStatus;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.entity.Transaction;

/**
 * Repository for Transaction entities in the TP module.
 */
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    /**
     * Find transaction by its idempotency key from the request.
     * Optimized with JOIN FETCH to prevent N+1 problems.
     *
     * @param transactionKey the idempotency key
     * @return an optional containing the transaction if found
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            LEFT JOIN FETCH t.events
            LEFT JOIN FETCH t.reservations
            WHERE r.idempotencyKey = :transactionKey
            """)
    Optional<Transaction> findByTransactionKey(@Param("transactionKey") String transactionKey);

    /**
     * Find all transactions where the account is the source.
     *
     * @param sourceAccountId the account ID
     * @return list of transactions
     */
    List<Transaction> findBySourceAccountId(UUID sourceAccountId);

    /**
     * Find all transactions where the account is the destination.
     *
     * @param destinationAccountId the account ID
     * @return list of transactions
     */
    List<Transaction> findByDestinationAccountId(UUID destinationAccountId);

    // Comprehensive transaction lookup methods by account, status, date ranges

    /**
     * Find transactions by account ID (either source or destination) with pagination.
     *
     * @param accountId the account ID
     * @param pageable pagination information
     * @return page of transactions involving the account
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            WHERE t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByAccountId(@Param("accountId") UUID accountId, Pageable pageable);

    /**
     * Find transactions by status with pagination.
     *
     * @param status the transaction status
     * @param pageable pagination information
     * @return page of transactions with the specified status
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            WHERE t.status = :status
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByStatus(@Param("status") TransactionStatus status, Pageable pageable);

    /**
     * Find transactions by account and status.
     *
     * @param accountId the account ID
     * @param status the transaction status
     * @param pageable pagination information
     * @return page of transactions matching both criteria
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            WHERE (t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId)
            AND t.status = :status
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByAccountIdAndStatus(@Param("accountId") UUID accountId,
            @Param("status") TransactionStatus status, Pageable pageable);

    /**
     * Find transactions by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of transactions in the date range
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            WHERE t.transactionDate BETWEEN :startDate AND :endDate
            ORDER BY t.transactionDate DESC, t.createdAt DESC
            """)
    Page<Transaction> findByTransactionDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find transactions by account and date range.
     *
     * @param accountId the account ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of transactions for the account in the date range
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            WHERE (t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId)
            AND t.transactionDate BETWEEN :startDate AND :endDate
            ORDER BY t.transactionDate DESC, t.createdAt DESC
            """)
    Page<Transaction> findByAccountIdAndTransactionDateBetween(@Param("accountId") UUID accountId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find transactions by value date range.
     *
     * @param startDate start of value date range
     * @param endDate end of value date range
     * @param pageable pagination information
     * @return page of transactions with value dates in the range
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            WHERE t.valueDate BETWEEN :startDate AND :endDate
            ORDER BY t.valueDate DESC, t.createdAt DESC
            """)
    Page<Transaction> findByValueDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    // Optimized queries for transaction history and reporting

    /**
     * Find transaction history for an account with optimized fetching.
     *
     * @param accountId the account ID
     * @param limit maximum number of transactions to return
     * @return list of recent transactions for the account
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            LEFT JOIN FETCH t.events
            WHERE (t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId)
            ORDER BY t.createdAt DESC
            """)
    List<Transaction> findTransactionHistory(@Param("accountId") UUID accountId, Pageable pageable);

    /**
     * Find transactions by GL transaction ID.
     *
     * @param glTransactionId the GL transaction ID
     * @return optional containing the transaction if found
     */
    Optional<Transaction> findByGlTransactionId(UUID glTransactionId);

    /**
     * Find non-terminal transactions that started processing before the given cutoff.
     * Used by the timeout scheduler to fail long-running transactions.
     *
     * @param statuses non-terminal statuses (e.g. INITIATED, PENDING_RESERVATION, AUTHORIZED)
     * @param cutoff   only transactions with processingStartedAt before this time
     * @return list of timed-out transactions
     */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.status IN :statuses
            AND t.processingStartedAt IS NOT NULL
            AND t.processingStartedAt < :cutoff
            ORDER BY t.processingStartedAt ASC
            """)
    List<Transaction> findByStatusInAndProcessingStartedAtBefore(
            @Param("statuses") Collection<TransactionStatus> statuses, @Param("cutoff") LocalDateTime cutoff);

    /**
     * Find transactions by external reference.
     *
     * @param externalReference the external reference
     * @return list of transactions with the external reference
     */
    List<Transaction> findByExternalReference(String externalReference);

    /**
     * Find transactions by gateway transaction ID.
     *
     * @param gatewayTransactionId the gateway transaction ID
     * @return optional containing the transaction if found
     */
    Optional<Transaction> findByGatewayTransactionId(String gatewayTransactionId);

    /**
     * Find transactions by currency.
     *
     * @param currency the currency code
     * @param pageable pagination information
     * @return page of transactions in the specified currency
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            WHERE t.currency = :currency
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByCurrency(@Param("currency") String currency, Pageable pageable);

    /**
     * Find transactions by transaction type.
     *
     * @param transactionType the transaction type
     * @param pageable pagination information
     * @return page of transactions of the specified type
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            WHERE r.transactionType = :transactionType
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByTransactionType(@Param("transactionType") TransactionType transactionType,
            Pageable pageable);

    // Batch processing support for high-volume operations

    /**
     * Find transactions ready for batch processing (by status and processing time).
     *
     * @param status the transaction status
     * @param processingCutoff cutoff time for processing
     * @param limit maximum number of transactions to return
     * @return list of transactions ready for batch processing
     */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.status = :status
            AND t.processingStartedAt <= :processingCutoff
            ORDER BY t.processingStartedAt ASC
            """)
    List<Transaction> findTransactionsForBatchProcessing(@Param("status") TransactionStatus status,
            @Param("processingCutoff") LocalDateTime processingCutoff, Pageable pageable);

    /**
     * Update transaction status in batch.
     *
     * @param transactionIds list of transaction IDs to update
     * @param newStatus the new status
     * @param failureReason failure reason (if applicable)
     * @return number of transactions updated
     */
    @Modifying
    @Query("""
            UPDATE Transaction t
            SET t.status = :newStatus, t.failureReason = :failureReason, t.updatedAt = :updatedAt
            WHERE t.id IN :transactionIds
            """)
    int updateTransactionStatusBatch(@Param("transactionIds") List<UUID> transactionIds,
            @Param("newStatus") TransactionStatus newStatus, @Param("failureReason") String failureReason,
            @Param("updatedAt") Instant updatedAt);

    /**
     * Update GL transaction ID for multiple transactions.
     *
     * @param transactionIds list of transaction IDs
     * @param glTransactionId the GL transaction ID to set
     * @param updatedAt the timestamp to set for updatedAt (e.g. Instant.now())
     * @return number of transactions updated
     */
    @Modifying
    @Query("""
            UPDATE Transaction t
            SET t.glTransactionId = :glTransactionId, t.updatedAt = :updatedAt
            WHERE t.id IN :transactionIds
            """)
    int updateGlTransactionIdBatch(@Param("transactionIds") List<UUID> transactionIds,
            @Param("glTransactionId") UUID glTransactionId, @Param("updatedAt") Instant updatedAt);

    // Reporting and analytics queries

    /**
     * Find transactions with amounts greater than specified threshold.
     *
     * @param threshold the amount threshold
     * @param pageable pagination information
     * @return page of high-value transactions
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            WHERE r.amount >= :threshold
            ORDER BY r.amount DESC
            """)
    Page<Transaction> findHighValueTransactions(@Param("threshold") BigDecimal threshold, Pageable pageable);

    /**
     * Find failed transactions within a date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of failed transactions in the date range
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            WHERE t.status = 'FAILED'
            AND t.failedAt BETWEEN :startDate AND :endDate
            ORDER BY t.failedAt DESC
            """)
    Page<Transaction> findFailedTransactionsBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, Pageable pageable);

    /**
     * Find pending transactions older than specified time.
     *
     * @param cutoffTime cutoff time for pending transactions
     * @param pageable pagination information
     * @return page of stale pending transactions
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            WHERE t.status IN ('INITIATED', 'PENDING_RESERVATION', 'AUTHORIZED')
            AND t.processingStartedAt <= :cutoffTime
            ORDER BY t.processingStartedAt ASC
            """)
    Page<Transaction> findStalePendingTransactions(@Param("cutoffTime") LocalDateTime cutoffTime, Pageable pageable);

    // Summary and count methods

    /**
     * Count transactions by status.
     *
     * @param status the transaction status
     * @return count of transactions with the specified status
     */
    long countByStatus(TransactionStatus status);

    /**
     * Count transactions by account ID.
     *
     * @param accountId the account ID
     * @return count of transactions involving the account
     */
    @Query("""
            SELECT COUNT(t) FROM Transaction t
            WHERE t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId
            """)
    long countByAccountId(@Param("accountId") UUID accountId);

    /**
     * Sum transaction amounts by account and date range.
     *
     * @param accountId the account ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @return total transaction amount for the account in the date range
     */
    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM Transaction t
            JOIN t.request r
            WHERE (t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId)
            AND t.transactionDate BETWEEN :startDate AND :endDate
            AND t.status = 'POSTED'
            """)
    BigDecimal sumTransactionAmountsByAccountAndDateRange(@Param("accountId") UUID accountId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find transactions requiring compensation (failed after authorization).
     * Optimized with proper JOIN FETCH to prevent N+1 problems.
     *
     * @param pageable pagination information
     * @return page of transactions requiring compensation
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            LEFT JOIN FETCH t.events
            WHERE t.status = 'FAILED'
            AND EXISTS (SELECT 1 FROM TransactionEvent e WHERE e.transaction = t AND e.newStatus = 'AUTHORIZED')
            ORDER BY t.failedAt DESC
            """)
    Page<Transaction> findTransactionsRequiringCompensation(Pageable pageable);

    // Optimized batch queries to prevent N+1 problems

    /**
     * Find multiple transactions by IDs with events fetched.
     * Prevents N+1 problems when loading transaction batches.
     *
     * @param transactionIds list of transaction IDs
     * @return list of transactions with events loaded
     */
    @Query("""
            SELECT DISTINCT t FROM Transaction t
            JOIN FETCH t.request r
            LEFT JOIN FETCH t.events
            WHERE t.id IN :transactionIds
            """)
    List<Transaction> findByIdInWithEvents(@Param("transactionIds") List<UUID> transactionIds);

    /**
     * Find multiple transactions by IDs with reservations fetched.
     * Prevents N+1 problems when loading transaction batches.
     *
     * @param transactionIds list of transaction IDs
     * @return list of transactions with reservations loaded
     */
    @Query("""
            SELECT DISTINCT t FROM Transaction t
            LEFT JOIN FETCH t.reservations
            WHERE t.id IN :transactionIds
            """)
    List<Transaction> findByIdInWithReservations(@Param("transactionIds") List<UUID> transactionIds);

    /**
     * Find transactions by account with optimized fetching for batch operations.
     *
     * @param accountIds list of account IDs
     * @param pageable pagination information
     * @return page of transactions for the accounts
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.request r
            WHERE t.sourceAccountId IN :accountIds OR t.destinationAccountId IN :accountIds
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByAccountIdInBatch(@Param("accountIds") List<UUID> accountIds, Pageable pageable);

    /**
     * Find transactions by status with minimal data for reporting.
     * Optimized for large result sets by selecting only necessary fields.
     *
     * @param status the transaction status
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of transaction summaries
     */
    @Query("""
            SELECT new com.openfinova.banking.tp.api.dto.TransactionSummary(t.id, r.amount, t.currency, t.status, CAST(t.createdAt as LocalDateTime))
            FROM Transaction t JOIN t.request r
            WHERE t.status = :status
            AND t.createdAt BETWEEN :startDate AND :endDate
            """)
    List<TransactionSummary> findTransactionSummariesByStatusAndDateRange(@Param("status") TransactionStatus status,
            @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    /**
     * Find account transaction counts for multiple accounts efficiently.
     * Optimized for batch account processing.
     *
     * @param accountIds list of account IDs
     * @return map of account ID to transaction count
     */
    @Query("""
            SELECT t.sourceAccountId as accountId, COUNT(t) as transactionCount
            FROM Transaction t
            WHERE t.sourceAccountId IN :accountIds
            GROUP BY t.sourceAccountId
            """)
    List<Object[]> countTransactionsByAccountIds(@Param("accountIds") List<UUID> accountIds);
}
