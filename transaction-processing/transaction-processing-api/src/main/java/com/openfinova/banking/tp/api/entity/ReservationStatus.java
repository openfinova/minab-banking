package com.openfinova.banking.tp.api.entity;

/**
 * Enumeration of balance reservation statuses.
 */
public enum ReservationStatus {
    /**
     * Reservation is active and holding funds
     */
    ACTIVE("Active"),

    /**
     * Reservation has been manually or automatically released
     */
    RELEASED("Released"),

    /**
     * Reservation has been converted to actual GL posting
     */
    CONVERTED("Converted to Posting"),

    /**
     * Reservation has expired due to timeout
     */
    EXPIRED("Expired");

    private final String displayName;

    ReservationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this reservation is still holding funds
     *
     * @return true if reservation is actively holding funds
     */
    public boolean isHoldingFunds() {
        return this == ACTIVE;
    }

    /**
     * Checks if this reservation is in a terminal state
     *
     * @return true if no further state changes are possible
     */
    public boolean isTerminal() {
        return this == RELEASED || this == CONVERTED || this == EXPIRED;
    }
}