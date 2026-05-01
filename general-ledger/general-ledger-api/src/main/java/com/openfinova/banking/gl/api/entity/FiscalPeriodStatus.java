package com.openfinova.banking.gl.api.entity;

/**
 * Enumeration of fiscal period status values.
 */
public enum FiscalPeriodStatus {
    /**
     * Period is open and accepts all transactions.
     */
    OPEN,

    /**
     * Period is undergoing closing adjustments.
     * Only specific adjustment transaction types allowed.
     */
    ADJUSTING,

    /**
     * Period is closed. No new transactions allowed.
     */
    CLOSED,

    /**
     * Period is strictly closed and archived.
     */
    LOCKED
}
