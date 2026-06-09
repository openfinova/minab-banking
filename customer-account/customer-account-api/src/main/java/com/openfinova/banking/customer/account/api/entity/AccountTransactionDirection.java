package com.openfinova.banking.customer.account.api.entity;

/**
 * Customer-facing debit/credit indicator for statement transactions.
 * Direction is derived from {@link AccountTransactionType}, not stored separately.
 */
public enum AccountTransactionDirection {
    CREDIT,
    DEBIT
}
