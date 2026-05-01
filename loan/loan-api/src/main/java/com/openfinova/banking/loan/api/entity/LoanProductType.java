package com.openfinova.banking.loan.api.entity;

/**
 * Types of loan products offered by the bank.
 */
public enum LoanProductType {

    /** Unsecured personal loan for individual borrowers */
    PERSONAL_LOAN,

    /** Mortgage loan secured by residential or commercial property */
    HOME_LOAN,

    /** Loan for purchasing vehicles, secured by the vehicle itself */
    AUTO_LOAN,

    /** Loan for business purposes including working capital and expansion */
    BUSINESS_LOAN,

    /** Loan for educational expenses including tuition and living costs */
    EDUCATION_LOAN,

    /** Short-term loan secured by gold jewelry or ornaments */
    GOLD_LOAN,

    /** Revolving credit facility allowing withdrawals up to a limit */
    OVERDRAFT,

    /** Pre-approved credit line that can be drawn as needed */
    CREDIT_LINE
}
