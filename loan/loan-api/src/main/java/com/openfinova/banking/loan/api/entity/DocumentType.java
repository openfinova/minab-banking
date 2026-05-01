package com.openfinova.banking.loan.api.entity;

/**
 * Type of document associated with a loan.
 */
public enum DocumentType {

    /** Legal agreement between bank and borrower */
    LOAN_AGREEMENT,

    /** Promissory note signed by borrower */
    PROMISSORY_NOTE,

    /** Documents related to pledged collateral */
    COLLATERAL_DOCUMENT,

    /** Insurance policy for loan or collateral */
    INSURANCE_POLICY,

    /** Agreement signed by guarantor */
    GUARANTOR_AGREEMENT,

    /** Salary slips, tax returns, or other income verification */
    INCOME_PROOF,

    /** National ID, passport, or other identity documents */
    IDENTITY_PROOF,

    /** Utility bills or other address verification */
    ADDRESS_PROOF,

    /** Professional valuation report for collateral */
    VALUATION_REPORT,

    /** Other supporting documents */
    OTHER
}
