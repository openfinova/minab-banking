package com.openfinova.banking.customer.api.entity;

/**
 * Enumeration of legal entity types for business customers (CustomerType.BUSINESS or TRUST).
 * Required for KYC, beneficial ownership determination, and regulatory classification.
 */
public enum LegalEntityType {
    /**
     * Limited Liability Company.
     */
    LLC,

    /**
     * Corporation (Inc., Corp., Ltd.).
     */
    CORPORATION,

    /**
     * General or limited partnership.
     */
    PARTNERSHIP,

    /**
     * Limited Liability Partnership.
     */
    LLP,

    /**
     * Sole proprietorship / sole trader.
     */
    SOLE_PROPRIETORSHIP,

    /**
     * Non-profit or not-for-profit organization.
     */
    NON_PROFIT,

    /**
     * Cooperative.
     */
    COOPERATIVE,

    /**
     * Trust (also covers CustomerType.TRUST directly).
     */
    TRUST,

    /**
     * Foundation or charitable entity.
     */
    FOUNDATION,

    /**
     * Government or public sector body.
     */
    GOVERNMENT_ENTITY,

    /**
     * Branch or representative office of a foreign entity.
     */
    BRANCH_OFFICE
}
