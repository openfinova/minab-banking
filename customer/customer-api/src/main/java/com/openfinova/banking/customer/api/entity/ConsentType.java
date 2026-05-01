package com.openfinova.banking.customer.api.entity;

/**
 * Enumeration of customer consent types.
 * Required for GDPR Article 6/7 compliance, ePrivacy Directive, and
 * banking-specific regulatory consent obligations.
 */
public enum ConsentType {
    /**
     * Consent to receive marketing communications via email.
     */
    MARKETING_EMAIL,

    /**
     * Consent to receive marketing communications via SMS.
     */
    MARKETING_SMS,

    /**
     * Consent to receive marketing communications via telephone.
     */
    MARKETING_PHONE,

    /**
     * Consent to share data with unaffiliated third parties.
     */
    DATA_SHARING_THIRD_PARTY,

    /**
     * Consent to share data within affiliated group companies.
     */
    DATA_SHARING_AFFILIATES,

    /**
     * Consent for credit reference agency reporting.
     */
    CREDIT_REPORTING,

    /**
     * Consent for profiling and automated decision-making (GDPR Art. 22).
     */
    AUTOMATED_DECISION_MAKING,

    /**
     * Acceptance of Terms and Conditions.
     */
    TERMS_AND_CONDITIONS,

    /**
     * Acceptance of the Privacy Policy.
     */
    PRIVACY_POLICY,

    /**
     * Consent to use biometric data for authentication.
     */
    BIOMETRIC_DATA_PROCESSING
}
