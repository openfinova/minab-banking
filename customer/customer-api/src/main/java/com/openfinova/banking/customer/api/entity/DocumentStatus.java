package com.openfinova.banking.customer.api.entity;

/**
 * Enumeration of lifecycle statuses for identification documents.
 * Tracks the document through the KYC-verification pipeline.
 */
public enum DocumentStatus {
    /**
     * Document has been submitted by the customer, awaiting review.
     */
    SUBMITTED,

    /**
     * Document is currently under review by a compliance officer.
     */
    UNDER_REVIEW,

    /**
     * Document has been verified and accepted.
     */
    VERIFIED,

    /**
     * Document has been rejected (e.g., tampered, illegible, mismatched data).
     */
    REJECTED,

    /**
     * Document has passed its expiry date and is no longer valid.
     */
    EXPIRED,

    /**
     * Document was superseded by a newer version submitted by the customer.
     */
    SUPERSEDED
}
