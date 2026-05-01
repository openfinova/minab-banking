package com.openfinova.banking.customer.api.entity;

/**
 * Enumeration of marital statuses for individual customers.
 * Required for KYC in many jurisdictions and used in risk scoring.
 */
public enum MaritalStatus {
    /**
     * Single — never married.
     */
    SINGLE,

    /**
     * Currently married or in a civil union.
     */
    MARRIED,

    /**
     * Legally divorced.
     */
    DIVORCED,

    /**
     * Widowed — spouse is deceased.
     */
    WIDOWED,

    /**
     * Legally separated but not yet divorced.
     */
    SEPARATED,

    /**
     * In a registered domestic partnership.
     */
    DOMESTIC_PARTNERSHIP,

    /**
     * Customer prefers not to disclose.
     */
    PREFER_NOT_TO_SAY
}
