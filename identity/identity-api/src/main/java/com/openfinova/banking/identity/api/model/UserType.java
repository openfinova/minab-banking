package com.openfinova.banking.identity.api.model;

/**
 * Distinguishes staff (internal users) from self-service customer users. Carried as a JWT claim so
 * downstream services can apply channel-level rules without loading the user from the database on
 * every request.
 */
public enum UserType {

    /**
     * Bank employees: tellers, loan officers, GL accountants, admins, etc.
     */
    STAFF,

    /**
     * Customer portal / mobile-banking users tied to a Customer party record.
     */
    CUSTOMER,

    /**
     * Machine-to-machine service accounts used for scheduled jobs and integrations.
     */
    SYSTEM
}
