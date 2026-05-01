package com.openfinova.banking.tp.api.entity;

/**
 * Enumeration of refund types supported by the transaction processing system.
 * This enum is used in DTOs for API request/response handling.
 */
public enum RefundType {
    /**
     * Full refund - returns the entire remaining refundable amount
     */
    FULL("Full Refund", "Returns the complete remaining refundable amount to the original source"),

    /**
     * Partial refund - returns a specified portion of the refundable amount
     */
    PARTIAL("Partial Refund", "Returns a specified amount (less than the total) to the original source");

    private final String displayName;
    private final String description;

    RefundType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this refund type requires an explicit amount.
     *
     * @return true if amount must be specified
     */
    public boolean requiresExplicitAmount() {
        return this == PARTIAL;
    }

    /**
     * Checks if this refund type automatically calculates the amount.
     *
     * @return true if amount is calculated automatically
     */
    public boolean isAutoCalculated() {
        return this == FULL;
    }
}
