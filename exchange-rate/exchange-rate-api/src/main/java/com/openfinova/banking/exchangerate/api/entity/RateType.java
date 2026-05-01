package com.openfinova.banking.exchangerate.api.entity;

/**
 * Enumeration of exchange rate types.
 */
public enum RateType {
    /**
     * Spot rate for immediate settlement (T+0 to T+2).
     */
    SPOT,

    /**
     * End of Day rate used for revaluation/reporting.
     */
    EOD,

    /**
     * Monthly average rate.
     */
    AVG_MONTH,

    /**
     * Corporate/Official rate fixed for a specific period.
     */
    OFFICIAL
}
