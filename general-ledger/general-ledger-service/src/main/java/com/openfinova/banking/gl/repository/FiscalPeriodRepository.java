package com.openfinova.banking.gl.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.gl.api.entity.FiscalPeriodStatus;
import com.openfinova.banking.gl.entity.FiscalPeriod;

public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, UUID> {

    /**
     * All periods chronologically ({@link FiscalPeriod#getStartDate}, then fiscal year / period).
     */
    List<FiscalPeriod> findAllByOrderByStartDateAscFiscalYearAscPeriodNumberAsc();

    /**
     * Find all fiscal periods belonging to a specific fiscal year, ordered
     * by period number. This is the primary query for regulatory reporting
     * and year-end processing — no date scanning required.
     *
     * @param fiscalYear the fiscal year (e.g. 2024)
     * @return periods for that year sorted by periodNumber ascending
     */
    List<FiscalPeriod> findByFiscalYearOrderByPeriodNumberAsc(int fiscalYear);

    /**
     * Look up a specific period by its natural key (year + number).
     * Useful for bulk imports and period-specific regulatory extracts.
     *
     * @param fiscalYear   the fiscal year
     * @param periodNumber the period number within the year
     * @return the matching period if it exists
     */
    Optional<FiscalPeriod> findByFiscalYearAndPeriodNumber(int fiscalYear, int periodNumber);

    /**
     * Find the active fiscal period for a specific date.
     *
     * @param date the date to check
     * @return optional containing the active period if found
     */
    @Query("""
            SELECT fp FROM FiscalPeriod fp
            WHERE fp.startDate <= :date
            AND fp.endDate >= :date
            AND fp.status = 'OPEN'
            """)
    Optional<FiscalPeriod> findActivePeriodForDate(@Param("date") LocalDate date);

    /**
     * Check if there are overlapping periods with the given date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return true if there are overlapping periods
     */
    @Query("SELECT COUNT(fp) > 0 FROM FiscalPeriod fp WHERE (fp.startDate <= :endDate AND fp.endDate >= :startDate)")
    boolean existsOverlappingPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find fiscal period by name.
     *
     * @param name the period name
     * @return optional containing the period if found
     */
    Optional<FiscalPeriod> findByName(String name);

    /**
     * Returns the highest period number used within a fiscal year.
     * Used to identify the year-end period so that P&L closing entries
     * are only generated once — at year-end — rather than on every
     * monthly or quarterly close.
     *
     * @param fiscalYear the fiscal year (e.g. 2025)
     * @return the maximum periodNumber for the year, or empty if no periods exist
     */
    @Query("SELECT MAX(fp.periodNumber) FROM FiscalPeriod fp WHERE fp.fiscalYear = :fiscalYear")
    Optional<Integer> findMaxPeriodNumberByFiscalYear(@Param("fiscalYear") int fiscalYear);

    /**
     * Find all fiscal periods with a specific status.
     *
     * @param status the fiscal period status
     * @return list of periods with the given status
     */
    List<FiscalPeriod> findByStatus(FiscalPeriodStatus status);
}
