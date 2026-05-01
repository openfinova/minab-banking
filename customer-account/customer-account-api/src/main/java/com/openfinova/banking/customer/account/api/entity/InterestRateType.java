package com.openfinova.banking.customer.account.api.entity;

/**
 * Type of interest rate for customer accounts.
 * CREDIT = interest paid on positive balance; DEBIT = interest charged on overdraft.
 */
public enum InterestRateType {
    CREDIT,
    DEBIT
}
