package com.openfinova.banking.identity.entity;

/**
 * Categorises security-relevant events recorded by the identity module.
 */
public enum SecurityAuditEventType {

    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,

    /** OAuth2 authorization persisted with access token (stateless token session started). */
    OAUTH2_AUTHORIZATION_ISSUED,
    /** OAuth2 authorization removed — logout, revoke, rotation, or concurrent-session cap. */
    OAUTH2_AUTHORIZATION_REVOKED,

    MFA_CHALLENGE_PRESENTED,
    MFA_SUCCESS,
    MFA_FAILURE,
    MFA_RECOVERY_CODE_USED,
    MFA_ENABLED,
    MFA_DISABLED,

    PASSWORD_CHANGED,
    PASSWORD_EXPIRED,
    PASSWORD_FORCE_CHANGE_SET,

    ACCOUNT_LOCKED_AUTO,
    ACCOUNT_LOCKED_ADMIN,
    ACCOUNT_UNLOCKED,
    ACCOUNT_DISABLED,
    ACCOUNT_ENABLED,
    ACCOUNT_EXPIRY_SET,
    ACCOUNT_EXPIRY_CLEARED,
    ACCOUNT_EXPIRY_WARNING_SENT,

    ACCOUNT_PROVISIONING_APPROVED,
    ACCOUNT_PROVISIONING_REJECTED,

    ACCOUNT_SUSPENDED,
    ACCOUNT_REACTIVATED,

    USER_DEPROVISIONED,

    ROLE_ASSIGNED,
    /** Role change blocked: assigner cannot grant a role outside their hierarchy. */
    ROLE_ASSIGNMENT_HIERARCHY_DENIED,
    /** Role change blocked: separation-of-duties conflict. */
    ROLE_ASSIGNMENT_SOD_VIOLATION,
    ROLE_REVOKED,
    ROLE_CREATED,
    ROLE_UPDATED,
    ROLE_DELETED,
    ROLE_PERMISSIONS_CHANGED,
    PERMISSION_ADDED,
    PERMISSION_REMOVED,

    USER_CREATED,
    USER_UPDATED,

    COMPLIANCE_STATUS_CHANGED,
    KYC_VERIFIED,
    AML_SCREENING_COMPLETED,
    PEP_SCREENING_COMPLETED,

    DOA_CREATED,
    DOA_REVOKED,

    APPROVAL_WORKFLOW_STARTED,
    APPROVAL_WORKFLOW_STEP_APPROVED,
    APPROVAL_WORKFLOW_REJECTED,
    APPROVAL_WORKFLOW_CANCELLED,

    /** Authenticated caller was denied access to a protected API (e.g. missing authority). */
    API_ACCESS_DENIED
}
