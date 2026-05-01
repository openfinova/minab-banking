package com.openfinova.banking.loan.api.entity;

/**
 * Status of an early settlement quote.
 */
public enum SettlementStatus {

    /** Settlement quote generated and valid */
    QUOTE,

    /** Settlement pending approval */
    PENDING_APPROVAL,

    /** Settlement approved */
    APPROVED,

    /** Settlement rejected */
    REJECTED,

    /** Settlement completed and loan closed */
    COMPLETED,

    /** Settlement quote cancelled before execution */
    CANCELLED,

    /** Quote validity period expired */
    EXPIRED
}
