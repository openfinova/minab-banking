package com.openfinova.banking.loan.api.entity;

/**
 * Frequency of loan repayment installments.
 */
public enum RepaymentFrequency {

    /** Payments due every day */
    DAILY,

    /** Payments due every week */
    WEEKLY,

    /** Payments due every two weeks (26 payments per year) */
    BIWEEKLY,

    /** Payments due every month (most common) */
    MONTHLY,

    /** Payments due every three months */
    QUARTERLY,

    /** Payments due every six months */
    SEMI_ANNUALLY,

    /** Payments due once per year */
    ANNUALLY,

    /** Single payment at maturity (principal and interest) */
    BULLET
}
