package com.openfinova.banking.customer.entity;

/**
 * Enumeration of all recognised audit action types for customer profile changes.
 *
 * <p>Using a closed enum instead of a free-form string prevents typos, enables
 * exhaustive switch-case analysis, and makes index cardinality predictable.</p>
 */
public enum CustomerAuditAction {

    /** One or more core profile fields (name, DOB, nationality, …) were updated. */
    PROFILE_UPDATE,

    /** The customer's lifecycle status changed (e.g. ACTIVE → SUSPENDED). */
    STATUS_CHANGE,

    /** The customer record was anonymised under GDPR Art. 17 / Art. 5(1)(e). */
    ANONYMIZATION,

    /** KYC status or related compliance data was updated. */
    KYC_UPDATE,

    /** A new address was added to the customer's profile. */
    ADDRESS_ADDED,

    /** An existing address was updated. */
    ADDRESS_UPDATED,

    /** An address was removed from the customer's profile. */
    ADDRESS_REMOVED,

    /** A contact detail (phone, e-mail) was verified. */
    CONTACT_VERIFIED,

    /** An identification document was added. */
    DOCUMENT_ADDED,

    /** An identification document was removed. */
    DOCUMENT_REMOVED,

    /** A marketing / processing consent preference was changed. */
    CONSENT_CHANGE,

    /** A relationship (joint account, beneficiary, PoA, …) was added. */
    RELATIONSHIP_ADDED,

    /** A relationship was removed. */
    RELATIONSHIP_REMOVED,

    /** A data subject access request was submitted. */
    DSAR_SUBMITTED,

    /** A data subject access request was fulfilled. */
    DSAR_FULFILLED,

    /** A data subject access request was rejected. */
    DSAR_REJECTED,
}
