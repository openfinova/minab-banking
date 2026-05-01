package com.openfinova.banking.gl.api.entity;

/**
 * Classification codes for why a transaction was posted to suspense.
 *
 * Used for:
 * - Root cause analysis
 * - Process improvement
 * - Regulatory reporting
 * - AML/fraud pattern detection
 */
public enum SuspenseReasonCode {

    /**
     * Payment received without valid account number or account not found.
     */
    INVALID_ACCOUNT_NUMBER,

    /**
     * Customer/beneficiary information incomplete or ambiguous.
     */
    INCOMPLETE_BENEFICIARY_INFO,

    /**
     * Payment reference missing or doesn't match expected format.
     */
    MISSING_PAYMENT_REFERENCE,

    /**
     * System error during transaction processing.
     */
    SYSTEM_ERROR,

    /**
     * Duplicate transaction detected, awaiting investigation.
     */
    DUPLICATE_TRANSACTION,

    /**
     * ATM/branch reconciliation difference.
     */
    RECONCILIATION_DIFFERENCE,

    /**
     * Foreign exchange or currency conversion issue.
     */
    FX_CONVERSION_ERROR,

    /**
     * Third-party integration failure or timeout.
     */
    INTEGRATION_FAILURE,

    /**
     * Transaction amount mismatch between systems.
     */
    AMOUNT_MISMATCH,

    /**
     * Unidentified deposit or credit.
     * AML concern - requires enhanced due diligence.
     */
    UNIDENTIFIED_DEPOSIT,

    /**
     * Reversal or chargeback without clear original transaction.
     */
    ORPHAN_REVERSAL,

    /**
     * Manual journal entry pending approval/correction.
     */
    MANUAL_ENTRY_PENDING,

    /**
     * Awaiting additional documentation or customer confirmation.
     */
    AWAITING_DOCUMENTATION,

    /**
     * Other reason not covered by standard codes.
     */
    OTHER;

    /**
     * Determine if this reason code requires AML review.
     * Unidentified deposits are high risk per FATF guidelines.
     */
    public boolean requiresAMLReview() {
        return this == UNIDENTIFIED_DEPOSIT || this == OTHER;
    }
}
