package com.openfinova.banking.customer.account.api.entity;

/**
 * Enumeration of transaction types that can be performed on a customer account.
 */
public enum AccountTransactionType {
    /**
     * Funds deposited into the account (e.g., ATM, Branch, Mobile Check).
     */
    DEPOSIT("Deposit funds"),

    /**
     * Funds withdrawn from the account (e.g., ATM, Branch).
     */
    WITHDRAWAL("Withdraw funds"),

    /**
     * Funds transferred into the account from another internal or external account.
     */
    TRANSFER_IN("Incoming transfer"),

    /**
     * Funds transferred out of the account to another internal or external account.
     */
    TRANSFER_OUT("Outgoing transfer"),

    /**
     * Fee charged to the account (e.g., monthly maintenance, overdraft).
     */
    FEE("Account fee"),

    /**
     * Interest paid to the customer (Credit).
     */
    INTEREST_CREDIT("Interest earned"),

    /**
     * Interest charged to the customer (Debit).
     */
    INTEREST_CHARGE("Interest charged"),

    /**
     * Administrative adjustment to the account balance.
     */
    ADJUSTMENT("Balance adjustment");

    private final String description;

    AccountTransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
