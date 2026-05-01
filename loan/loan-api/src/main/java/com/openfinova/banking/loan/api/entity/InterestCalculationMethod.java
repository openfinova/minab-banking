package com.openfinova.banking.loan.api.entity;

/**
 * Methods for calculating interest on loan principal.
 */
public enum InterestCalculationMethod {

    /** Interest calculated on original principal for entire loan term */
    FLAT_RATE,

    /** Interest calculated on outstanding principal balance (most common) */
    REDUCING_BALANCE,

    /** Simple interest: I = P × R × T */
    SIMPLE_INTEREST,

    /** Interest compounded at specified intervals */
    COMPOUND_INTEREST,

    /** Interest calculated daily on outstanding balance */
    DAILY_REDUCING,

    /** Pre-computed interest with rebate formula for early payoff */
    RULE_OF_78
}
