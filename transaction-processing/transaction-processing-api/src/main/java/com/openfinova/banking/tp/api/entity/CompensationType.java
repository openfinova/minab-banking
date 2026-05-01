package com.openfinova.banking.tp.api.entity;

/**
 * Enumeration of compensation types for transaction reversals.
 */
public enum CompensationType {
    /**
     * Reversal of the full transaction amount and all associated fees.
     */
    FULL,

    /**
     * Partial reversal of the transaction amount.
     */
    PARTIAL,

    /**
     * Release of reserved funds without transaction reversal.
     */
    RESERVATION_RELEASE
}
