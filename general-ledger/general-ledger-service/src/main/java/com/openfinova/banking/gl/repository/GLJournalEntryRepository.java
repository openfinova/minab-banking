package com.openfinova.banking.gl.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.gl.entity.GLJournalEntry;

public interface GLJournalEntryRepository extends JpaRepository<GLJournalEntry, UUID> {

    /**
     * Find all posted journal entries for a specific account.
     *
     * @param accountId the GL account ID
     * @return list of posted journal entries for the account
     */
    @Query("""
            SELECT je FROM GLJournalEntry je
            JOIN je.transaction t
            WHERE je.account.id = :accountId
            AND t.status = 'POSTED'
            ORDER BY je.valueDate ASC
            """)
    List<GLJournalEntry> findEntriesByAccount(@Param("accountId") UUID accountId);

    /**
     * Find journal entries for a specific account and date.
     *
     * @param accountId the GL account ID
     * @param date the transaction date
     * @return list of journal entries for the account on the date
     */
    @Query("""
            SELECT je FROM GLJournalEntry je
            JOIN je.transaction t
            WHERE je.account.id = :accountId
            AND je.valueDate = :date
            AND t.status = 'POSTED'
            ORDER BY je.valueDate ASC
            """)
    List<GLJournalEntry> findEntriesByAccountAndDate(@Param("accountId") UUID accountId, @Param("date") LocalDate date);

    /**
     * Find journal entries for a specific account within a date range.
     *
     * @param accountId the GL account ID
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of journal entries for the account in the date range
     */
    @Query("""
            SELECT je FROM GLJournalEntry je
            JOIN je.transaction t
            WHERE je.account.id = :accountId
            AND je.valueDate BETWEEN :startDate AND :endDate
            AND t.status = 'POSTED'
            ORDER BY je.valueDate ASC
            """)
    List<GLJournalEntry> findEntriesByAccountAndDateRange(@Param("accountId") UUID accountId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find journal entries for a specific account up to a date.
     *
     * @param accountId the GL account ID
     * @param toDate the end date (inclusive)
     * @return list of journal entries for the account up to the date
     */
    @Query("""
            SELECT je FROM GLJournalEntry je
            JOIN je.transaction t
            WHERE je.account.id = :accountId
            AND je.valueDate <= :toDate
            AND t.status = 'POSTED'
            ORDER BY je.valueDate ASC
            """)
    List<GLJournalEntry> findEntriesByAccountUpToDate(@Param("accountId") UUID accountId,
            @Param("toDate") LocalDate toDate);

    /**
     * Find journal entries for a specific account, date range, and currency.
     *
     * @param accountId the GL account ID
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param currency the currency code
     * @return list of journal entries matching the criteria
     */
    @Query("""
            SELECT je FROM GLJournalEntry je
            JOIN je.transaction t
            WHERE je.account.id = :accountId
            AND je.valueDate BETWEEN :startDate AND :endDate
            AND je.currency = :currency
            AND t.status = 'POSTED'
            ORDER BY je.valueDate ASC
            """)
    List<GLJournalEntry> findEntriesByAccountDateRangeAndCurrency(@Param("accountId") UUID accountId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            @Param("currency") String currency);

    /**
     * Efficiently checks whether at least one posted journal entry exists for the given account.
     * Uses a targeted EXISTS sub-query to avoid hydrating any rows.
     *
     * @param accountId the GL account ID
     * @return {@code true} if the account has at least one posted journal entry
     */
    @Query("""
            SELECT CASE WHEN COUNT(je) > 0 THEN true ELSE false END
            FROM GLJournalEntry je
            JOIN je.transaction t
            WHERE je.account.id = :accountId
            AND t.status = 'POSTED'
            """)
    boolean existsPostedEntryForAccount(@Param("accountId") UUID accountId);

    /**
     * Bulk variant of {@code findEntriesByAccountAndDateRange}: fetches posted journal
     * entries for <em>all</em> accounts in {@code accountIds} whose value date falls
     * in [{@code fromDate}, {@code toDate}]. Used by
     * {@code BalanceService.getTrialBalance} to avoid one query per account.
     */
    @Query("""
            SELECT je FROM GLJournalEntry je
            JOIN je.transaction t
            WHERE je.account.id IN :accountIds
              AND je.valueDate BETWEEN :fromDate AND :toDate
              AND t.status = 'POSTED'
            ORDER BY je.account.id ASC, je.valueDate ASC
            """)
    List<GLJournalEntry> findEntriesForAccountsInDateRange(@Param("accountIds") Collection<UUID> accountIds,
            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    /**
     * Bulk variant of {@code findEntriesByAccountUpToDate}: fetches all posted journal
     * entries for <em>all</em> accounts in {@code accountIds} up to and including
     * {@code toDate}. Used by {@code BalanceService.getTrialBalance} for accounts
     * that have no daily-balance snapshot yet.
     */
    @Query("""
            SELECT je FROM GLJournalEntry je
            JOIN je.transaction t
            WHERE je.account.id IN :accountIds
              AND je.valueDate <= :toDate
              AND t.status = 'POSTED'
            ORDER BY je.account.id ASC, je.valueDate ASC
            """)
    List<GLJournalEntry> findEntriesForAccountsUpToDate(@Param("accountIds") Collection<UUID> accountIds,
            @Param("toDate") LocalDate toDate);
}
