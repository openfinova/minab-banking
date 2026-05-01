package com.openfinova.banking.customer.api.entity;

/**
 * Enumeration of address types.
 */
public enum AddressType {
    /**
     * Legal/Registered address.
     */
    LEGAL,

    /**
     * Physical address if different from legal (e.g., store location).
     */
    PHYSICAL,

    /**
     * Mailing address for correspondence.
     */
    MAILING,

    /**
     * Official registered office (for businesses).
     */
    REGISTERED_OFFICE
}
