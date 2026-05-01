package com.openfinova.banking.loan.api.entity;

/**
 * Status of collateral pledged against a loan.
 */
public enum CollateralStatus {

    /** Collateral currently securing the loan */
    ACTIVE,

    /** Collateral released back to borrower after loan repayment */
    RELEASED,

    /** Collateral sold to recover loan amount after default */
    LIQUIDATED,

    /** Collateral being valued or revalued */
    UNDER_VALUATION
}
