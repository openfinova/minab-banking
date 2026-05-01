package com.openfinova.banking.customer.api.entity;

/**
 * Enumeration of customer onboarding lifecycle statuses.
 * Tracks a customer through the full onboarding journey from initial
 * registration to becoming an active, fully operational customer.
 */
public enum OnboardingStatus {
    /**
     * Onboarding process has been initiated (registration form submitted).
     */
    INITIATED,

    /**
     * KYC document collection and identity verification is in progress.
     */
    KYC_IN_PROGRESS,

    /**
     * KYC has been completed and the customer is verified.
     */
    KYC_COMPLETED,

    /**
     * Account setup phase — products and accounts are being configured.
     */
    ACCOUNT_SETUP,

    /**
     * Welcome kit (card, PIN, documents) has been dispatched or delivered.
     */
    WELCOME_KIT_SENT,

    /**
     * Onboarding fully completed — customer is active and operational.
     */
    COMPLETED,

    /**
     * Onboarding was abandoned by the customer or timed out.
     */
    ABANDONED,

    /**
     * Onboarding was rejected (e.g., KYC failure, sanctions hit, policy decline).
     */
    REJECTED
}
