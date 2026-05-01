package com.openfinova.banking.customer.api.entity;

/**
 * Enumeration of customer lifecycle statuses.
 */
public enum CustomerStatus {
    /**
     * Prospect, not yet a full customer.
     */
    PROSPECT,

    /**
     * Active customer with good standing.
     */
    ACTIVE,

    /**
     * Inactive customer (e.g., no active accounts).
     */
    INACTIVE,

    /**
     * Blocked due to suspicious activity or admin action.
     */
    BLOCKED,

    /**
     * Deceased (for individual customers) or Dissolved (for businesses).
     */
    DECEASED,

    /**
     * Closed — all accounts terminated, no further transactions.
     */
    CLOSED,

    /**
     * Anonymized — personal data has been removed or pseudonymized in compliance with
     * GDPR Art. 17 (right to erasure) after the mandatory AML/legal retention period
     * has expired. The structural record is preserved for audit-trail integrity
     * (transaction references, account history) but contains no identifiable information.
     */
    ANONYMIZED
}
