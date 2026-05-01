package com.openfinova.banking.loan.api.entity;

/**
 * Method used to make a loan payment.
 */
public enum PaymentMethod {

    /** Cash payment at branch or agent */
    CASH,

    /** Electronic bank transfer */
    BANK_TRANSFER,

    /** Payment by cheque */
    CHEQUE,

    /** Automatic debit from customer account */
    DIRECT_DEBIT,

    /** Payment by debit or credit card */
    CARD,

    /** Payment via mobile money service */
    MOBILE_MONEY,

    /** Payment through online banking portal */
    ONLINE
}
