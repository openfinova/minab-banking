package com.openfinova.banking.tp.api.entity;

/**
 * Enumeration of balance reservation types.
 */
public enum ReservationType {
    /**
     * Hold on debit amount for outgoing transactions
     */
    DEBIT_HOLD("Debit Hold"),

    /**
     * Hold on credit amount for incoming transactions (rare, used for compliance)
     */
    CREDIT_HOLD("Credit Hold"),

    /**
     * Hold for transaction fees
     */
    FEE_HOLD("Fee Hold");

    private final String displayName;

    ReservationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this reservation type affects available balance
     *
     * @return true if this reservation reduces available balance
     */
    public boolean reducesAvailableBalance() {
        return this == DEBIT_HOLD || this == FEE_HOLD;
    }
}