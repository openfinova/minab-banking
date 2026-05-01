package com.openfinova.banking.loan.api.entity;

/**
 * Type of financial transaction on a loan account.
 */
public enum LoanTransactionType {

    /** Loan amount disbursed to borrower */
    DISBURSEMENT,

    /** Payment received from borrower */
    REPAYMENT,

    /** Fee charged to loan account */
    FEE_CHARGE,

    /** Penalty charged for late payment or violation */
    PENALTY_CHARGE,

    /** Interest accrued on outstanding balance */
    INTEREST_ACCRUAL,

    /** Interest amount waived by bank */
    INTEREST_WAIVER,

    /** Fee amount waived by bank */
    FEE_WAIVER,

    /** Penalty amount waived by bank */
    PENALTY_WAIVER,

    /** Loan written off as bad debt */
    WRITE_OFF,

    /** Amount recovered from written-off loan */
    RECOVERY,

    /** Reversal of a previous transaction */
    REVERSAL
}
