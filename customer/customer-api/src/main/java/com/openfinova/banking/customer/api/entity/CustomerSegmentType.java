package com.openfinova.banking.customer.api.entity;

/**
 * Enumeration of customer CRM segments.
 * Drives product eligibility, fee structures, relationship manager assignment,
 * and service-level agreements.
 */
public enum CustomerSegmentType {
    /**
     * Standard retail/personal banking customer.
     */
    RETAIL,

    /**
     * Premium retail customer with elevated service.
     */
    PREMIUM,

    /**
     * High-net-worth individual — private banking tier.
     */
    PRIVATE_BANKING,

    /**
     * Small and medium enterprise.
     */
    SME,

    /**
     * Large corporate client.
     */
    CORPORATE,

    /**
     * Ultra-high-net-worth or strategic relationship client.
     */
    VIP,

    /**
     * Mass-market or entry-level customer (e.g., basic savings, digital-only).
     */
    MASS_MARKET,

    /**
     * Non-resident or expatriate customer.
     */
    NON_RESIDENT
}
