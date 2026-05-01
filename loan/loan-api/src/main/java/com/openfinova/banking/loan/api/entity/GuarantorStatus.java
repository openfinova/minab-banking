package com.openfinova.banking.loan.api.entity;

/**
 * Status of a loan guarantor.
 */
public enum GuarantorStatus {

    /** Guarantor added but pending verification */
    PENDING,

    /** Guarantor obligation is active and in force */
    ACTIVE,

    /** Guarantor released from obligation after loan repayment */
    RELEASED,

    /** Guarantee invoked and guarantor liable for payment */
    INVOKED,

    /** Guarantor removed before activation */
    REMOVED
}
