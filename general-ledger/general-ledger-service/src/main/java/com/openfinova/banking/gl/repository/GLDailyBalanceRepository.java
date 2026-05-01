package com.openfinova.banking.gl.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.gl.entity.GLDailyBalance;

public interface GLDailyBalanceRepository extends JpaRepository<GLDailyBalance, UUID> {

    /**
     * Find the latest daily balance for an account.
     *
     * @param accountId the GL account ID
     * @return optional containing the latest daily balance
     */
    @Query("SELECT db FROM GLDailyBalance db WHERE db.glAccount.id = :accountId ORDER BY db.balanceDate DESC")
    Optional<GLDailyBalance> findLatestDailyBalanceByAccount(@Param("accountId") UUID accountId);

    /**
     * Find daily balance for a specific account and date.
     *
     * @param accountId the GL account ID
     * @param date the balance date
     * @return optional containing the daily balance
     */
    @Query("SELECT db FROM GLDailyBalance db WHERE db.glAccount.id = :accountId AND db.balanceDate = :date")
    Optional<GLDailyBalance> findDailyBalanceByAccountAndDate(@Param("accountId") UUID accountId,
            @Param("date") LocalDate date);

    /**
     * Find the latest daily balance for an account before a specific date.
     *
     * @param accountId the GL account ID
     * @param date the cutoff date
     * @return optional containing the latest daily balance before the date
     */
    @Query("SELECT db FROM GLDailyBalance db WHERE db.glAccount.id = :accountId AND db.balanceDate < :date ORDER BY db.balanceDate DESC")
    Optional<GLDailyBalance> findLatestDailyBalanceByAccountBeforeDate(@Param("accountId") UUID accountId,
            @Param("date") LocalDate date);

    /**
     * Find daily balances for an account within a date range.
     *
     * @param accountId the GL account ID
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of daily balances in chronological order
     */
    @Query("""
            SELECT db FROM GLDailyBalance db
            WHERE db.glAccount.id = :accountId
            AND db.balanceDate BETWEEN :startDate AND :endDate
            ORDER BY db.balanceDate ASC
            """)
    List<GLDailyBalance> findDailyBalancesByAccountAndDateRange(@Param("accountId") UUID accountId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find accounts missing daily balance snapshots for a specific date.
     *
     * @param date the date to check
     * @return list of account IDs missing snapshots
     */
    @Query("SELECT a.id FROM GLAccount a WHERE a.id NOT IN (SELECT db.glAccount.id FROM GLDailyBalance db WHERE db.balanceDate = :date)")
    List<UUID> findAccountsMissingSnapshotsForDate(@Param("date") LocalDate date);

    /**
     * Find the latest date for which daily balance snapshots exist.
     *
     * @return optional containing the latest snapshot date
     */
    @Query("SELECT MAX(db.balanceDate) FROM GLDailyBalance db")
    Optional<LocalDate> findLatestSnapshotDate();

    /**
     * Returns the most recent snapshot on or before {@code asOfDate} for every account
     * that has at least one such snapshot. Exactly one row per account.
     * Used by {@code BalanceService.getTrialBalance} to replace per-account snapshot
     * lookups with a single round-trip.
     *
     * @param asOfDate the upper bound (inclusive)
     * @return one {@link GLDailyBalance} per account, the one with the highest
     *         {@code balanceDate} that is still ≤ {@code asOfDate}
     */
    @Query("""
            SELECT db FROM GLDailyBalance db
            WHERE db.balanceDate <= :asOfDate
              AND db.balanceDate = (
                  SELECT MAX(db2.balanceDate) FROM GLDailyBalance db2
                  WHERE db2.glAccount.id = db.glAccount.id
                    AND db2.balanceDate <= :asOfDate
              )
            """)
    List<GLDailyBalance> findLatestSnapshotsForAllAccountsAsOf(@Param("asOfDate") LocalDate asOfDate);

    /**
     * Find daily balances older than the specified date.
     *
     * @param cutoffDate the cutoff date (exclusive)
     * @return list of daily balances before the cutoff date
     */
    @Query("SELECT db FROM GLDailyBalance db WHERE db.balanceDate < :cutoffDate")
    List<GLDailyBalance> findByBalanceDateBefore(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Delete daily balances older than the specified date.
     *
     * @param cutoffDate the cutoff date (exclusive)
     * @return number of deleted records
     */
    @Query("DELETE FROM GLDailyBalance db WHERE db.balanceDate < :cutoffDate")
    @Modifying
    int deleteByBalanceDateBefore(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Delete daily balances for a specific date range.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return number of deleted records
     */
    @Query("DELETE FROM GLDailyBalance db WHERE db.balanceDate BETWEEN :startDate AND :endDate")
    @Modifying
    int deleteByBalanceDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Count daily balances for a specific date range.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return count of daily balances in the date range
     */
    @Query("SELECT COUNT(db) FROM GLDailyBalance db WHERE db.balanceDate BETWEEN :startDate AND :endDate")
    long countByBalanceDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
