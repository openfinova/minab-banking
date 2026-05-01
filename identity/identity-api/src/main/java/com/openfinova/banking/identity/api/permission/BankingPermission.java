package com.openfinova.banking.identity.api.permission;

/**
 * Fine-grained permission catalogue for the entire banking platform.
 *
 * Permissions follow the pattern  {@code domain:action}  and are stored as
 * authorities in the JWT under a custom {@code permissions} claim.
 * Use {@code hasAuthority('loan:read')} (or the constant) in @PreAuthorize
 * expressions.
 *
 * Roles (LOAN_OFFICER, GL_MANAGER, ...) are coarse role bundles; each role owns
 * a set of these permissions which are then flattened into the token so
 * endpoint guards never need to know role names.
 */
public enum BankingPermission {

    // ── Loan ───────────────────────────────────────────────────────────────────
    LOAN_READ("loan:read"),
    LOAN_WRITE("loan:write"),
    LOAN_APPROVE("loan:approve"),
    LOAN_DISBURSE("loan:disburse"),
    /** Approve or complete a disbursement after initiation (maker–checker). */
    LOAN_DISBURSE_APPROVE("loan:disburse:approve"),
    LOAN_RESTRUCTURE("loan:restructure"),
    /** Approve or reject a loan restructuring request (maker–checker). */
    LOAN_RESTRUCTURE_APPROVE("loan:restructure:approve"),
    /** Record collections and payments; use {@link #LOAN_COLLECT_APPROVE} for supervisory approval steps. */
    LOAN_COLLECT("loan:collect"),
    /** Approve collection outcomes (e.g. settlement, waiver) separate from day-to-day recording. */
    LOAN_COLLECT_APPROVE("loan:collect:approve"),
    LOAN_WRITE_OFF("loan:write-off"),
    /** Customer self-service: view own loans only. */
    LOAN_READ_OWN("loan:read:own"),

    // ── Account ────────────────────────────────────────────────────────────────
    ACCOUNT_READ("account:read"),
    ACCOUNT_WRITE("account:write"),
    ACCOUNT_TRANSFER("account:transfer"),
    /** Customer self-service: view own accounts only. */
    ACCOUNT_READ_OWN("account:read:own"),
    ACCOUNT_TRANSFER_OWN("account:transfer:own"),

    // ── Customer ───────────────────────────────────────────────────────────────
    CUSTOMER_READ("customer:read"),
    CUSTOMER_WRITE("customer:write"),
    /** Access to PII fields: tax ID, date of birth, mother's maiden name, etc. */
    CUSTOMER_PII_READ("customer:pii:read"),
    /** Customer self-service: view own profile. */
    CUSTOMER_READ_OWN("customer:read:own"),
    /** Customer self-service: update own profile. */
    CUSTOMER_WRITE_OWN("customer:write:own"),

    // ── Self-service (identity / portal) ───────────────────────────────────────
    /** View own identity access info (roles, claims) from {@code /identity/me}. */
    PROFILE_READ_OWN("profile:read:own"),
    /** Change own password via self-service. */
    PASSWORD_CHANGE_OWN("password:change:own"),
    /** Enroll, verify, or disable own MFA. */
    MFA_MANAGE_OWN("mfa:manage:own"),
    /** View own security audit events only. */
    AUDIT_READ_OWN("audit:read:own"),

    // ── General Ledger ─────────────────────────────────────────────────────────
    GL_READ("gl:read"),
    GL_POST("gl:post"),
    GL_APPROVE("gl:approve"),

    // ── Payments ───────────────────────────────────────────────────────────────
    PAYMENT_INITIATE("payment:initiate"),
    PAYMENT_INITIATE_OWN("payment:initiate:own"),

    // ── Exchange Rate ──────────────────────────────────────────────────────────
    EXCHANGE_RATE_READ("exchange-rate:read"),
    EXCHANGE_RATE_WRITE("exchange-rate:write"),

    // ── Audit & Reporting ──────────────────────────────────────────────────────
    AUDIT_READ("audit:read"),
    REPORT_READ("report:read"),
    REPORT_GENERATE("report:generate"),

    // ── Transaction Processing ─────────────────────────────────────────────────
    TRANSACTION_READ("transaction:read"),
    TRANSACTION_WRITE("transaction:write"),
    VELOCITY_LIMIT_READ("velocity-limit:read"),
    VELOCITY_LIMIT_WRITE("velocity-limit:write"),
    FEE_READ("fee:read"),
    FEE_WRITE("fee:write"),
    COMPENSATION_READ("compensation:read"),
    COMPENSATION_WRITE("compensation:write"),

    // ── Enhanced General Ledger ────────────────────────────────────────────────
    GL_SUSPENSE_READ("gl:suspense:read"),
    GL_SUSPENSE_WRITE("gl:suspense:write"),
    GL_REVALUATION_READ("gl:revaluation:read"),
    GL_REVALUATION_WRITE("gl:revaluation:write"),
    GL_FISCAL_PERIOD_READ("gl:fiscal-period:read"),
    GL_FISCAL_PERIOD_WRITE("gl:fiscal-period:write"),
    GL_SETUP_READ("gl:setup:read"),
    GL_SETUP_WRITE("gl:setup:write"),

    // ── Setup Service ──────────────────────────────────────────────────────────
    BANK_CONFIG_READ("bank:config:read"),
    BANK_CONFIG_WRITE("bank:config:write"),
    HOLIDAY_READ("holiday:read"),
    HOLIDAY_WRITE("holiday:write"),

    // ── Customer Account Management ────────────────────────────────────────────
    CUSTOMER_ACCOUNT_READ("customer-account:read"),
    CUSTOMER_ACCOUNT_WRITE("customer-account:write"),
    CUSTOMER_ACCOUNT_LIMIT_READ("customer-account:limit:read"),
    CUSTOMER_ACCOUNT_LIMIT_WRITE("customer-account:limit:write"),

    // ── Inter-Service Communication ────────────────────────────────────────────
    SERVICE_EXCHANGE_RATE_READ("service:exchange-rate:read"),
    SERVICE_EXCHANGE_RATE_WRITE("service:exchange-rate:write"),
    SERVICE_CUSTOMER_READ("service:customer:read"),
    SERVICE_CUSTOMER_WRITE("service:customer:write"),
    SERVICE_SETUP_READ("service:setup:read"),
    SERVICE_SETUP_WRITE("service:setup:write"),
    SERVICE_ACCOUNT_READ("service:account:read"),
    SERVICE_ACCOUNT_WRITE("service:account:write"),
    SERVICE_TRANSACTION_READ("service:transaction:read"),
    SERVICE_TRANSACTION_WRITE("service:transaction:write"),
    SERVICE_GL_READ("service:gl:read"),
    SERVICE_GL_WRITE("service:gl:write"),
    SERVICE_LOAN_READ("service:loan:read"),
    SERVICE_LOAN_WRITE("service:loan:write"),

    // ── Administration ─────────────────────────────────────────────────────────
    ADMIN_USERS_READ("admin:users:read"),
    ADMIN_USERS_WRITE("admin:users:write"),
    ADMIN_ROLES_READ("admin:roles:read"),
    ADMIN_ROLES_WRITE("admin:roles:write"),
    ADMIN_CONFIG_READ("admin:config:read"),
    ADMIN_CONFIG_WRITE("admin:config:write"),

    /** View delegations of authority and cross-domain approval workflows. */
    ADMIN_DOA_READ("admin:doa:read"),
    /** Create, revoke, or drive identity approval workflows for DoA. */
    ADMIN_DOA_WRITE("admin:doa:write");

    private final String authority;

    BankingPermission(String authority) {
        this.authority = authority;
    }

    /** Authority string used in @PreAuthorize and as a JWT claim value. */
    public String getAuthority() {
        return authority;
    }

    @Override
    public String toString() {
        return authority;
    }
}
