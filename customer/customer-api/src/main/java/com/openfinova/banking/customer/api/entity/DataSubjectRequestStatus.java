package com.openfinova.banking.customer.api.entity;

/**
 * Lifecycle status of a Data Subject Request (DSAR).
 * Tracks the bank's handling of GDPR Art. 15–22 requests through to resolution.
 * The 30-day response SLA (Art. 12(3)) must be enforced at the service layer.
 */
public enum DataSubjectRequestStatus {

    /**
     * Request received and logged. Identity of the data subject not yet verified.
     */
    RECEIVED,

    /**
     * Identity of the requester is being verified to prevent unauthorised disclosure.
     */
    IDENTITY_VERIFICATION,

    /**
     * Request is being reviewed by the Data Protection / Compliance team.
     */
    IN_REVIEW,

    /**
     * Request has been fulfilled (data provided, deletion done, data corrected, etc.).
     */
    FULFILLED,

    /**
     * Request cannot be fulfilled immediately due to a legal retention obligation.
     * The customer must be informed of the reason and the expected erasure date.
     * Automatically transitions to FULFILLED once the retention window expires.
     */
    DEFERRED,

    /**
     * Request was rejected — fully outside the bank's legal obligation to comply
     * (e.g., data needed for legal proceedings, objection overridden by legitimate interest).
     * Rejection reason and legal basis must be documented.
     */
    REJECTED,

    /**
     * Request was withdrawn by the customer before completion.
     */
    WITHDRAWN
}
