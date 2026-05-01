package com.openfinova.banking.loan.api.entity;

/**
 * Status of a loan document.
 */
public enum DocumentStatus {

    /** Document is current and valid */
    ACTIVE,

    /** Document has passed its expiry date */
    EXPIRED,

    /** Document archived for record keeping */
    ARCHIVED,

    /** Document replaced by a newer version */
    SUPERSEDED
}
