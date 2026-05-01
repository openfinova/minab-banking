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

import com.openfinova.banking.identity.api.permission.BankingPermission;
import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.entity.BankingRole;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.repository.RoleRepository;
import com.openfinova.banking.identity.repository.UserRepository;

/**
 * Seeds the database with the default role catalogue and a dev admin user on startup. Safe to run
 * multiple times -- skips any role/user that already exists.
 *
 * Override or disable in production via {@code @Profile} or by replacing this bean.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Merged into every seeded role so all users can use identity self-service endpoints. */
    private static final Set<BankingPermission> IDENTITY_SELF_SERVICE = EnumSet.of(
            BankingPermission.PROFILE_READ_OWN,
            BankingPermission.PASSWORD_CHANGE_OWN,
            BankingPermission.MFA_MANAGE_OWN,
            BankingPermission.AUDIT_READ_OWN);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRoles();
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
                        BankingPermission.ADMIN_DOA_READ));

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
                        BankingPermission.EXCHANGE_RATE_READ));

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
                        BankingPermission.EXCHANGE_RATE_READ));

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
                        BankingPermission.EXCHANGE_RATE_READ));

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
                        BankingPermission.EXCHANGE_RATE_READ));

        createSystemRole(
                "GL_ACCOUNTANT",
                "GL Accountant",
                "Posts journal entries to the general ledger.",
                EnumSet.of(BankingPermission.GL_READ, BankingPermission.GL_POST, BankingPermission.EXCHANGE_RATE_READ));

        createSystemRole(
                "GL_MANAGER",
                "GL Manager",
                "Approves journal entries and manages the chart of accounts.",
                EnumSet.of(
                        BankingPermission.GL_READ,
                        BankingPermission.GL_POST,
                        BankingPermission.GL_APPROVE,
                        BankingPermission.EXCHANGE_RATE_READ));

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
                        BankingPermission.EXCHANGE_RATE_READ));

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
                        BankingPermission.CUSTOMER_WRITE_OWN));

        log.info("Identity role catalogue seeded.");
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

    private void seedDefaultUsers() {
        if (userRepository.existsByUsername("admin")) {
            return;
        }

        BankingRole adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found after seed"));

        BankingUser admin = new BankingUser("admin", passwordEncoder.encode("admin"), UserType.STAFF);
        admin.setEmail("admin@openfinova.local");
        admin.setEmployeeId("EMP-0001");
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);
        log.warn(
                "Created default admin user (username=admin, password=admin). "
                        + "Change this before production deployment.");
    }
}
