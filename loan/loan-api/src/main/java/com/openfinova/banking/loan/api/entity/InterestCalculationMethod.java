package com.openfinova.banking.loan.api.entity;

/**
 * Methods for calculating interest on loan principal.
 */
public enum InterestCalculationMethod {

    /** Interest calculated on original principal for entire loan term */
    FLAT_RATE("Flat Rate", "Interest calculated on original principal for entire loan term"),

    /** Interest calculated on outstanding principal balance (most common) */
    REDUCING_BALANCE("Reducing Balance", "Interest calculated on outstanding principal balance (most common)"),

    /** Simple interest: I = P × R × T */
    SIMPLE_INTEREST("Simple Interest", "Simple interest: I = P × R × T"),

    /** Interest compounded at specified intervals */
    COMPOUND_INTEREST("Compound Interest", " Interest compounded at specified intervals"),

    /** Interest calculated daily on outstanding balance */
    DAILY_REDUCING("Daily Reducing", "Interest calculated daily on outstanding balance"),

    /** Pre-computed interest with rebate formula for early payoff */
    RULE_OF_78("Rule of 78", "Pre-computed interest with rebate formula for early payoff");

    private final String name;
    private final String description;

    InterestCalculationMethod(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
