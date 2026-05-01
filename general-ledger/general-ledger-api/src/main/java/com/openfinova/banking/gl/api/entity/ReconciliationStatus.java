package com.openfinova.banking.gl.api.entity;

/**
 * Status of statement reconciliation for a GL transaction.
 * Used to prevent reversal of transactions that have been matched to external
 * statements (e.g. Central Bank, SWIFT Nostro).
 */
public enum ReconciliationStatus {

    /**
     * Transaction has been reconciled against an external statement.
     * Reversal is not allowed; use an adjusting journal entry instead.
     */
    RECONCILED,

    /**
     * Transaction is pending reconciliation (not yet matched to external statement).
     */
    PENDING,

    /**
     * Reconciliation was voided (e.g. un-reconciled). Transaction may be reversed if otherwise allowed.
     */
    VOIDED;
}
