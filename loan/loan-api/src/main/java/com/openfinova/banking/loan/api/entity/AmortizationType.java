package com.openfinova.banking.loan.api.entity;

/**
 * Method of loan principal and interest repayment over time.
 */
public enum AmortizationType {

    /** Equal monthly installments (EMI) - same amount each period */
    EQUAL_INSTALLMENTS,

    /** Equal principal payments with decreasing interest */
    EQUAL_PRINCIPAL,

    /** Small periodic payments with large final payment */
    BALLOON_PAYMENT,

    /** Interest-only payments with principal due at maturity */
    BULLET_PAYMENT,

    /** Custom repayment schedule defined per loan */
    CUSTOM
}
