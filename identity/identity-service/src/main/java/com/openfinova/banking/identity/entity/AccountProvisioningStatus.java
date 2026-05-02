package com.openfinova.banking.identity.entity;

/**
 * Provisioning lifecycle for a new account. Only {@link #ACTIVE} accounts may authenticate.
 */
public enum AccountProvisioningStatus {

    /**
     * Awaiting maker–checker or HR approval before the account may be used.
     */
    PENDING_APPROVAL,

    /**
     * Approved and eligible for authentication (subject to enable/suspend/lock rules).
     */
    ACTIVE,

    /**
     * Provisioning was rejected; the record remains for audit but cannot authenticate.
     */
    REJECTED,

    /**
     * Account was deprovisioned; credentials and entitlements were cleared.
     */
    DEPROVISIONED
}
