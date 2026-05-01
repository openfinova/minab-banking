package com.openfinova.banking.loan.api.entity;

/**
 * Status of a collection activity.
 */
public enum CollectionStatus {

    /** Activity scheduled but not yet started */
    PENDING,

    /** Activity currently being worked on */
    IN_PROGRESS,

    /** Activity completed successfully */
    COMPLETED,

    /** Activity escalated to higher authority or legal team */
    ESCALATED,

    /** Activity closed without further action */
    CLOSED
}
