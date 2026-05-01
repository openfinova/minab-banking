package com.openfinova.banking.loan.api.entity;

/**
 * Type of collection activity performed for delinquent loans.
 */
public enum CollectionActivityType {

    /** Phone call to borrower */
    PHONE_CALL,

    /** SMS reminder sent to borrower */
    SMS,

    /** Email communication sent to borrower */
    EMAIL,

    /** Physical letter mailed to borrower */
    LETTER,

    /** In-person visit to borrower's location */
    FIELD_VISIT,

    /** Formal legal notice sent to borrower */
    LEGAL_NOTICE,

    /** Borrower committed to pay by specific date */
    PROMISE_TO_PAY,

    /** Negotiated payment plan agreed with borrower */
    PAYMENT_ARRANGEMENT
}
