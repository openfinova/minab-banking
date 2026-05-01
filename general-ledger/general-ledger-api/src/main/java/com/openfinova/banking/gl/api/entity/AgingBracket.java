package com.openfinova.banking.gl.api.entity;

/**
 * Standard aging brackets for suspense items reporting.
 *
 * Industry standard aging categories used for:
 * - Management reporting
 * - Regulatory submissions
 * - Trend analysis
 * - Performance metrics
 */
public enum AgingBracket {

    /**
     * 0-7 days: Current items, normal processing window.
     */
    CURRENT_0_7_DAYS("0-7 days", 0, 7),

    /**
     * 8-30 days: Recent, requires attention.
     */
    RECENT_8_30_DAYS("8-30 days", 8, 30),

    /**
     * 31-60 days: Aging, supervisor review required.
     */
    AGING_31_60_DAYS("31-60 days", 31, 60),

    /**
     * 61-90 days: Overdue, manager approval needed.
     */
    OVERDUE_61_90_DAYS("61-90 days", 61, 90),

    /**
     * 91-180 days: Critical, senior management escalation.
     */
    CRITICAL_91_180_DAYS("91-180 days", 91, 180),

    /**
     * 180+ days: Severely aged, potential write-off.
     * Requires executive approval to retain.
     */
    SEVERELY_AGED_180_PLUS("180+ days", 181, Integer.MAX_VALUE);

    private final String description;
    private final int minDays;
    private final int maxDays;

    AgingBracket(String description, int minDays, int maxDays) {
        this.description = description;
        this.minDays = minDays;
        this.maxDays = maxDays;
    }

    public String getDescription() {
        return description;
    }

    public int getMinDays() {
        return minDays;
    }

    public int getMaxDays() {
        return maxDays;
    }

    /**
     * Determine which aging bracket an item falls into based on age.
     */
    public static AgingBracket fromAgeDays(long ageDays) {
        for (AgingBracket bracket : values()) {
            if (ageDays >= bracket.minDays && ageDays <= bracket.maxDays) {
                return bracket;
            }
        }
        return SEVERELY_AGED_180_PLUS; // Default for very old items
    }

    /**
     * Check if this bracket requires escalation.
     */
    public boolean requiresEscalation() {
        return this == AGING_31_60_DAYS || this == OVERDUE_61_90_DAYS || this == CRITICAL_91_180_DAYS
                || this == SEVERELY_AGED_180_PLUS;
    }
}
