package com.openfinova.banking.setup;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.openfinova.banking.setup.api.DateTimeService;
import com.openfinova.banking.setup.api.HolidayService;

/**
 * System implementation of DateTimeService using the system clock.
 *
 * This implementation provides:
 * - Current date/time from system clock
 * - Business day calculations (Monday-Friday, excluding holidays)
 * - Business hours validation (9 AM - 5 PM)
 * - Fiscal period calculations (calendar year based)
 * - Holiday management via HolidayService
 *
 * Configuration:
 * - Business hours: configurable via banking.datetime.business-hours.start / .end (default 9–17)
 * - Fiscal year: Follows calendar year (January - December)
 * - Holidays: Managed by HolidayService with country/region support
 * - Default country: Configurable via application.properties
 */
@Service
public class SystemDateTimeService implements DateTimeService {

    private static final Logger logger = LoggerFactory.getLogger(SystemDateTimeService.class);

    // Business hours configuration — injected from properties, not compile-time constants
    @Value("${banking.datetime.business-hours.start:9}")
    private int businessDayStartHour;

    @Value("${banking.datetime.business-hours.end:17}")
    private int businessDayEndHour;

    @Value("${banking.datetime.default-country:US}")
    private String defaultCountryCode;

    @Value("${banking.datetime.default-region:#{null}}")
    private String defaultRegionCode;

    private final HolidayService holidayService;
    private final Clock clock;

    public SystemDateTimeService(HolidayService holidayService, Clock clock) {
        this.holidayService = holidayService;
        this.clock = clock;
    }

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    @Override
    public Instant instant() {
        return clock.instant();
    }

    @Override
    public Clock clock() {
        return clock;
    }

    @Override
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    @Override
    public ZonedDateTime nowZoned() {
        return ZonedDateTime.now(clock);
    }

    @Override
    public LocalDateTime nowInZone(ZoneId zoneId) {
        return LocalDateTime.ofInstant(clock.instant(), zoneId);
    }

    @Override
    public LocalDate todayInZone(ZoneId zoneId) {
        return LocalDate.ofInstant(clock.instant(), zoneId);
    }

    @Override
    public boolean isBusinessDay(LocalDate date) {
        return !isWeekend(date) && !holidayService.isHoliday(date, defaultCountryCode, defaultRegionCode);
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

    @Override
    public boolean isWithinBusinessHours(LocalDateTime dateTime) {
        if (!isBusinessDay(dateTime.toLocalDate())) {
            return false;
        }

        int hour = dateTime.getHour();
        return hour >= businessDayStartHour && hour < businessDayEndHour;
    }

    @Override
    public LocalDateTime getBusinessDayStart(LocalDate date) {
        return date.atTime(businessDayStartHour, 0);
    }

    @Override
    public LocalDateTime getBusinessDayEnd(LocalDate date) {
        return date.atTime(businessDayEndHour, 0);
    }

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

    /**
     * Gets the holiday service for all holiday management operations.
     * Use HolidayService directly for advanced holiday management instead of
     * the convenience methods in this class.
     *
     * @return the holiday service
     */
    public HolidayService getHolidayService() {
        return holidayService;
    }

    /**
     * Gets the default country code used for holiday lookups.
     *
     * @return the country code
     */
    public String getDefaultCountryCode() {
        return defaultCountryCode;
    }

    /**
     * Sets the default country code for holiday lookups.
     *
     * @param countryCode the country code
     */
    public void setDefaultCountryCode(String countryCode) {
        this.defaultCountryCode = countryCode;
        logger.info("Default country code set to: {}", countryCode);
    }

    /**
     * Gets the default region code used for holiday lookups.
     *
     * @return the region code
     */
    public String getDefaultRegionCode() {
        return defaultRegionCode;
    }

    /**
     * Sets the default region code for holiday lookups.
     *
     * @param regionCode the region code
     */
    public void setDefaultRegionCode(String regionCode) {
        this.defaultRegionCode = regionCode;
        logger.info("Default region code set to: {}", regionCode);
    }
}
