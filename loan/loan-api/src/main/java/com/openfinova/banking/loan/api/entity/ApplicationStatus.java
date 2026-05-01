package com.openfinova.banking.loan.api.entity;

/**
 * Status of a loan application through its workflow.
 */
public enum ApplicationStatus {

    /** Application created but not yet submitted by customer */
    DRAFT,

    /** Application submitted and awaiting initial review */
    SUBMITTED,

    /** Application being reviewed by loan officer */
    UNDER_REVIEW,

    /** Application on hold waiting for additional documents from customer */
    PENDING_DOCUMENTS,

    /** Application in underwriting process for credit assessment */
    UNDERWRITING,

    /** Application approved and ready for loan account creation */
    APPROVED,

    /** Application rejected due to credit, policy, or other reasons */
    REJECTED,

    /** Application withdrawn by customer before decision */
    WITHDRAWN,

    /** Application expired due to inactivity or time limit */
    EXPIRED
}
