package com.openfinova.banking.customer.api.entity;

/**
 * Enumeration of Data Subject Request types as defined by GDPR Articles 15–22.
 * A customer (data subject) may submit any of these requests and the bank must
 * respond within 30 calendar days (Art. 12(3)).
 */
public enum DataSubjectRequestType {

    /**
     * Art. 15 — Right of access.
     * Customer requests a copy of all personal data held about them.
     */
    ACCESS,

    /**
     * Art. 17 — Right to erasure ("right to be forgotten").
     * Customer requests deletion of their personal data.
     * May be deferred if legal retention obligations apply (Art. 17(3)(b)).
     */
    ERASURE,

    /**
     * Art. 20 — Right to data portability.
     * Customer requests their data in a structured, machine-readable format (JSON/XML).
     */
    PORTABILITY,

    /**
     * Art. 16 — Right to rectification.
     * Customer requests correction of inaccurate or incomplete personal data.
     */
    RECTIFICATION,

    /**
     * Art. 21 — Right to object.
     * Customer objects to processing of their data for a specific purpose
     * (e.g., direct marketing, profiling).
     */
    OBJECTION,

    /**
     * Art. 18 — Right to restriction of processing.
     * Customer requests that processing be restricted while accuracy or
     * lawfulness of processing is contested.
     */
    RESTRICTION
}
