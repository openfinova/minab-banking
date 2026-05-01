package com.openfinova.banking.setup.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.openfinova.banking.setup.entity.Holiday;

/**
 * Repository for Holiday entity.
 */
@Repository
public interface HolidayRepository extends JpaRepository<Holiday, UUID> {

    /**
     * Finds a holiday by date, country, and region.
     */
    Optional<Holiday> findByDateAndCountryCodeAndRegionCode(LocalDate date, String countryCode, String regionCode);

    /**
     * Finds all holidays for a specific year and country.
     */
    List<Holiday> findByYearAndCountryCodeOrderByDateAsc(int year, String countryCode);

    /**
     * Finds all holidays for a specific year, country, and region.
     */
    List<Holiday> findByYearAndCountryCodeAndRegionCodeOrderByDateAsc(int year, String countryCode, String regionCode);

    /**
     * Finds all holidays for a specific date range.
     */
    List<Holiday> findByDateBetweenOrderByDateAsc(LocalDate startDate, LocalDate endDate);

    /**
     * Finds all bank holidays for a specific year and country.
     */
    List<Holiday> findByYearAndCountryCodeAndBankHolidayTrueOrderByDateAsc(int year, String countryCode);

    /**
     * Checks if a date is a holiday for a specific country and region.
     */
    boolean existsByDateAndCountryCodeAndRegionCode(LocalDate date, String countryCode, String regionCode);

    /**
     * Checks if a date is a holiday for a specific country, matching either a
     * national holiday (regionCode IS NULL) or a region-specific holiday.
     * Used by isHoliday() to ensure that regional queries also catch national holidays.
     */
    @Query("""
            SELECT COUNT(h) > 0 FROM Holiday h
               WHERE h.date = :date
               AND h.countryCode = :countryCode
               AND (h.regionCode = :regionCode OR h.regionCode IS NULL)
               """)
    boolean existsByDateAndCountryCodeAndRegionOrNational(@Param("date") LocalDate date,
            @Param("countryCode") String countryCode, @Param("regionCode") String regionCode);

    /**
     * Gets all distinct country codes.
     */
    @Query("SELECT DISTINCT h.countryCode FROM Holiday h ORDER BY h.countryCode")
    List<String> findDistinctCountryCodes();

    /**
     * Gets all distinct region codes for a country.
     */
    @Query("""
            SELECT DISTINCT h.regionCode
            FROM Holiday h WHERE
                    h.countryCode = :countryCode
                    AND h.regionCode IS NOT NULL
            ORDER BY h.regionCode
            """)
    List<String> findDistinctRegionCodesByCountryCode(@Param("countryCode") String countryCode);

    /**
     * Deletes all holidays for a specific year, country, and region.
     */
    void deleteByYearAndCountryCodeAndRegionCode(int year, String countryCode, String regionCode);

    /**
     * Deletes all holidays for a specific year and country.
     */
    void deleteByYearAndCountryCode(int year, String countryCode);
}
