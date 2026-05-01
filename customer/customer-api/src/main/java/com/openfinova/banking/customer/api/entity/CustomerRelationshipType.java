package com.openfinova.banking.customer.api.entity;

/**
 * Enumeration of customer relationship types.
 */
public enum CustomerRelationshipType {
    /**
     * Spouse or domestic partner.
     */
    SPOUSE,

    /**
     * Business partner or co-owner.
     */
    BUSINESS_PARTNER,

    /**
     * Parent-child relationship.
     */
    PARENT,

    /**
     * Child of the primary customer.
     */
    CHILD,

    /**
     * Sibling relationship.
     */
    SIBLING,

    /**
     * Authorized user on accounts (e.g., authorized signer).
     */
    AUTHORIZED_USER,

    /**
     * Power of attorney holder.
     */
    POWER_OF_ATTORNEY,

    /**
     * Beneficiary designation.
     */
    BENEFICIARY,

    /**
     * Legal guardian.
     */
    GUARDIAN,

    /**
     * Corporate officer or director.
     */
    CORPORATE_OFFICER
}
