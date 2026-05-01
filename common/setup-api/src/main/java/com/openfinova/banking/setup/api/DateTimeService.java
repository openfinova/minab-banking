package com.openfinova.banking.setup.api;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Service for obtaining current date and time with business logic support.
 *
 * This service provides a testable abstraction over the system clock and includes
 * business-related date/time operations such as business day calculations
 * (including holidays) and timezone handling.
 *
 * Benefits:
 * - Testability: Can mock time in unit tests
 * - Consistency: Single source of truth for time across the application
 * - Business Logic: Centralized business day logic
 * - Timezone Management: Consistent timezone handling
 * - Audit Trail: Can log all time access for compliance
 *
 * Spring applications register a singleton {@link Clock} bean
 * ({@code Clock.systemDefaultZone()}) in
 * {@code com.openfinova.banking.setup.config.SetupServiceConfig} so components
 * that need {@link Clock} directly (e.g. token issuance, permission time windows)
 * stay aligned with this service.
 *
 * Usage:
 * {@code
 * @Autowired
 * private DateTimeService dateTimeService;
 *
 * LocalDateTime now = dateTimeService.now();
 * LocalDate today = dateTimeService.today();
 * boolean isBusinessDay = dateTimeService.isBusinessDay(today);
 * }
 */
public interface DateTimeService {

    /**
     * Gets the current date-time in the system default timezone.
     *
     * @return current date-time
     */
    LocalDateTime now();

    /**
     * Gets the current instant (UTC).
     *
     * @return current instant in UTC
     */
    Instant instant();

    /**
     * The {@link Clock} backing {@link #now()}, {@link #instant()}, and related
     * methods. Inject this where APIs require a {@code Clock} instead of this service.
     *
     * @return the clock (never null)
     */
    Clock clock();

    /**
     * Gets the current date in the system default timezone.
     *
     * @return current date
     */
    LocalDate today();

    /**
     * Gets the current date-time with timezone information.
     *
     * @return current zoned date-time
     */
    ZonedDateTime nowZoned();

    /**
     * Gets the current date-time in a specific timezone.
     *
     * @param zoneId the timezone
     * @return current date-time in the specified timezone
     */
    LocalDateTime nowInZone(ZoneId zoneId);

    /**
     * Gets the current date in a specific timezone.
     *
     * @param zoneId the timezone
     * @return current date in the specified timezone
     */
    LocalDate todayInZone(ZoneId zoneId);

    /**
     * Checks if a given date is a business day.
     * A business day is a weekday (Monday-Friday) that is not a holiday.
     *
     * @param date the date to check
     * @return true if the date is a business day
     */
    boolean isBusinessDay(LocalDate date);

    /**
     * Checks if a given date is a weekend (Saturday or Sunday).
     *
     * @param date the date to check
     * @return true if the date is a weekend
     */
    boolean isWeekend(LocalDate date);

    /**
     * Gets the next business day from a given date.
     * If the given date is a business day, returns the next business day.
     *
     * @param date the starting date
     * @return the next business day
     */
    LocalDate nextBusinessDay(LocalDate date);

    /**
     * Gets the previous business day from a given date.
     * If the given date is a business day, returns the previous business day.
     *
     * @param date the starting date
     * @return the previous business day
     */
    LocalDate previousBusinessDay(LocalDate date);

    /**
     * Adds a specified number of business days to a date.
     * Skips weekends and holidays.
     *
     * @param date the starting date
     * @param businessDays the number of business days to add (can be negative)
     * @return the resulting date
     */
    LocalDate addBusinessDays(LocalDate date, int businessDays);

    /**
     * Calculates the number of business days between two dates (inclusive).
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return the number of business days
     */
    long countBusinessDays(LocalDate startDate, LocalDate endDate);

    /**
     * Checks if a given date-time is within business hours.
     * Business hours are typically 9:00 AM to 5:00 PM on business days.
     *
     * @param dateTime the date-time to check
     * @return true if within business hours
     */
    boolean isWithinBusinessHours(LocalDateTime dateTime);

    /**
     * Gets the start of business hours for a given date.
     *
     * @param date the date
     * @return the start of business hours (e.g., 9:00 AM)
     */
    LocalDateTime getBusinessDayStart(LocalDate date);

    /**
     * Gets the end of business hours for a given date.
     *
     * @param date the date
     * @return the end of business hours (e.g., 5:00 PM)
     */
    LocalDateTime getBusinessDayEnd(LocalDate date);

    /**
     * Gets the current fiscal year.
     *
     * @return the current fiscal year
     */
    int getCurrentFiscalYear();

    /**
     * Gets the current fiscal quarter (1-4).
     *
     * @return the current fiscal quarter
     */
    int getCurrentFiscalQuarter();

    /**
     * Gets the current fiscal month (1-12).
     *
     * @return the current fiscal month
     */
    int getCurrentFiscalMonth();

    /**
     * Gets the start date of the current fiscal year.
     *
     * @return the fiscal year start date
     */
    LocalDate getFiscalYearStart();

    /**
     * Gets the end date of the current fiscal year.
     *
     * @return the fiscal year end date
     */
    LocalDate getFiscalYearEnd();

    /**
     * Gets the start of day (00:00:00) for a given date.
     *
     * @param date the date
     * @return the start of day
     */
    LocalDateTime startOfDay(LocalDate date);

    /**
     * Gets the end of day (23:59:59.999999999) for a given date.
     *
     * @param date the date
     * @return the end of day
     */
    LocalDateTime endOfDay(LocalDate date);

    /**
     * Gets the start of month for a given date.
     *
     * @param date the date
     * @return the first day of the month
     */
    LocalDate startOfMonth(LocalDate date);

    /**
     * Gets the end of month for a given date.
     *
     * @param date the date
     * @return the last day of the month
     */
    LocalDate endOfMonth(LocalDate date);

    /**
     * Calculates the number of days between two dates.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return the number of days
     */
    long daysBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Checks if a date is in the past.
     *
     * @param date the date to check
     * @return true if the date is before today
     */
    boolean isPast(LocalDate date);

    /**
     * Checks if a date is in the future.
     *
     * @param date the date to check
     * @return true if the date is after today
     */
    boolean isFuture(LocalDate date);

    /**
     * Checks if a date is today.
     *
     * @param date the date to check
     * @return true if the date is today
     */
    boolean isToday(LocalDate date);
}
