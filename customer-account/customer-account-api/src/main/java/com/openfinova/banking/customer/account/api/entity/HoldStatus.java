package com.openfinova.banking.customer.account.api.entity;

/**
 * Enumeration of possible statuses for account holds.
 */
public enum HoldStatus {
    /**
     * Hold is active and funds are blocked.
     */
    ACTIVE("Active", "Funds are currently blocked"),

    /**
     * Hold was manually released, funds are available.
     */
    RELEASED("Released", "Hold was manually released"),

    /**
     * Hold expired automatically, funds are available.
     */
    EXPIRED("Expired", "Hold expired due to timeout"),

    /**
     * Hold was consumed by a posted transaction.
     */
    SETTLED("Settled", "Hold was consumed by transaction");

    private final String displayName;
    private final String description;

    HoldStatus(String displayName, String description) {
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
     * Checks if this status represents an active hold.
     *
     * @return true if status is ACTIVE
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Checks if this status represents a terminal state (cannot be changed).
     *
     * @return true if status is RELEASED, EXPIRED, or SETTLED
     */
    public boolean isTerminal() {
        return this == RELEASED || this == EXPIRED || this == SETTLED;
    }
}
