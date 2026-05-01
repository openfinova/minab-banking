package com.openfinova.banking.customer.api.entity;

/**
 * Enumeration of Know Your Customer (KYC) compliance status.
 */
public enum KYCStatus {
    /**
     * KYC process not yet started.
     */
    PENDING,

    /**
     * Documents submitted, verification in progress.
     */
    IN_REVIEW,

    /**
     * KYC verified successfully.
     */
    VERIFIED,

    /**
     * KYC verification failed or rejected.
     */
    REJECTED,

    /**
     * Information outdated, re-verification required.
     */
    EXPIRED
}
