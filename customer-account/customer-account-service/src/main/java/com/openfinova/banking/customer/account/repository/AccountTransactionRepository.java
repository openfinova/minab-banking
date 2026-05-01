package com.openfinova.banking.customer.account.repository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.customer.account.entity.AccountTransaction;

import jakarta.persistence.QueryHint;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {

    /**
     * Count pending transactions for an account.
     *
     * @param accountId the account ID
     * @return count of pending transactions
     */
    @Query("SELECT COUNT(t) FROM AccountTransaction t WHERE t.customerAccount.id = :accountId AND t.status = 'PENDING'")
    long countPendingTransactionsByAccount(@Param("accountId") UUID accountId);

    /**
     * Check if account has transactions since a specific date.
     *
     * @param accountId the account ID
     * @param sinceDate the date to check from
     * @return true if there are transactions since the date
     */
    @Query("SELECT COUNT(t) > 0 FROM AccountTransaction t WHERE t.customerAccount.id = :accountId AND t.transactionDate > :sinceDate")
    boolean hasTransactionsSince(@Param("accountId") UUID accountId, @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Find all transactions for an account within a date range with pagination.
     *
     * This query uses indexed columns for optimal performance:
     * - customer_account_id (indexed via idx_acc_trx_account)
     * - transaction_date (indexed via idx_acc_trx_date)
     *
     * The date range is inclusive on both ends.
     *
     * Sorting can be customized via the Pageable parameter. If no sort is specified,
     * results are returned in database order (consider adding default sort in the query
     * or always specify sort in Pageable).
     *
     * @param accountId the account ID
     * @param fromDate  the start of the date range (inclusive)
     * @param toDate    the end of the date range (inclusive)
     * @param pageable  pagination and sorting parameters
     * @return paginated list of transactions
     */
    @Query("""
            SELECT t FROM AccountTransaction t
            WHERE t.customerAccount.id = :accountId
            AND t.transactionDate >= :fromDate
            AND t.transactionDate <= :toDate
            """)
    Page<AccountTransaction> findByAccountAndDateRange(@Param("accountId") UUID accountId,
            @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate, Pageable pageable);

    /**
     * Stream all transactions for an account within a date range, ordered by transaction date.
     * Uses fetch size hint so drivers (e.g. MySQL, PostgreSQL) stream rows instead of loading all into memory.
     * Caller must consume or close the stream within the same transaction.
     *
     * @param accountId the account ID
     * @param fromDate  the start of the date range (inclusive)
     * @param toDate    the end of the date range (inclusive)
     * @return stream of transactions, must be closed after use
     */
    @Query("""
            SELECT t FROM AccountTransaction t
            WHERE t.customerAccount.id = :accountId
            AND t.transactionDate >= :fromDate
            AND t.transactionDate <= :toDate
            ORDER BY t.transactionDate ASC
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "500"))
    Stream<AccountTransaction> streamByAccountAndDateRange(@Param("accountId") UUID accountId,
            @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);
}
