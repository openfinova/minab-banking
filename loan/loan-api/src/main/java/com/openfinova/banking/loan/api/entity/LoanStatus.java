package com.openfinova.banking.loan.api.entity;

/**
 * Lifecycle status of a loan account.
 */
public enum LoanStatus {

    /** Loan created but awaiting final approval before disbursement */
    PENDING_APPROVAL,

    /** Loan approved and ready for disbursement */
    APPROVED,

    /** Loan disbursed and currently being repaid */
    ACTIVE,

    /** Loan temporarily suspended due to operational or compliance issues */
    SUSPENDED,

    /** Loan fully repaid and closed normally */
    CLOSED,

    /** Loan deemed uncollectible and removed from active portfolio */
    WRITTEN_OFF,

    /** Loan terms modified due to borrower hardship or other reasons */
    RESTRUCTURED,

    /** Loan paid off early through settlement process */
    SETTLED
}
