package com.openfinova.banking.identity.api.permission;

/**
 * Fine-grained permission catalogue for the entire banking platform.
 *
 * Permissions follow the pattern {@code domain:action} and are stored as authorities in the JWT under
 * a custom {@code permissions} claim. Use {@code hasAuthority('loan:read')} (or the constant) in
 * {@code @PreAuthorize} expressions.
 *
 * <p>Roles (LOAN_OFFICER, GL_MANAGER, ...) are coarse role bundles; each role owns a set of these
 * permissions which are then flattened into the token so endpoint guards never need to know role names.
 */
public enum BankingPermission {

    // ── Loan ───────────────────────────────────────────────────────────────────
    LOAN_READ("loan:read", "View loan accounts and applications."),
    LOAN_WRITE("loan:write", "Create and modify loans (non-approval steps)."),
    LOAN_APPROVE("loan:approve", "Approve or reject loan applications within delegated limits."),
    LOAN_DISBURSE("loan:disburse", "Initiate loan disbursements."),
    LOAN_DISBURSE_APPROVE("loan:disburse:approve", "Approve disbursements after initiation (maker-checker)."),
    LOAN_RESTRUCTURE("loan:restructure", "Initiate loan restructuring workflows."),
    LOAN_RESTRUCTURE_APPROVE("loan:restructure:approve", "Approve or reject restructuring requests."),
    LOAN_COLLECT("loan:collect", "Record collections and repayments."),
    LOAN_COLLECT_APPROVE("loan:collect:approve", "Approve collection outcomes such as settlements or waivers."),
    LOAN_WRITE_OFF("loan:write-off", "Write off loan balances subject to policy."),
    LOAN_READ_OWN("loan:read:own", "Customer self-service: view own loans only."),

    // ── Account ────────────────────────────────────────────────────────────────
    ACCOUNT_READ("account:read", "View customer deposit accounts."),
    ACCOUNT_WRITE("account:write", "Open, update, or close customer accounts."),
    ACCOUNT_TRANSFER("account:transfer", "Post transfers between accounts (staff-initiated)."),
    ACCOUNT_READ_OWN("account:read:own", "Customer self-service: view own accounts."),
    ACCOUNT_TRANSFER_OWN("account:transfer:own", "Customer self-service: transfer between own accounts."),

    // ── Customer ───────────────────────────────────────────────────────────────
    CUSTOMER_READ("customer:read", "View customer profiles (non-PII fields per channel rules)."),
    CUSTOMER_WRITE("customer:write", "Create or update customer master data."),
    CUSTOMER_PII_READ("customer:pii:read", "View restricted personally identifiable information."),
    CUSTOMER_READ_OWN("customer:read:own", "Customer self-service: view own profile."),
    CUSTOMER_WRITE_OWN("customer:write:own", "Customer self-service: update own profile."),

    // ── Self-service (identity / portal) ───────────────────────────────────────
    PROFILE_READ_OWN("profile:read:own", "View own roles and identity claims."),
    PASSWORD_CHANGE_OWN("password:change:own", "Change own password via self-service."),
    MFA_MANAGE_OWN("mfa:manage:own", "Enrol, verify, or disable own MFA."),
    AUDIT_READ_OWN("audit:read:own", "View own security audit history."),

    // ── General Ledger ─────────────────────────────────────────────────────────
    GL_READ("gl:read", "View chart of accounts, balances, and journal enquiries."),
    GL_POST("gl:post", "Create and post journal entries."),
    GL_APPROVE("gl:approve", "Approve GL postings and sensitive ledger actions."),

    // ── Payments ───────────────────────────────────────────────────────────────
    PAYMENT_INITIATE("payment:initiate", "Initiate outbound or internal payments."),
    PAYMENT_INITIATE_OWN("payment:initiate:own", "Customer self-service: initiate own payments."),

    // ── Exchange Rate ──────────────────────────────────────────────────────────
    EXCHANGE_RATE_READ("exchange-rate:read", "View FX and treasury rates."),
    EXCHANGE_RATE_WRITE("exchange-rate:write", "Maintain exchange rates."),

    // ── Audit & Reporting ──────────────────────────────────────────────────────
    AUDIT_READ("audit:read", "Search and review organisation-wide security audit events."),
    REPORT_READ("report:read", "Run read-only operational and regulatory reports."),
    REPORT_GENERATE("report:generate", "Generate or export reports."),

    // ── Transaction Processing ─────────────────────────────────────────────────
    TRANSACTION_READ("transaction:read", "View core banking transactions."),
    TRANSACTION_WRITE("transaction:write", "Create or reverse transactions."),
    VELOCITY_LIMIT_READ("velocity-limit:read", "View velocity and limits configuration."),
    VELOCITY_LIMIT_WRITE("velocity-limit:write", "Maintain velocity limits."),
    FEE_READ("fee:read", "View fee rules and assessments."),
    FEE_WRITE("fee:write", "Maintain fee rules."),
    COMPENSATION_READ("compensation:read", "View compensation and reversal workflows."),
    COMPENSATION_WRITE("compensation:write", "Operate compensation workflows."),

    // ── Enhanced General Ledger ────────────────────────────────────────────────
    GL_SUSPENSE_READ("gl:suspense:read", "View suspense account queues."),
    GL_SUSPENSE_WRITE("gl:suspense:write", "Manage suspense postings."),
    GL_REVALUATION_READ("gl:revaluation:read", "View FX revaluation runs."),
    GL_REVALUATION_WRITE("gl:revaluation:write", "Run or approve revaluation."),
    GL_FISCAL_PERIOD_READ("gl:fiscal-period:read", "View fiscal periods."),
    GL_FISCAL_PERIOD_WRITE("gl:fiscal-period:write", "Open, close, or adjust fiscal periods."),
    GL_SETUP_READ("gl:setup:read", "View GL setup (segments, mappings)."),
    GL_SETUP_WRITE("gl:setup:write", "Maintain GL setup."),

    // ── Setup Service ──────────────────────────────────────────────────────────
    BANK_CONFIG_READ("bank:config:read", "View bank-wide configuration."),
    BANK_CONFIG_WRITE("bank:config:write", "Change bank-wide configuration."),
    HOLIDAY_READ("holiday:read", "View holiday calendars."),
    HOLIDAY_WRITE("holiday:write", "Maintain holiday calendars."),

    // ── Customer Account Management ────────────────────────────────────────────
    CUSTOMER_ACCOUNT_READ("customer-account:read", "View customer account servicing details."),
    CUSTOMER_ACCOUNT_WRITE("customer-account:write", "Maintain customer accounts (limits, status)."),
    CUSTOMER_ACCOUNT_LIMIT_READ("customer-account:limit:read", "View account limits."),
    CUSTOMER_ACCOUNT_LIMIT_WRITE("customer-account:limit:write", "Maintain account limits."),

    // ── Inter-Service Communication ────────────────────────────────────────────
    SERVICE_EXCHANGE_RATE_READ("service:exchange-rate:read", "Machine/service account: read exchange rates."),
    SERVICE_EXCHANGE_RATE_WRITE("service:exchange-rate:write", "Machine/service account: publish exchange rates."),
    SERVICE_CUSTOMER_READ("service:customer:read", "Machine/service account: read customer data."),
    SERVICE_CUSTOMER_WRITE("service:customer:write", "Machine/service account: update customer data."),
    SERVICE_SETUP_READ("service:setup:read", "Machine/service account: read setup/reference data."),
    SERVICE_SETUP_WRITE("service:setup:write", "Machine/service account: update setup data."),
    SERVICE_ACCOUNT_READ("service:account:read", "Machine/service account: read accounts."),
    SERVICE_ACCOUNT_WRITE("service:account:write", "Machine/service account: update accounts."),
    SERVICE_TRANSACTION_READ("service:transaction:read", "Machine/service account: read transactions."),
    SERVICE_TRANSACTION_WRITE("service:transaction:write", "Machine/service account: post transactions."),
    SERVICE_GL_READ("service:gl:read", "Machine/service account: read ledger."),
    SERVICE_GL_WRITE("service:gl:write", "Machine/service account: post to ledger."),
    SERVICE_LOAN_READ("service:loan:read", "Machine/service account: read loans."),
    SERVICE_LOAN_WRITE("service:loan:write", "Machine/service account: update loans."),

    // ── Administration ─────────────────────────────────────────────────────────
    ADMIN_USERS_READ("admin:users:read", "View identity users and provisioning."),
    ADMIN_USERS_WRITE("admin:users:write", "Create, lock, suspend, or deprovision users."),
    ADMIN_ROLES_READ("admin:roles:read", "View roles and permission catalogue."),
    ADMIN_ROLES_WRITE("admin:roles:write", "Create roles and change permission assignments."),
    ADMIN_CONFIG_READ("admin:config:read", "View administrative configuration."),
    ADMIN_CONFIG_WRITE("admin:config:write", "Change administrative configuration."),
    ADMIN_DOA_READ("admin:doa:read", "View delegations of authority and approval workflows."),
    ADMIN_DOA_WRITE("admin:doa:write", "Manage delegations and drive approval workflows."),

    // ── Compliance / operator workspace ────────────────────────────────────────
    COMPLIANCE_SCREENING_RUN("compliance:screening:run", "Trigger AML/sanctions screening."),
    COMPLIANCE_SCREENING_READ("compliance:screening:read", "View screening results."),
    COMPLIANCE_ALERT_READ("compliance:alert:read", "View compliance alerts."),
    COMPLIANCE_ALERT_TRIAGE("compliance:alert:triage", "Triage or disposition compliance alerts."),
    OPERATOR_NOTE_READ("operator:note:read", "View operator notes on cases."),
    OPERATOR_NOTE_WRITE("operator:note:write", "Add or amend operator notes."),
    STAFF_NOTIFICATION_READ("notification:read", "View internal staff notifications."),
    STAFF_NOTIFICATION_WRITE("notification:write", "Publish staff notifications."),
    RECONCILIATION_READ("reconciliation:read", "View reconciliation tasks."),
    RECONCILIATION_WRITE("reconciliation:write", "Perform reconciliation."),
    FEE_CAMPAIGN_WRITE("fee:campaign:write", "Manage fee waiver campaigns.");

    private final String authority;
    private final String catalogDescription;

    BankingPermission(String authority, String catalogDescription) {
        this.authority = authority;
        this.catalogDescription = catalogDescription;
    }

    /** Authority string used in {@code @PreAuthorize} and as a JWT claim value. */
    public String getAuthority() {
        return authority;
    }

    /** Plain-language explanation for admin catalogue and API listings. */
    public String getCatalogDescription() {
        return catalogDescription;
    }

    @Override
    public String toString() {
        return authority;
    }
}
