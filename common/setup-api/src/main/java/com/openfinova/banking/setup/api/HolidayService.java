package com.openfinova.banking.setup.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.openfinova.banking.setup.api.dto.HolidayDTO;

/**
 * Service for managing holidays by country and region.
 *
 * This service provides centralized holiday management with support for:
 * - Multiple countries and regions
 * - Holiday descriptions and metadata
 * - Year-based holiday retrieval
 * - Holiday validation
 *
 * Holidays are identified by country code (ISO 3166-1 alpha-2) and optional region code.
 *
 * Examples:
 * - US national holidays: country="US", region=null
 * - US state holidays: country="US", region="NY" (New York)
 * - UK holidays: country="GB", region=null
 *
 * Usage:
 * {@code
 * @Autowired
 * private HolidayService holidayService;
 *
 * // Check if date is a holiday
 * boolean isHoliday = holidayService.isHoliday(date, "US", null);
 *
 * // Get all holidays for a year
 * List<HolidayDTO> holidays = holidayService.getHolidays(2026, "US", null);
 * }
 */
public interface HolidayService {

    /**
     * Checks if a given date is a holiday for a specific country and region.
     *
     * @param date the date to check
     * @param countryCode the ISO 3166-1 alpha-2 country code (e.g., "US", "GB", "CA")
     * @param regionCode the region code (optional, can be null for national holidays)
     * @return true if the date is a holiday
     */
    boolean isHoliday(LocalDate date, String countryCode, String regionCode);

    /**
     * Gets all holidays for a specific year, country, and region.
     *
     * @param year the year
     * @param countryCode the ISO 3166-1 alpha-2 country code
     * @param regionCode the region code (optional, can be null for national holidays)
     * @return list of holidays
     */
    List<HolidayDTO> getHolidays(int year, String countryCode, String regionCode);

    /**
     * Gets all holidays for a specific year and country (all regions).
     *
     * @param year the year
     * @param countryCode the ISO 3166-1 alpha-2 country code
     * @return list of holidays
     */
    List<HolidayDTO> getHolidaysByCountry(int year, String countryCode);

    /**
     * Gets holiday information for a specific date.
     *
     * @param date the date
     * @param countryCode the ISO 3166-1 alpha-2 country code
     * @param regionCode the region code (optional)
     * @return optional holiday information
     */
    Optional<HolidayDTO> getHoliday(LocalDate date, String countryCode, String regionCode);

    /**
     * Adds a holiday to the calendar.
     *
     * @param holiday the holiday to add
     */
    void addHoliday(HolidayDTO holiday);

    /**
     * Removes a holiday from the calendar.
     *
     * @param date the holiday date
     * @param countryCode the ISO 3166-1 alpha-2 country code
     * @param regionCode the region code (optional)
     * @return true if the holiday was removed
     */
    boolean removeHoliday(LocalDate date, String countryCode, String regionCode);

    /**
     * Gets all supported country codes.
     *
     * @return set of country codes
     */
    Set<String> getSupportedCountries();

    /**
     * Gets all supported region codes for a country.
     *
     * @param countryCode the ISO 3166-1 alpha-2 country code
     * @return set of region codes
     */
    Set<String> getSupportedRegions(String countryCode);

    /**
     * Initializes standard holidays for a country and year.
     * This loads commonly recognized holidays for the specified country.
     *
     * @param year the year
     * @param countryCode the ISO 3166-1 alpha-2 country code
     */
    void initializeStandardHolidays(int year, String countryCode);

    /**
     * Clears all holidays for a specific year, country, and region.
     *
     * @param year the year
     * @param countryCode the ISO 3166-1 alpha-2 country code
     * @param regionCode the region code (optional)
     */
    void clearHolidays(int year, String countryCode, String regionCode);

    /**
     * Clears all holidays.
     */
    void clearAllHolidays();
}
