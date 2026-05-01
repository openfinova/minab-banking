package com.openfinova.banking.loan.api.entity;

/**
 * Type of payment made towards a loan.
 */
public enum PaymentType {

    /** Scheduled installment payment */
    REGULAR_PAYMENT,

    /** Additional payment towards principal before due date */
    PREPAYMENT,

    /** Full payoff of remaining loan balance */
    EARLY_SETTLEMENT,

    /** Payment of late payment fee */
    LATE_FEE,

    /** Payment of penalty charges */
    PENALTY,

    /** Fee paid for loan restructuring */
    RESTRUCTURING_FEE,

    /** Reversal of a previous payment */
    REVERSAL
}
