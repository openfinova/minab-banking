package com.openfinova.banking.loan.api.entity;

/**
 * Types of loan products offered by the bank.
 */
public enum LoanProductType {

    /** Unsecured personal loan for individual borrowers */
    PERSONAL_LOAN("Personal Loan", "A loan for individual borrowers"),

    /** Mortgage loan secured by residential or commercial property */
    HOME_LOAN("Home Loan", "A loan for residential or commercial property"),

    /** Loan for purchasing vehicles, secured by the vehicle itself */
    AUTO_LOAN("Auto Loan", "A loan for purchasing vehicles, secured by the vehicle itself"),

    /** Loan for business purposes including working capital and expansion */
    BUSINESS_LOAN("Business Loan", "A loan for business purposes including working capital and expansion"),

    /** Loan for educational expenses including tuition and living costs */
    EDUCATION_LOAN("Education Loan", "A loan for educational expenses including tuition and living costs"),

    /** Short-term loan secured by gold jewelry or ornaments */
    GOLD_LOAN("Gold Loan", "A short-term loan secured by gold jewelry or ornaments"),

    /** Revolving credit facility allowing withdrawals up to a limit */
    OVERDRAFT("Overdraft", "A revolving credit facility allowing withdrawals up to a limit"),

    /** Pre-approved credit line that can be drawn as needed */
    CREDIT_LINE("Credit Line", "A pre-approved credit line that can be drawn as needed");

    private final String name;
    private final String description;

    LoanProductType(String name, String description) {
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
