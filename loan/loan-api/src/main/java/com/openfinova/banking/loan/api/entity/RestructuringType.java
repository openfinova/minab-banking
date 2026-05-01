package com.openfinova.banking.loan.api.entity;

/**
 * Type of loan restructuring or modification.
 */
public enum RestructuringType {

    /** Extending loan maturity date to reduce installment amount */
    TERM_EXTENSION,

    /** Reducing interest rate to ease repayment burden */
    RATE_REDUCTION,

    /** Temporary suspension of all payments */
    PAYMENT_HOLIDAY,

    /** Temporary suspension of principal payments (interest continues) */
    PRINCIPAL_MORATORIUM,

    /** Comprehensive restructuring with multiple changes */
    FULL_RESTRUCTURE
}
