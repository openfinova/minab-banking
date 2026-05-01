package com.openfinova.banking.gl.api.entity;

/**
 * Enumeration of balance types indicating the normal balance side for an
 * account.
 */
public enum BalanceType {
    /**
     * Debit normal balance - increases with debits (Assets, Expenses)
     */
    DEBIT,

    /**
     * Credit normal balance - increases with credits (Liabilities, Equity, Revenue)
     */
    CREDIT
}