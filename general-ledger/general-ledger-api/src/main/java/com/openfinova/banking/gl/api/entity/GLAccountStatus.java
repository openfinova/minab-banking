package com.openfinova.banking.gl.api.entity;

/**
 * Enumeration of account status values for soft delete functionality.
 */
public enum GLAccountStatus {
    /**
     * Account is active and can receive new transactions
     */
    ACTIVE,

    /**
     * Account is inactive - no new transactions allowed but historical data
     * preserved
     */
    INACTIVE
}