package com.openfinova.banking.loan.api.entity;

/**
 * Type of fee charged on a loan.
 */
public enum LoanFeeType {

    /** Fee for processing loan application */
    PROCESSING_FEE,

    /** Fee charged for late or missed payment */
    LATE_PAYMENT_FEE,

    /** Penalty for paying off loan early */
    PREPAYMENT_PENALTY,

    /** Fee for restructuring loan terms */
    RESTRUCTURING_FEE,

    /** Legal fees for documentation or enforcement */
    LEGAL_FEE,

    /** Fee for collateral valuation */
    VALUATION_FEE,

    /** Insurance premium for loan protection */
    INSURANCE_FEE,

    /** Fee for preparing loan documents */
    DOCUMENTATION_FEE,

    /** Other miscellaneous fees */
    OTHER
}
