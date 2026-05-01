package com.openfinova.banking.customer.api.entity;

/**
 * Enumeration of gender identities for individual customers.
 * Used for KYC identity verification and regulatory reporting.
 */
public enum Gender {
    /**
     * Male gender.
     */
    MALE,

    /**
     * Female gender.
     */
    FEMALE,

    /**
     * Non-binary or gender-diverse.
     */
    NON_BINARY,

    /**
     * Customer prefers not to disclose.
     */
    PREFER_NOT_TO_SAY
}
