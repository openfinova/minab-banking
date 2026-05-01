package com.openfinova.banking.customer.account.api.entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Enumeration of limit periods that define the time window for limit
 * enforcement.
 * Provides methods to calculate period boundaries for limit tracking.
 */
public enum LimitPeriod {
    DAILY("Daily limit period"),
    WEEKLY("Weekly limit period"),
    MONTHLY("Monthly limit period"),
    QUARTERLY("Quarterly limit period"),
    ANNUAL("Annual limit period"),
    LIFETIME("Lifetime limit period");

    private final String description;

    LimitPeriod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets the start of the current period for the given timestamp.
     *
     * @param timestamp the reference timestamp
     * @return the start of the period containing the timestamp
     */
    public LocalDateTime getPeriodStart(LocalDateTime timestamp) {
        return switch (this) {
            case DAILY -> timestamp.truncatedTo(ChronoUnit.DAYS);
            case WEEKLY -> timestamp.truncatedTo(ChronoUnit.DAYS).minusDays(timestamp.getDayOfWeek().getValue() - 1);
            case MONTHLY -> timestamp.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            case QUARTERLY -> {
                int quarter = (timestamp.getMonthValue() - 1) / 3;
                int quarterStartMonth = quarter * 3 + 1;
                yield timestamp.withMonth(quarterStartMonth).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            }
            case ANNUAL -> timestamp.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
            case LIFETIME -> LocalDateTime.MIN;
        };
    }

    /**
     * Gets the end of the current period for the given timestamp.
     *
     * @param timestamp the reference timestamp
     * @return the end of the period containing the timestamp
     */
    public LocalDateTime getPeriodEnd(LocalDateTime timestamp) {
        return switch (this) {
            case DAILY -> timestamp.truncatedTo(ChronoUnit.DAYS).plusDays(1).minusNanos(1);
            case WEEKLY -> getPeriodStart(timestamp).plusWeeks(1).minusNanos(1);
            case MONTHLY -> getPeriodStart(timestamp).plusMonths(1).minusNanos(1);
            case QUARTERLY -> getPeriodStart(timestamp).plusMonths(3).minusNanos(1);
            case ANNUAL -> getPeriodStart(timestamp).plusYears(1).minusNanos(1);
            case LIFETIME -> LocalDateTime.MAX;
        };
    }

    /**
     * Determines if this is a rolling period (vs. calendar period).
     *
     * @return true if this is a rolling period
     */
    public boolean isRollingPeriod() {
        return this == LIFETIME;
    }
}