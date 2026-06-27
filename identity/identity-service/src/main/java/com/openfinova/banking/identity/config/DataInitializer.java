package com.openfinova.banking.identity.config;

import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.api.permission.BankingPermission;
import com.openfinova.banking.identity.entity.BankingRole;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.repository.RoleRepository;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.service.KeycloakUserProvisioningService;

/**
 * Seeds the database with the default role catalogue and a dev admin user on startup. Safe to run
 * multiple times -- skips any role/user that already exists.
 *
 * <p>On each run, the ADMIN role persisted permissions are merged with
 * {@code EnumSet.allOf(BankingPermission.class)} so new enum constants automatically apply without
 * using the guarded role-management APIs.
 *
 * Override or disable in production via {@code @Profile} or by replacing this bean.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /**
     * Dev-only default password for the seeded {@code admin} user. Must satisfy the Keycloak realm
     * password policy (length, complexity, notUsername) configured in {@code openfinova.yaml}.
     */
    private static final String DEV_ADMIN_PASSWORD = "Openfinova123!";

    /** Merged into every seeded role so all users can use identity self-service endpoints. */
    private static final Set<BankingPermission> IDENTITY_SELF_SERVICE = EnumSet.of(
            BankingPermission.PROFILE_READ_OWN,
            BankingPermission.PASSWORD_CHANGE_OWN,
            BankingPermission.MFA_MANAGE_OWN,
            BankingPermission.AUDIT_READ_OWN);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakUserProvisioningService keycloakProvisioning;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder, KeycloakUserProvisioningService keycloakProvisioning) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.keycloakProvisioning = keycloakProvisioning;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRoles();
        reconcileAdminPermissionsWithEnum();
        applyRoleHierarchy();
        seedDefaultUsers();
    }

    private void seedRoles() {

        createSystemRole(
                "ADMIN",
                "System Administrator",
                "Full access to all system functions.",
                EnumSet.allOf(BankingPermission.class));

        createSystemRole(
                "COMPLIANCE",
                "Compliance Officer",
                "Cross-module read access including PII for regulatory compliance.",
                EnumSet.of(
                        BankingPermission.CUSTOMER_READ,
                        BankingPermission.CUSTOMER_PII_READ,
                        BankingPermission.LOAN_READ,
                        BankingPermission.ACCOUNT_READ,
                        BankingPermission.GL_READ,
                        BankingPermission.AUDIT_READ,
                        BankingPermission.REPORT_READ,
                        BankingPermission.REPORT_GENERATE,
                        BankingPermission.ADMIN_USERS_READ,
                        BankingPermission.ADMIN_ROLES_READ,
                        BankingPermission.ADMIN_DOA_READ,
                        BankingPermission.COMPLIANCE_SCREENING_RUN,
                        BankingPermission.COMPLIANCE_SCREENING_READ,
                        BankingPermission.COMPLIANCE_ALERT_READ,
                        BankingPermission.COMPLIANCE_ALERT_TRIAGE,
                        BankingPermission.OPERATOR_NOTE_READ,
                        BankingPermission.OPERATOR_NOTE_WRITE,
                        BankingPermission.STAFF_NOTIFICATION_READ,
                        BankingPermission.STAFF_NOTIFICATION_WRITE,
                        BankingPermission.RECONCILIATION_READ,
                        BankingPermission.RECONCILIATION_WRITE,
                        BankingPermission.FEE_CAMPAIGN_WRITE));

        createSystemRole(
                "AUDITOR",
                "Auditor",
                "Read-only cross-module access for internal and external auditors.",
                EnumSet.of(
                        BankingPermission.CUSTOMER_READ,
                        BankingPermission.CUSTOMER_PII_READ,
                        BankingPermission.LOAN_READ,
                        BankingPermission.ACCOUNT_READ,
                        BankingPermission.GL_READ,
                        BankingPermission.EXCHANGE_RATE_READ,
                        BankingPermission.SERVICE_EXCHANGE_RATE_READ,
                        BankingPermission.AUDIT_READ,
                        BankingPermission.REPORT_READ,
                        BankingPermission.REPORT_GENERATE,
                        BankingPermission.ADMIN_USERS_READ,
                        BankingPermission.ADMIN_ROLES_READ,
                        BankingPermission.ADMIN_CONFIG_READ,
                        BankingPermission.ADMIN_DOA_READ));

        createSystemRole(
                "LOAN_OFFICER",
                "Loan Officer",
                "Originates and manages loan applications.",
                EnumSet.of(
                        BankingPermission.CUSTOMER_READ,
                        BankingPermission.LOAN_READ,
                        BankingPermission.LOAN_WRITE,
                        BankingPermission.ACCOUNT_READ,
                        BankingPermission.EXCHANGE_RATE_READ,
                        BankingPermission.SERVICE_EXCHANGE_RATE_READ));

        createSystemRole(
                "LOAN_SUPERVISOR",
                "Loan Supervisor",
                "Reviews and approves loan applications within their delegation of authority.",
                EnumSet.of(
                        BankingPermission.CUSTOMER_READ,
                        BankingPermission.CUSTOMER_PII_READ,
                        BankingPermission.LOAN_READ,
                        BankingPermission.LOAN_WRITE,
                        BankingPermission.LOAN_APPROVE,
                        BankingPermission.LOAN_DISBURSE_APPROVE,
                        BankingPermission.LOAN_COLLECT_APPROVE,
                        BankingPermission.LOAN_RESTRUCTURE_APPROVE,
                        BankingPermission.ACCOUNT_READ,
                        BankingPermission.EXCHANGE_RATE_READ,
                        BankingPermission.SERVICE_EXCHANGE_RATE_READ));

        createSystemRole(
                "LOAN_CREDIT_SENIOR",
                "Senior Credit Officer",
                "Approves, restructures, and writes off loans within senior delegation limits.",
                EnumSet.of(
                        BankingPermission.CUSTOMER_READ,
                        BankingPermission.CUSTOMER_PII_READ,
                        BankingPermission.LOAN_READ,
                        BankingPermission.LOAN_WRITE,
                        BankingPermission.LOAN_APPROVE,
                        BankingPermission.LOAN_RESTRUCTURE,
                        BankingPermission.LOAN_RESTRUCTURE_APPROVE,
                        BankingPermission.LOAN_WRITE_OFF,
                        BankingPermission.LOAN_DISBURSE_APPROVE,
                        BankingPermission.LOAN_COLLECT_APPROVE,
                        BankingPermission.ACCOUNT_READ,
                        BankingPermission.EXCHANGE_RATE_READ,
                        BankingPermission.SERVICE_EXCHANGE_RATE_READ));

        createSystemRole(
                "LOAN_OPERATIONS",
                "Loan Operations",
                "Handles disbursement, collection, and operational loan processing.",
                EnumSet.of(
                        BankingPermission.CUSTOMER_READ,
                        BankingPermission.LOAN_READ,
                        BankingPermission.LOAN_WRITE,
                        BankingPermission.LOAN_DISBURSE,
                        BankingPermission.LOAN_COLLECT,
                        BankingPermission.ACCOUNT_READ,
                        BankingPermission.PAYMENT_INITIATE,
                        BankingPermission.EXCHANGE_RATE_READ,
                        BankingPermission.SERVICE_EXCHANGE_RATE_READ));

        createSystemRole(
                "GL_ACCOUNTANT",
                "GL Accountant",
                "Posts journal entries to the general ledger.",
                EnumSet.of(
                        BankingPermission.GL_READ,
                        BankingPermission.GL_POST,
                        BankingPermission.EXCHANGE_RATE_READ,
                        BankingPermission.SERVICE_EXCHANGE_RATE_READ));

        createSystemRole(
                "GL_MANAGER",
                "GL Manager",
                "Approves journal entries and manages the chart of accounts.",
                EnumSet.of(
                        BankingPermission.GL_READ,
                        BankingPermission.GL_POST,
                        BankingPermission.GL_APPROVE,
                        BankingPermission.EXCHANGE_RATE_READ,
                        BankingPermission.SERVICE_EXCHANGE_RATE_READ));

        createSystemRole(
                "BRANCH_MANAGER",
                "Branch Manager",
                "Branch-level oversight of customer, account, and loan operations.",
                EnumSet.of(
                        BankingPermission.CUSTOMER_READ,
                        BankingPermission.CUSTOMER_WRITE,
                        BankingPermission.CUSTOMER_PII_READ,
                        BankingPermission.ACCOUNT_READ,
                        BankingPermission.ACCOUNT_WRITE,
                        BankingPermission.LOAN_READ,
                        BankingPermission.PAYMENT_INITIATE,
                        BankingPermission.EXCHANGE_RATE_READ,
                        BankingPermission.SERVICE_EXCHANGE_RATE_READ,
                        BankingPermission.REPORT_READ));

        createSystemRole(
                "CUSTOMER_SERVICE_REP",
                "Customer Service Representative",
                "Front-desk or call-center agent handling customer inquiries and basic account operations.",
                EnumSet.of(
                        BankingPermission.CUSTOMER_READ,
                        BankingPermission.CUSTOMER_WRITE,
                        BankingPermission.ACCOUNT_READ));

        createSystemRole(
                "TELLER",
                "Teller",
                "Processes over-the-counter transactions and payments.",
                EnumSet.of(
                        BankingPermission.CUSTOMER_READ,
                        BankingPermission.ACCOUNT_READ,
                        BankingPermission.ACCOUNT_TRANSFER,
                        BankingPermission.PAYMENT_INITIATE,
                        BankingPermission.EXCHANGE_RATE_READ,
                        BankingPermission.SERVICE_EXCHANGE_RATE_READ));

        createSystemRole(
                "TREASURY",
                "Treasury",
                "Manages treasury operations including FX, liquidity, and interbank transfers.",
                EnumSet.of(
                        BankingPermission.ACCOUNT_READ,
                        BankingPermission.ACCOUNT_TRANSFER,
                        BankingPermission.PAYMENT_INITIATE,
                        BankingPermission.EXCHANGE_RATE_READ,
                        BankingPermission.EXCHANGE_RATE_WRITE,
                        BankingPermission.SERVICE_EXCHANGE_RATE_READ,
                        BankingPermission.SERVICE_EXCHANGE_RATE_WRITE,
                        BankingPermission.GL_READ));

        createSystemRole(
                "CUSTOMER",
                "Customer (self-service)",
                "End-customer portal and mobile banking access limited to own resources.",
                EnumSet.of(
                        BankingPermission.LOAN_READ_OWN,
                        BankingPermission.ACCOUNT_READ_OWN,
                        BankingPermission.ACCOUNT_TRANSFER_OWN,
                        BankingPermission.PAYMENT_INITIATE_OWN,
                        BankingPermission.CUSTOMER_READ_OWN,
                        BankingPermission.CUSTOMER_WRITE_OWN,
                        BankingPermission.TAN_GENERATE));

        reconcileCustomerTanPermission();
        log.info("Identity role catalogue seeded.");
    }

    /**
     * ADMIN is defined as holding every {@link BankingPermission}. Existing deployments keep a
     * snapshot in {@code identity_role_permissions}; merge in any new enum values on startup.
     */
    /** Ensures existing CUSTOMER deployments receive {@link BankingPermission#TAN_GENERATE}. */
    private void reconcileCustomerTanPermission() {
        roleRepository.findByName("CUSTOMER").ifPresentOrElse(customer -> {
            if (!customer.getPermissions().contains(BankingPermission.TAN_GENERATE)) {
                customer.getPermissions().add(BankingPermission.TAN_GENERATE);
                roleRepository.save(customer);
                log.info("Added tan:generate to CUSTOMER role.");
            }
        }, () -> log.warn("CUSTOMER role not present; skipping tan:generate reconcile."));
    }

    private void reconcileAdminPermissionsWithEnum() {
        roleRepository.findByName("ADMIN").ifPresentOrElse(admin -> {
            EnumSet<BankingPermission> fullCatalog = EnumSet.allOf(BankingPermission.class);
            if (admin.getPermissions().containsAll(fullCatalog)) {
                return;
            }
            EnumSet<BankingPermission> merged = EnumSet.copyOf(admin.getPermissions());
            merged.addAll(fullCatalog);
            admin.setPermissions(merged);
            roleRepository.save(admin);
            log.info("ADMIN permissions reconciled with BankingPermission enum ({} authorities).", merged.size());
        }, () -> log.warn("ADMIN role not present; skipping permission catalogue reconcile."));
    }

    /**
     * Links {@link BankingRole#getParentRole()} so non-{@code ADMIN} assigners can only grant roles
     * at or below their position (see
     * {@link com.openfinova.banking.identity.service.RoleAssignmentValidationService}).
     */
    private void applyRoleHierarchy() {
        linkChildToParent("GL_ACCOUNTANT", "GL_MANAGER");
        linkChildToParent("GL_MANAGER", "ADMIN");
        linkChildToParent("LOAN_OPERATIONS", "LOAN_OFFICER");
        linkChildToParent("LOAN_OFFICER", "LOAN_SUPERVISOR");
        linkChildToParent("LOAN_SUPERVISOR", "LOAN_CREDIT_SENIOR");
        linkChildToParent("LOAN_CREDIT_SENIOR", "ADMIN");
        linkChildToParent("CUSTOMER_SERVICE_REP", "BRANCH_MANAGER");
        linkChildToParent("TELLER", "BRANCH_MANAGER");
        linkChildToParent("BRANCH_MANAGER", "ADMIN");
        linkChildToParent("COMPLIANCE", "ADMIN");
        linkChildToParent("AUDITOR", "ADMIN");
        linkChildToParent("TREASURY", "ADMIN");
        linkChildToParent("CUSTOMER", "ADMIN");
        log.debug("Identity role parent links applied.");
    }

    private void linkChildToParent(String childName, String parentName) {
        roleRepository.findByName(childName)
                .ifPresent(child -> roleRepository.findByName(parentName).ifPresent(parent -> {
                    child.setParentRole(parent);
                    roleRepository.save(child);
                }));
    }

    private BankingRole createSystemRole(String name, String displayName, String description,
            Set<BankingPermission> permissions) {
        EnumSet<BankingPermission> merged = EnumSet.copyOf(permissions);
        merged.addAll(IDENTITY_SELF_SERVICE);
        return roleRepository.findByName(name).orElseGet(() -> {
            BankingRole role = new BankingRole(name, displayName);
            role.setDescription(description);
            role.setSystemRole(true);
            role.setPermissions(merged);
            BankingRole saved = roleRepository.save(role);
            log.info("Created system role: {}", name);
            return saved;
        });
    }

    private void syncKeycloakUser(BankingUser user) {
        ensureDevAdminGlProfile(user);
        keycloakProvisioning.ensureUser(user, DEV_ADMIN_PASSWORD, false);
        if (user.isMfaEnabled() && user.getMfaSecret() != null && !user.getMfaSecret().isBlank()) {
            try {
                keycloakProvisioning.syncTotpCredential(user.getUsername(), user.getMfaSecret());
            } catch (KeycloakUserProvisioningService.KeycloakProvisioningException ex) {
                log.warn(
                        "Failed to sync TOTP to Keycloak for {}; login MFA may be unavailable until re-enrolled",
                        user.getUsername(),
                        ex);
            }
        }
    }

    /**
     * Dev admin must carry a GL approval role so the approvals API can resolve limits and queue rows.
     * Production admins are assigned explicitly via user management.
     */
    private void ensureDevAdminGlProfile(BankingUser user) {
        if (!"admin".equals(user.getUsername())) {
            return;
        }
        if (user.getGlApprovalRole() == null || user.getGlApprovalRole().isBlank()) {
            user.setGlApprovalRole("CFO");
            userRepository.save(user);
            log.info("Set dev admin gl_approval_role=CFO for GL approvals console");
        }
    }

    private void seedDefaultUsers() {
        if (userRepository.existsByUsername("admin")) {
            log.info("Default admin user already exists; ensuring Keycloak mirror is present.");
            userRepository.findByUsername("admin").ifPresent(this::syncKeycloakUser);
            return;
        }

        BankingRole adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found after seed"));

        BankingUser admin = new BankingUser("admin", passwordEncoder.encode(DEV_ADMIN_PASSWORD), UserType.STAFF);
        admin.setEmail("admin@openfinova.local");
        admin.setEmployeeId("EMP-0001");
        admin.setGlApprovalRole("CFO");
        admin.setRoles(Set.of(adminRole));
        BankingUser saved = userRepository.save(admin);

        // Keycloak is the credential authority: provision the dev admin there with banking_user_id
        // set to the generated banking id, so the JWT sub resolves back to this account.
        keycloakProvisioning.ensureUser(saved, DEV_ADMIN_PASSWORD, false);

        log.warn(
                "Created default admin user (username=admin, password={}). "
                        + "Change this before production deployment.",
                DEV_ADMIN_PASSWORD);
    }
}
