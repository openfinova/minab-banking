package com.openfinova.banking.customer.account.api.entity;

/**
 * Enumeration of account product types supported by the Customer Account
 * Management system.
 * Each product type has a display name and code for identification and GL
 * account mapping.
 */
public enum AccountProductType {
    /**
     * Standard checking account for daily transactions.
     */
    CHECKING("Checking Account", "CHK"),

    /**
     * Savings account for accruing interest.
     */
    SAVINGS("Savings Account", "SAV"),

    /**
     * Money Market Account offering higher interest rates with some restrictions.
     */
    MONEY_MARKET("Money Market Account", "MMA"),

    /**
     * Time deposit with fixed term and interest rate.
     */
    CERTIFICATE_OF_DEPOSIT("Certificate of Deposit", "CD"),

    /**
     * Line of credit or overdraft facility.
     */
    CREDIT_LINE("Credit Line", "LOC"),

    /**
     * Account holding investment assets.
     */
    INVESTMENT("Investment Account", "INV");

    private final String displayName;
    private final String code;

    AccountProductType(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }
}