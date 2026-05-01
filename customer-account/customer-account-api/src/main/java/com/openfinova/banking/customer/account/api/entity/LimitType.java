package com.openfinova.banking.customer.account.api.entity;

/**
 * Enumeration of account limit types that can be applied to customer accounts.
 * Defines different categories of limits for transaction and balance control.
 */
public enum LimitType {
    DAILY_TRANSACTION("Daily transaction limit"),
    WEEKLY_TRANSACTION("Weekly transaction limit"),
    MONTHLY_TRANSACTION("Monthly transaction limit"),
    ANNUAL_TRANSACTION("Annual transaction limit"),
    MAXIMUM_BALANCE("Maximum account balance limit"),
    MINIMUM_BALANCE("Minimum account balance requirement"),
    OVERDRAFT_LIMIT("Overdraft protection limit"),
    WITHDRAWAL_LIMIT("Daily withdrawal limit"),
    TRANSFER_LIMIT("Transfer amount limit"),
    VELOCITY_LIMIT("Transaction velocity limit");

    private final String description;

    LimitType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determines if this limit type is transaction-based.
     *
     * @return true if this is a transaction limit
     */
    public boolean isTransactionLimit() {
        return this == DAILY_TRANSACTION || this == WEEKLY_TRANSACTION || this == MONTHLY_TRANSACTION
                || this == ANNUAL_TRANSACTION || this == WITHDRAWAL_LIMIT || this == TRANSFER_LIMIT
                || this == VELOCITY_LIMIT;
    }

    /**
     * Determines if this limit type is balance-based.
     *
     * @return true if this is a balance limit
     */
    public boolean isBalanceLimit() {
        return this == MAXIMUM_BALANCE || this == MINIMUM_BALANCE || this == OVERDRAFT_LIMIT;
    }
}