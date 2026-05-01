package com.openfinova.banking.tp.api.entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Enumeration of velocity limit periods for transaction monitoring.
 * Defines different time windows for tracking transaction limits.
 *
 * Requirements addressed:
 * - Define limit periods: DAILY, WEEKLY, MONTHLY
 * - Customer tier-based limit variations
 */
public enum VelocityLimitPeriod {
    /**
     * Daily limit period - resets every 24 hours
     */
    DAILY("Daily", ChronoUnit.DAYS, 1),

    /**
     * Weekly limit period - resets every 7 days
     */
    WEEKLY("Weekly", ChronoUnit.WEEKS, 1),

    /**
     * Monthly limit period - resets every 30 days
     */
    MONTHLY("Monthly", ChronoUnit.MONTHS, 1);

    private final String displayName;
    private final ChronoUnit chronoUnit;
    private final long duration;

    VelocityLimitPeriod(String displayName, ChronoUnit chronoUnit, long duration) {
        this.displayName = displayName;
        this.chronoUnit = chronoUnit;
        this.duration = duration;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChronoUnit getChronoUnit() {
        return chronoUnit;
    }

    public long getDuration() {
        return duration;
    }

    /**
     * Calculates the start of the current period for the given timestamp.
     *
     * @param timestamp the reference timestamp
     * @return the start of the current period
     */
    public LocalDateTime getPeriodStart(LocalDateTime timestamp) {
        switch (this) {
            case DAILY:
                return timestamp.truncatedTo(ChronoUnit.DAYS);
            case WEEKLY:
                return timestamp.truncatedTo(ChronoUnit.DAYS).minusDays(timestamp.getDayOfWeek().getValue() - 1);
            case MONTHLY:
                return timestamp.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
            default:
                throw new IllegalStateException("Unsupported limit period: " + this);
        }
    }

    /**
     * Calculates the end of the current period for the given timestamp.
     *
     * @param timestamp the reference timestamp
     * @return the end of the current period
     */
    public LocalDateTime getPeriodEnd(LocalDateTime timestamp) {
        LocalDateTime periodStart = getPeriodStart(timestamp);
        return periodStart.plus(duration, chronoUnit);
    }

    /**
     * Checks if the given timestamp is within the same period as the reference
     * timestamp.
     *
     * @param timestamp the timestamp to check
     * @param reference the reference timestamp
     * @return true if both timestamps are in the same period
     */
    public boolean isInSamePeriod(LocalDateTime timestamp, LocalDateTime reference) {
        LocalDateTime periodStart = getPeriodStart(reference);
        LocalDateTime periodEnd = getPeriodEnd(reference);
        return !timestamp.isBefore(periodStart) && timestamp.isBefore(periodEnd);
    }
}
