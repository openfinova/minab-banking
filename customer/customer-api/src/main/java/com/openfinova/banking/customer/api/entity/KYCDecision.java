package com.openfinova.banking.customer.api.entity;

/**
 * Enumeration of KYC review decisions.
 */
public enum KYCDecision {
    /**
     * KYC documents and information approved.
     */
    APPROVED,

    /**
     * KYC documents or information rejected.
     */
    REJECTED,

    /**
     * Additional information or documents required.
     */
    REQUIRES_ADDITIONAL_INFO
}
