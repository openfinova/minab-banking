package com.openfinova.banking.setup.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Profile;

import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Test-friendly implementation of DateTimeService with controllable time.
 *
 * This implementation allows tests to:
 * - Set a fixed time that won't change
 * - Advance time programmatically
 * - Freeze time at a specific moment
 * - Reset to system time
 *
 * Usage in tests:
 * {@code
 * @SpringBootTest
 * @ActiveProfiles("test")
 * public class MyTest {
 *     @Autowired
 *     private FixedDateTimeService dateTimeService;
 *
 *     @Test
 *     public void testWithFixedTime() {
 *         // Set fixed time
 *         dateTimeService.setFixedTime(LocalDateTime.of(2026, 1, 25, 10, 30));
 *
 *         // Test with predictable time
 *         // ...
 *     }
 * }
 * }
 *
 * Note: This class is annotated with @TestComponent and @Profile("test")
 * to ensure it's only available in test context.
 */
@TestComponent
@Profile("test")
public class FixedDateTimeService implements DateTimeService {

    private static final Logger logger = LoggerFactory.getLogger(FixedDateTimeService.class);

    // Business hours configuration
    private static final int BUSINESS_DAY_START_HOUR = 9;
    private static final int BUSINESS_DAY_END_HOUR = 17;

    // Time control
    private LocalDateTime fixedTime;
    private ZoneId zoneId = ZoneId.systemDefault();

    // Holiday storage
    private final Map<LocalDate, String> holidays = new ConcurrentHashMap<>();

    /**
     * Creates a FixedDateTimeService using system time initially.
     */
    public FixedDateTimeService() {
        this.fixedTime = null; // Use system time by default
    }

    /**
     * Creates a FixedDateTimeService with a fixed time.
     *
     * @param fixedTime the fixed time to use
     */
    public FixedDateTimeService(LocalDateTime fixedTime) {
        this.fixedTime = fixedTime;
    }

    // ==================== Time Control Methods ====================

    /**
     * Sets a fixed time that will be returned by now() and related methods.
     *
     * @param fixedTime the fixed time
     */
    public void setFixedTime(LocalDateTime fixedTime) {
        this.fixedTime = fixedTime;
        logger.debug("Fixed time set to: {}", fixedTime);
    }

    /**
     * Sets a fixed date (time will be set to start of day).
     *
     * @param fixedDate the fixed date
     */
    public void setFixedDate(LocalDate fixedDate) {
        this.fixedTime = fixedDate.atStartOfDay();
        logger.debug("Fixed date set to: {}", fixedDate);
    }

    /**
     * Advances the fixed time by a specified duration.
     *
     * @param duration the duration to advance
     */
    public void advanceBy(Duration duration) {
        if (fixedTime == null) {
            throw new IllegalStateException("Cannot advance time when using system time. Call setFixedTime() first.");
        }
        fixedTime = fixedTime.plus(duration);
        logger.debug("Time advanced by {} to: {}", duration, fixedTime);
    }

    /**
     * Advances the fixed time by a specified number of days.
     *
     * @param days the number of days to advance
     */
    public void advanceByDays(long days) {
        advanceBy(Duration.ofDays(days));
    }

    /**
     * Advances the fixed time by a specified number of hours.
     *
     * @param hours the number of hours to advance
     */
    public void advanceByHours(long hours) {
        advanceBy(Duration.ofHours(hours));
    }

    /**
     * Resets to use system time instead of fixed time.
     */
    public void useSystemTime() {
        this.fixedTime = null;
        logger.debug("Reset to use system time");
    }

    /**
     * Sets the timezone to use.
     *
     * @param zoneId the timezone
     */
    public void setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
        logger.debug("Timezone set to: {}", zoneId);
    }

    /**
     * Checks if using fixed time.
     *
     * @return true if using fixed time
     */
    public boolean isUsingFixedTime() {
        return fixedTime != null;
    }

    // ==================== Current Time Methods ====================

    @Override
    public LocalDateTime now() {
        return fixedTime != null ? fixedTime : LocalDateTime.now(zoneId);
    }

    @Override
    public Instant instant() {
        return now().atZone(zoneId).toInstant();
    }

    @Override
    public Clock clock() {
        if (fixedTime != null) {
            return Clock.fixed(fixedTime.atZone(zoneId).toInstant(), zoneId);
        }
        return Clock.system(zoneId);
    }

    @Override
    public LocalDate today() {
        return now().toLocalDate();
    }

    @Override
    public ZonedDateTime nowZoned() {
        return now().atZone(zoneId);
    }

    @Override
    public LocalDateTime nowInZone(ZoneId targetZoneId) {
        return fixedTime != null ? fixedTime.atZone(zoneId).withZoneSameInstant(targetZoneId).toLocalDateTime()
                : LocalDateTime.now(targetZoneId);
    }

    @Override
    public LocalDate todayInZone(ZoneId targetZoneId) {
        return nowInZone(targetZoneId).toLocalDate();
    }

    // ==================== Business Day Methods ====================

    @Override
    public boolean isBusinessDay(LocalDate date) {
        return !isWeekend(date) && !holidays.containsKey(date);
    }

    @Override
    public boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    @Override
    public LocalDate nextBusinessDay(LocalDate date) {
        LocalDate nextDay = date.plusDays(1);
        while (!isBusinessDay(nextDay)) {
            nextDay = nextDay.plusDays(1);
        }
        return nextDay;
    }

    @Override
    public LocalDate previousBusinessDay(LocalDate date) {
        LocalDate prevDay = date.minusDays(1);
        while (!isBusinessDay(prevDay)) {
            prevDay = prevDay.minusDays(1);
        }
        return prevDay;
    }

    @Override
    public LocalDate addBusinessDays(LocalDate date, int businessDays) {
        if (businessDays == 0) {
            return date;
        }

        LocalDate result = date;
        int daysToAdd = Math.abs(businessDays);
        int direction = businessDays > 0 ? 1 : -1;

        while (daysToAdd > 0) {
            result = result.plusDays(direction);
            if (isBusinessDay(result)) {
                daysToAdd--;
            }
        }

        return result;
    }

    @Override
    public long countBusinessDays(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            return 0;
        }

        long businessDays = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            if (isBusinessDay(current)) {
                businessDays++;
            }
            current = current.plusDays(1);
        }

        return businessDays;
    }

    // ==================== Business Hours Methods ====================

    @Override
    public boolean isWithinBusinessHours(LocalDateTime dateTime) {
        if (!isBusinessDay(dateTime.toLocalDate())) {
            return false;
        }

        int hour = dateTime.getHour();
        return hour >= BUSINESS_DAY_START_HOUR && hour < BUSINESS_DAY_END_HOUR;
    }

    @Override
    public LocalDateTime getBusinessDayStart(LocalDate date) {
        return date.atTime(BUSINESS_DAY_START_HOUR, 0);
    }

    @Override
    public LocalDateTime getBusinessDayEnd(LocalDate date) {
        return date.atTime(BUSINESS_DAY_END_HOUR, 0);
    }

    // ==================== Fiscal Period Methods ====================

    @Override
    public int getCurrentFiscalYear() {
        return today().getYear();
    }

    @Override
    public int getCurrentFiscalQuarter() {
        int month = today().getMonthValue();
        return (month - 1) / 3 + 1;
    }

    @Override
    public int getCurrentFiscalMonth() {
        return today().getMonthValue();
    }

    @Override
    public LocalDate getFiscalYearStart() {
        return LocalDate.of(getCurrentFiscalYear(), 1, 1);
    }

    @Override
    public LocalDate getFiscalYearEnd() {
        return LocalDate.of(getCurrentFiscalYear(), 12, 31);
    }

    // ==================== Utility Methods ====================

    @Override
    public LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    @Override
    public LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    @Override
    public LocalDate startOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    @Override
    public LocalDate endOfMonth(LocalDate date) {
        return date.withDayOfMonth(date.lengthOfMonth());
    }

    @Override
    public long daysBetween(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    @Override
    public boolean isPast(LocalDate date) {
        return date.isBefore(today());
    }

    @Override
    public boolean isFuture(LocalDate date) {
        return date.isAfter(today());
    }

    @Override
    public boolean isToday(LocalDate date) {
        return date.equals(today());
    }

    // ==================== Holiday Management ====================

    /**
     * Gets all holidays stored in memory for a given year.
     *
     * @param year the year to filter
     * @return set of holiday dates in the given year
     */
    public Set<LocalDate> getHolidays(int year) {
        Set<LocalDate> yearHolidays = new HashSet<>();
        for (LocalDate date : holidays.keySet()) {
            if (date.getYear() == year) {
                yearHolidays.add(date);
            }
        }
        return yearHolidays;
    }

    /**
     * Adds a holiday to the in-memory store.
     *
     * @param date the holiday date
     * @param description description for debugging/logging
     */
    public void addHoliday(LocalDate date, String description) {
        holidays.put(date, description);
        logger.debug("Added holiday: {} - {}", date, description);
    }

    /**
     * Removes a holiday from the in-memory store.
     *
     * @param date the holiday date
     */
    public void removeHoliday(LocalDate date) {
        holidays.remove(date);
        logger.debug("Removed holiday: {}", date);
    }

    /**
     * Clears all holidays from the in-memory store.
     */
    public void clearHolidays() {
        holidays.clear();
        logger.debug("Cleared all holidays");
    }
}
