package com.openfinova.banking.gl.api.entity;

/**
 * Enumeration of standard account types in double-entry bookkeeping.
 * Each type has a normal balance side (debit or credit).
 */
public enum GLAccountType {
    /**
     * Assets - resources owned by the organization (normal debit balance)
     */
    ASSET,

    /**
     * Liabilities - debts owed by the organization (normal credit balance)
     */
    LIABILITY,

    /**
     * Equity - owner's interest in the organization (normal credit balance)
     */
    EQUITY,

    /**
     * Revenue - income earned by the organization (normal credit balance)
     */
    REVENUE,

    /**
     * Expenses - costs incurred by the organization (normal debit balance)
     */
    EXPENSE;

    /**
     * Determines if this account type has a normal debit balance.
     *
     * @return true if the account type normally has a debit balance
     */
    public boolean isDebitNormal() {
        return this == ASSET || this == EXPENSE;
    }
}