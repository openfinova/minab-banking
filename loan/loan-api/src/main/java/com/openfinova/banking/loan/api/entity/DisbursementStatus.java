package com.openfinova.banking.loan.api.entity;

/**
 * Status of loan disbursement process.
 */
public enum DisbursementStatus {

    /** Disbursement request created but not yet approved */
    PENDING,

    /** Disbursement approved and ready for processing */
    APPROVED,

    /** Disbursement currently being processed */
    PROCESSING,

    /** Disbursement successfully completed */
    COMPLETED,

    /** Disbursement failed due to technical or validation error */
    FAILED,

    /** Disbursement reversed after completion */
    REVERSED
}
