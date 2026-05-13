package com.openfinova.banking.identity.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.customer.api.CustomerInfoService;
import com.openfinova.banking.identity.api.audit.AuditActor;
import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.audit.AuditEventDetail;
import com.openfinova.banking.identity.audit.SecurityAuditExtensions;
import com.openfinova.banking.identity.config.AccountLifecycleProperties;
import com.openfinova.banking.identity.config.WorkflowEnforcementProperties;
import com.openfinova.banking.identity.dto.CreateUserRequest;
import com.openfinova.banking.identity.dto.UpdateUserAccessRequest;
import com.openfinova.banking.identity.dto.UserResponse;
import com.openfinova.banking.identity.dto.UserSearchCriteria;
import com.openfinova.banking.identity.entity.AccountProvisioningStatus;
import com.openfinova.banking.identity.entity.BankingRole;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.event.UserAccountDeprovisionedEvent;
import com.openfinova.banking.identity.exception.PasswordPolicyViolationException;
import com.openfinova.banking.identity.repository.ApprovalWorkflowInstanceRepository;
import com.openfinova.banking.identity.repository.RoleRepository;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.validation.GlApprovalRoleValidation;

import jakarta.persistence.criteria.Predicate;

/**
 * Central service for the full lifecycle of banking user accounts.
 *
 * Covers account creation, profile and access-field updates, role assignment with optional
 * maker-checker workflow enforcement, provisioning approval and rejection, account
 * enable/disable, administrative and temporary locking, suspension and reactivation,
 * full deprovisioning with credential scrubbing, and all password operations including
 * admin reset, forced expiry, and self-service change.
 *
 * Every mutating operation records a corresponding security audit event through
 * SecurityAuditService. Cross-module integration with the customer service is performed
 * through an optional ObjectProvider-injected CustomerInfoService, so the identity module
 * can operate independently when the customer module is not present.
 */
@Service
public class UserManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserManagementService.class);

    private static final String OPAQUE_NOT_FOUND = "The requested resource was not found.";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final SecurityAuditService auditService;
    private final RoleAssignmentValidationService roleAssignmentValidationService;
    private final AccountLifecycleProperties lifecycleProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final ApprovalWorkflowInstanceRepository workflowRepository;
    private final WorkflowEnforcementProperties enforcementProperties;
    private final ObjectProvider<CustomerInfoService> customerInfoServiceProvider;

    /** Resource type used when creating an approval workflow for a user role assignment. */
    public static final String RESOURCE_TYPE_USER_ROLE_ASSIGNMENT = "USER_ROLE_ASSIGNMENT";

    public UserManagementService(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder, PasswordPolicyService passwordPolicyService,
            SecurityAuditService auditService, RoleAssignmentValidationService roleAssignmentValidationService,
            AccountLifecycleProperties lifecycleProperties, ApplicationEventPublisher eventPublisher,
            ApprovalWorkflowInstanceRepository workflowRepository, WorkflowEnforcementProperties enforcementProperties,
            ObjectProvider<CustomerInfoService> customerInfoServiceProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.auditService = auditService;
        this.roleAssignmentValidationService = roleAssignmentValidationService;
        this.lifecycleProperties = lifecycleProperties;
        this.eventPublisher = eventPublisher;
        this.workflowRepository = workflowRepository;
        this.enforcementProperties = enforcementProperties;
        this.customerInfoServiceProvider = customerInfoServiceProvider;
    }

    /**
     * Creates a new banking user account from the supplied request.
     *
     * The following steps are executed in order: username uniqueness check, GL approval role
     * validation, customer party existence and activity check when a party ID is provided,
     * password complexity validation, role resolution and role-assignment policy validation,
     * and finally account persistence. When requireProvisioningApproval is enabled the account
     * is stored with PENDING_APPROVAL status and left disabled until an administrator approves
     * it via approveProvisioning. On success a USER_CREATED audit event is recorded and, for
     * CUSTOMER-type accounts, the identity link on the customer party record is updated through
     * CustomerInfoService if the service is available.
     *
     * @param request  the creation payload containing username, raw password, user type, roles,
     *                 optional email, branch code, employee ID, GL approval role, customer party
     *                 ID, account expiry, and provisioning eligibility notes
     * @param auditor  the authenticated actor initiating the operation, used for audit recording
     *                 and role-assignment policy checks
     * @return the newly persisted BankingUser entity
     * @throws IllegalArgumentException if the username is already taken, the password violates
     *                                  complexity rules, or the referenced customer party is not
     *                                  found or is not active
     */
    @Transactional
    public BankingUser createUser(CreateUserRequest request, AuditActor auditor) {
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("User create rejected: username already in use (username={})", request.getUsername());
            throw new IllegalArgumentException(
                    "Unable to create the user account. Check the submitted data and try again.");
        }

        GlApprovalRoleValidation.requireValidOrNull(request.getGlApprovalRole());

        // Validate that the referenced customer party exists and is active before creating the account
        validateCustomerPartyId(request.getCustomerPartyId());

        passwordPolicyService.validate(request.getPassword());

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        BankingUser user = new BankingUser(request.getUsername(), encodedPassword, request.getUserType());
        user.setEmail(request.getEmail());
        user.setBranchCode(request.getBranchCode());
        user.setEmployeeId(request.getEmployeeId());
        user.setGlApprovalRole(request.getGlApprovalRole());
        user.setCustomerPartyId(request.getCustomerPartyId());

        Set<BankingRole> initialRoles = resolveRoles(request.getRoleNames());
        roleAssignmentValidationService.validate(auditor, user, initialRoles);
        user.setRoles(initialRoles);

        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordExpiresAt(LocalDateTime.now().plusDays(passwordPolicyService.getMaxAgeDays()));
        user.getPasswordHistory().add(encodedPassword);

        user.setProvisioningEligibilityNotes(request.getProvisioningEligibilityNotes());
        user.setCreatedBy(auditor.userId());

        if (lifecycleProperties.isRequireProvisioningApproval()) {
            user.setProvisioningStatus(AccountProvisioningStatus.PENDING_APPROVAL);
            user.setEnabled(false);
        } else {
            user.setProvisioningStatus(AccountProvisioningStatus.ACTIVE);
        }

        if (request.getAccountExpiresAt() != null) {
            user.setAccountExpiresAt(request.getAccountExpiresAt());
            user.setAccountExpiryWarningNotifiedAt(null);
        }

        BankingUser saved = userRepository.save(user);

        String createDetails = lifecycleProperties.isRequireProvisioningApproval()
                ? "User created (pending provisioning approval); roles: " + request.getRoleNames()
                : "User created with roles: " + request.getRoleNames();

        auditService.recordParticipating(
                SecurityAuditEventType.USER_CREATED,
                saved.getId(),
                saved.getUsername(),
                createDetails,
                auditor,
                SecurityAuditExtensions.changeTracking(null, null, null, String.valueOf(request.getRoleNames())),
                AuditEventDetail.userCreated(
                        request.getUserType() != null ? request.getUserType().name() : null,
                        request.getRoleNames() != null ? new ArrayList<>(request.getRoleNames())
                                : Collections.emptyList(),
                        lifecycleProperties.isRequireProvisioningApproval()));
        recordAccountExpirySet(
                saved.getId(),
                saved.getUsername(),
                auditor,
                null,
                saved.getPasswordExpiresAt(),
                "Initial password expiry policy applied");

        // Record the newly created identity account as linked to the customer party in customer-service
        if (saved.getUserType() == UserType.CUSTOMER && saved.getCustomerPartyId() != null) {
            CustomerInfoService customerInfoService = customerInfoServiceProvider.getIfAvailable();
            if (customerInfoService != null) {
                customerInfoService.linkIdentityUser(saved.getCustomerPartyId(), saved.getId(), saved.getUsername());
            }
        }

        return saved;
    }

    /**
     * Returns a paginated slice of all user accounts with no filtering applied.
     *
     * Intended for administrative overviews. For filtered queries use searchUsers instead.
     *
     * @param pageable  pagination and sorting parameters
     * @return a page of BankingUser entities
     */
    @Transactional(readOnly = true)
    public Page<BankingUser> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Retrieves a single user account by its internal UUID.
     *
     * @param id  the unique identifier of the user to look up
     * @return the matching BankingUser entity
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional(readOnly = true)
    public BankingUser getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id, OPAQUE_NOT_FOUND));
    }

    /**
     * Looks up the user account linked to the given customer party ID.
     *
     * Each active customer party may be linked to at most one identity user. This method is
     * typically called when the customer module needs to resolve the identity side of a customer
     * relationship.
     *
     * @param customerPartyId  the UUID of the customer party whose linked user is requested
     * @return the BankingUser linked to the specified customer party
     * @throws ResourceNotFoundException if no user is currently linked to the given party ID
     */
    @Transactional(readOnly = true)
    public BankingUser getUserByCustomerPartyId(UUID customerPartyId) {
        return userRepository.findByCustomerPartyId(customerPartyId).orElseThrow(
                () -> ResourceNotFoundException
                        .opaque("No user linked to customer: " + customerPartyId, OPAQUE_NOT_FOUND));
    }

    /**
     * Searches for user accounts matching the supplied filter criteria.
     *
     * All criteria fields are optional and combined with AND semantics. String fields such as
     * username and email use case-insensitive contains matching. The suspended flag, when true,
     * returns only accounts with a non-null suspendedAt timestamp; when false it returns only
     * accounts whose suspendedAt is null; when null the filter is not applied.
     *
     * @param criteria  the filter object; all fields are optional and null values are ignored
     * @param pageable  pagination and sorting parameters
     * @return a page of BankingUser entities matching all supplied criteria
     */
    @Transactional(readOnly = true)
    public Page<BankingUser> searchUsers(UserSearchCriteria criteria, Pageable pageable) {
        Specification<BankingUser> spec = (root, query, cb) -> cb.conjunction();

        if (criteria.getQ() != null && !criteria.getQ().isBlank()) {
            spec = spec.and(qSpecification(criteria.getQ().trim()));
        }

        if (criteria.getUsername() != null && !criteria.getUsername().isBlank()) {
            spec = spec.and(
                    (root, query, cb) -> cb
                            .like(cb.lower(root.get("username")), "%" + criteria.getUsername().toLowerCase() + "%"));
        }

        if (criteria.getEmail() != null && !criteria.getEmail().isBlank()) {
            spec = spec.and(
                    (root, query, cb) -> cb
                            .like(cb.lower(root.get("email")), "%" + criteria.getEmail().toLowerCase() + "%"));
        }

        if (criteria.getUserType() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userType"), criteria.getUserType()));
        }

        if (criteria.getEnabled() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("enabled"), criteria.getEnabled()));
        }

        if (criteria.getLocked() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("accountLocked"), criteria.getLocked()));
        }

        if (criteria.getBranchCode() != null && !criteria.getBranchCode().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("branchCode"), criteria.getBranchCode()));
        }

        if (criteria.getProvisioningStatus() != null) {
            spec = spec.and(
                    (root, query, cb) -> cb.equal(root.get("provisioningStatus"), criteria.getProvisioningStatus()));
        }

        if (Boolean.TRUE.equals(criteria.getSuspended())) {
            spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("suspendedAt")));
        }

        if (Boolean.FALSE.equals(criteria.getSuspended())) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("suspendedAt")));
        }

        return userRepository.findAll(spec, pageable);
    }

    /**
     * Typeahead for audit filters and similar screens ({@link com.openfinova.banking.identity.controller.SecurityAuditController}).
     * All user types; same {@code q} matching rules as {@link #searchUsers}.
     *
     * @param rawQ  the search term to match against
     * @param limit the maximum number of results to return
     * @return a list of UserResponse objects matching the search term
     */
    @Transactional(readOnly = true)
    public List<UserResponse> suggestUsersForLookup(String rawQ, int limit) {
        int cap = Math.min(Math.max(limit, 1), 50);
        String term = rawQ == null ? "" : rawQ.trim();
        if (term.isEmpty()) {
            return List.of();
        }
        try {
            UUID id = UUID.fromString(term);
            return userRepository.findById(id).map(u -> List.of(UserResponse.from(u))).orElseGet(List::of);
        } catch (IllegalArgumentException ignored) {
            /* not a UUID */
        }
        Specification<BankingUser> spec = qSpecification(term);
        Page<BankingUser> page = userRepository.findAll(spec, PageRequest.of(0, cap));
        return page.getContent().stream().map(UserResponse::from).toList();
    }

    /**
     * Updates the mutable access and profile fields of an existing banking user account.
     *
     * Only non-null fields in the request are applied, making this a partial-update operation.
     * When customerPartyId changes the previous customer party link is removed and a new one is
     * established through CustomerInfoService, provided the service is available and the new
     * party is active. Changing accountExpiresAt also clears accountExpiryWarningNotifiedAt and
     * records an ACCOUNT_EXPIRY_SET audit event. A USER_UPDATED audit event is recorded with a
     * before-and-after snapshot of all modified fields.
     *
     * @param userId   the ID of the user to update
     * @param request  the partial update payload; fields left null are not changed
     * @param auditor  the authenticated actor performing the update, used for audit recording
     * @return the updated and persisted BankingUser entity
     * @throws ResourceNotFoundException if no user exists with the given ID
     * @throws IllegalArgumentException  if the new customer party ID refers to a party that is
     *                                   not found or is not active
     */
    @Transactional
    public BankingUser updateUserAccess(UUID userId, UpdateUserAccessRequest request, AuditActor auditor) {
        BankingUser user = getUser(userId);

        if (request.getGlApprovalRole() != null) {
            GlApprovalRoleValidation.requireValidOrNull(request.getGlApprovalRole());
        }

        StringBuilder prev = new StringBuilder();
        prev.append("email=").append(user.getEmail()).append(";branch=").append(user.getBranchCode())
                .append(";employeeId=").append(user.getEmployeeId()).append(";glApprovalRole=")
                .append(user.getGlApprovalRole()).append(";customerPartyId=").append(user.getCustomerPartyId());

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        if (request.getBranchCode() != null) {
            user.setBranchCode(request.getBranchCode());
        }

        if (request.getEmployeeId() != null) {
            user.setEmployeeId(request.getEmployeeId());
        }

        if (request.getGlApprovalRole() != null) {
            user.setGlApprovalRole(request.getGlApprovalRole());
        }

        if (request.getCustomerPartyId() != null) {
            // Validate the new customer party exists and is active before re-linking
            validateCustomerPartyId(request.getCustomerPartyId());
            UUID previousPartyId = user.getCustomerPartyId();
            user.setCustomerPartyId(request.getCustomerPartyId());
            CustomerInfoService customerInfoService = customerInfoServiceProvider.getIfAvailable();
            if (customerInfoService != null && !request.getCustomerPartyId().equals(previousPartyId)) {
                if (previousPartyId != null) {
                    customerInfoService.unlinkIdentityUser(previousPartyId);
                }
                customerInfoService.linkIdentityUser(request.getCustomerPartyId(), user.getId(), user.getUsername());
            }
        }

        if (request.getAccountExpiresAt() != null) {
            LocalDateTime previousExpiry = user.getAccountExpiresAt();
            LocalDateTime nextExpiry = request.getAccountExpiresAt();
            user.setAccountExpiresAt(nextExpiry);
            if (!Objects.equals(previousExpiry, nextExpiry)) {
                user.setAccountExpiryWarningNotifiedAt(null);
                recordAccountExpirySet(
                        userId,
                        user.getUsername(),
                        auditor,
                        previousExpiry,
                        nextExpiry,
                        "Account expiry updated via access fields");
            }
        }

        BankingUser saved = userRepository.save(user);
        String curr = "email=" + saved.getEmail() + ";branch=" + saved.getBranchCode() + ";employeeId="
                + saved.getEmployeeId() + ";glApprovalRole=" + saved.getGlApprovalRole() + ";customerPartyId="
                + saved.getCustomerPartyId();
        auditService.recordParticipating(
                SecurityAuditEventType.USER_UPDATED,
                userId,
                user.getUsername(),
                "Access fields updated",
                auditor,
                SecurityAuditExtensions.changeTracking(null, null, prev.toString(), curr));
        return saved;
    }

    /**
     * Replaces the complete set of roles assigned to a user.
     *
     * This is a full replacement, not an additive merge: all roles previously held by the user
     * are discarded and the resolved set from the supplied names becomes the new membership.
     * When the configuration flag requireApprovalForRoleAssignment is enabled an approved
     * workflow of type USER_ROLE_ASSIGNMENT must already exist for the target user, enforcing a
     * maker-checker control. A ROLE_ASSIGNED audit event is recorded capturing the before and
     * after role sets.
     *
     * @param userId     the ID of the user whose roles are being replaced
     * @param roleNames  the exact names of the roles to assign; every name must exist in the
     *                   database
     * @param auditor    the authenticated actor performing the operation, used for audit
     *                   recording and role-assignment policy checks
     * @return the updated and persisted BankingUser entity
     * @throws ResourceNotFoundException if the user or any named role does not exist
     * @throws IllegalStateException     if workflow enforcement is active and no approved
     *                                   workflow exists for this user
     */
    @Transactional
    public BankingUser assignRoles(UUID userId, Set<String> roleNames, AuditActor auditor) {
        BankingUser user = getUser(userId);
        requireApprovedWorkflowForRoleAssignment(userId);

        Set<String> previousRoles = new HashSet<>();
        user.getRoles().forEach(r -> previousRoles.add(r.getName()));

        Set<BankingRole> resolved = resolveRoles(roleNames);
        roleAssignmentValidationService.validate(auditor, user, resolved);
        user.setRoles(resolved);

        BankingUser saved = userRepository.save(user);

        auditService.recordParticipating(
                SecurityAuditEventType.ROLE_ASSIGNED,
                userId,
                user.getUsername(),
                "Roles replaced",
                auditor,
                SecurityAuditExtensions.changeTracking(null, null, previousRoles.toString(), roleNames.toString()),
                AuditEventDetail.rolesAssigned(user.getUsername(), previousRoles, roleNames));
        return saved;
    }

    /**
     * Enables or disables a user account.
     *
     * When disabling, the current timestamp is stored in disabledAt. When re-enabling,
     * disabledAt is cleared. An ACCOUNT_ENABLED or ACCOUNT_DISABLED audit event is recorded
     * depending on the new state.
     *
     * @param userId   the ID of the user to enable or disable
     * @param enabled  true to enable the account, false to disable it
     * @param auditor  the authenticated actor performing the operation, used for audit recording
     * @return the updated and persisted BankingUser entity
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional
    public BankingUser setEnabled(UUID userId, boolean enabled, AuditActor auditor) {
        BankingUser user = getUser(userId);
        boolean wasEnabled = user.isEnabled();
        user.setEnabled(enabled);
        if (!enabled) {
            user.setDisabledAt(LocalDateTime.now());
        } else {
            user.setDisabledAt(null);
        }

        BankingUser saved = userRepository.save(user);
        auditService.recordParticipating(
                enabled ? SecurityAuditEventType.ACCOUNT_ENABLED : SecurityAuditEventType.ACCOUNT_DISABLED,
                userId,
                user.getUsername(),
                enabled ? "Account enabled" : "Account disabled",
                auditor,
                SecurityAuditExtensions
                        .changeTracking(null, null, Boolean.toString(wasEnabled), Boolean.toString(enabled)));
        return saved;
    }

    /**
     * Administratively locks a user account, preventing login regardless of credentials.
     *
     * Sets accountLocked to true, records the lock timestamp, and stores the supplied reason.
     * Unlike a temporary failed-login lock this lock can only be removed by an explicit call to
     * unlockUser. An ACCOUNT_LOCKED_ADMIN audit event is recorded.
     *
     * @param userId  the ID of the user to lock
     * @param reason  a human-readable explanation for the lock, stored on the account and
     *                included in the audit record
     * @param auditor the authenticated actor performing the operation, used for audit recording
     * @return the updated and persisted BankingUser entity
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional
    public BankingUser lockUser(UUID userId, String reason, AuditActor auditor) {
        BankingUser user = getUser(userId);
        user.setAccountLocked(true);
        user.setLockedAt(LocalDateTime.now());
        user.setLockedReason(reason);
        BankingUser saved = userRepository.save(user);
        auditService.recordParticipating(
                SecurityAuditEventType.ACCOUNT_LOCKED_ADMIN,
                userId,
                user.getUsername(),
                "Locked by admin: " + reason,
                auditor);
        return saved;
    }

    /**
     * Removes an administrative or failed-login lock from a user account.
     *
     * Clears accountLocked, lockedAt, and lockedReason, resets the failed login attempt
     * counter to zero, and clears any temporary lockout expiry timestamp. An ACCOUNT_UNLOCKED
     * audit event is recorded.
     *
     * @param userId  the ID of the user to unlock
     * @param auditor the authenticated actor performing the operation, used for audit recording
     * @return the updated and persisted BankingUser entity
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional
    public BankingUser unlockUser(UUID userId, AuditActor auditor) {
        BankingUser user = getUser(userId);
        user.setAccountLocked(false);
        user.setLockedAt(null);
        user.setLockedReason(null);
        user.setFailedLoginAttempts(0);
        user.setFailedLoginLockedUntil(null);
        BankingUser saved = userRepository.save(user);
        auditService.recordParticipating(
                SecurityAuditEventType.ACCOUNT_UNLOCKED,
                userId,
                user.getUsername(),
                "Account unlocked by admin",
                auditor);
        return saved;
    }

    /**
     * Soft-deletes a user account by disabling it without removing any persisted data.
     *
     * Sets enabled to false and records the disabledAt timestamp. The account remains in the
     * database and is still visible to administrators. This is distinct from deprovisionUser,
     * which also scrubs sensitive fields such as the password hash, MFA secrets, and roles.
     * An ACCOUNT_DISABLED audit event is recorded.
     *
     * @param userId  the ID of the user to soft-delete
     * @param auditor the authenticated actor performing the operation, used for audit recording
     * @return the updated and persisted BankingUser entity
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional
    public BankingUser softDeleteUser(UUID userId, AuditActor auditor) {
        BankingUser user = getUser(userId);
        user.setEnabled(false);
        user.setDisabledAt(LocalDateTime.now());
        BankingUser saved = userRepository.save(user);
        auditService.recordParticipating(
                SecurityAuditEventType.ACCOUNT_DISABLED,
                userId,
                user.getUsername(),
                "Account soft-deleted",
                auditor);
        return saved;
    }

    /**
     * Approves a pending provisioning request, activating the user account.
     *
     * Transitions the account from PENDING_APPROVAL to ACTIVE status and sets enabled to true,
     * allowing the user to log in. Throws if the account is not currently in PENDING_APPROVAL
     * state. An ACCOUNT_PROVISIONING_APPROVED audit event is recorded.
     *
     * @param userId  the ID of the user whose provisioning is being approved
     * @param auditor   the authenticated actor performing the approval, used for audit recording
     * @return the updated and persisted BankingUser entity
     * @throws ResourceNotFoundException if no user exists with the given ID
     * @throws IllegalStateException     if the user is not currently in PENDING_APPROVAL status
     */
    @Transactional
    public BankingUser approveProvisioning(UUID userId, AuditActor auditor) {
        BankingUser user = getUser(userId);
        if (user.getProvisioningStatus() != AccountProvisioningStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("User is not awaiting provisioning approval: " + userId);
        }
        user.setProvisioningStatus(AccountProvisioningStatus.ACTIVE);
        user.setEnabled(true);
        BankingUser saved = userRepository.save(user);
        auditService.recordParticipating(
                SecurityAuditEventType.ACCOUNT_PROVISIONING_APPROVED,
                userId,
                user.getUsername(),
                "Provisioning approved",
                auditor);
        return saved;
    }

    /**
     * Rejects a pending provisioning request, preventing the account from becoming active.
     *
     * Transitions the account to REJECTED status and leaves it disabled. Throws if the account
     * is not currently in PENDING_APPROVAL state. An ACCOUNT_PROVISIONING_REJECTED audit event
     * is recorded including the supplied reason.
     *
     * @param userId   the ID of the user whose provisioning is being rejected
     * @param reason   a human-readable explanation for the rejection, included in the audit
     *                 record
     * @param auditor    the authenticated actor performing the rejection, used for audit recording
     * @return the updated and persisted BankingUser entity
     * @throws ResourceNotFoundException if no user exists with the given ID
     * @throws IllegalStateException     if the user is not currently in PENDING_APPROVAL status
     */
    @Transactional
    public BankingUser rejectProvisioning(UUID userId, String reason, AuditActor auditor) {
        BankingUser user = getUser(userId);
        if (user.getProvisioningStatus() != AccountProvisioningStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("User is not awaiting provisioning approval: " + userId);
        }
        user.setProvisioningStatus(AccountProvisioningStatus.REJECTED);
        user.setEnabled(false);
        BankingUser saved = userRepository.save(user);
        auditService.recordParticipating(
                SecurityAuditEventType.ACCOUNT_PROVISIONING_REJECTED,
                userId,
                user.getUsername(),
                "Rejected: " + reason,
                auditor);
        return saved;
    }

    /**
     * Administratively suspends a user account, optionally until a specified date and time.
     *
     * Records the suspension timestamp, reason, and optional lift date on the account.
     * Deprovisioned accounts cannot be suspended. If suspensionUntil is provided it must be
     * strictly in the future at the time of the call. Downstream access checks are expected to
     * honour the suspended state. An ACCOUNT_SUSPENDED audit event is recorded.
     *
     * @param userId           the ID of the user to suspend
     * @param reason           a human-readable explanation for the suspension
     * @param suspensionUntil  the date and time at which the suspension automatically lifts,
     *                         or null for an indefinite suspension
     * @param actor            the authenticated actor performing the suspension, used for audit
     *                         recording
     * @return the updated and persisted BankingUser entity
     * @throws ResourceNotFoundException if no user exists with the given ID
     * @throws IllegalStateException     if the user is already deprovisioned
     * @throws IllegalArgumentException  if suspensionUntil is not null and is not in the future
     */
    @Transactional
    public BankingUser suspendUser(UUID userId, String reason, LocalDateTime suspensionUntil, AuditActor auditor) {
        BankingUser user = getUser(userId);
        if (user.getProvisioningStatus() == AccountProvisioningStatus.DEPROVISIONED) {
            throw new IllegalStateException("Cannot suspend a deprovisioned user: " + userId);
        }
        LocalDateTime now = LocalDateTime.now();
        if (suspensionUntil != null && !suspensionUntil.isAfter(now)) {
            throw new IllegalArgumentException("suspensionUntil must be in the future");
        }
        user.setSuspendedAt(now);
        user.setSuspensionReason(reason);
        user.setSuspensionUntil(suspensionUntil);
        BankingUser saved = userRepository.save(user);
        auditService.recordParticipating(
                SecurityAuditEventType.ACCOUNT_SUSPENDED,
                userId,
                user.getUsername(),
                suspensionUntil != null ? "Suspended until " + suspensionUntil + ": " + reason : "Suspended: " + reason,
                auditor);
        return saved;
    }

    /**
     * Clears an administrative suspension from a user account.
     *
     * Removes suspendedAt, suspensionReason, and suspensionUntil, restoring the account to its
     * normal operating state. An ACCOUNT_REACTIVATED audit event is recorded.
     *
     * @param userId  the ID of the user to reactivate
     * @param auditor   the authenticated actor performing the reactivation, used for audit
     *                recording
     * @return the updated and persisted BankingUser entity
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional
    public BankingUser reactivateUser(UUID userId, AuditActor auditor) {
        BankingUser user = getUser(userId);
        user.setSuspendedAt(null);
        user.setSuspensionReason(null);
        user.setSuspensionUntil(null);
        BankingUser saved = userRepository.save(user);
        auditService.recordParticipating(
                SecurityAuditEventType.ACCOUNT_REACTIVATED,
                userId,
                user.getUsername(),
                "Administrative suspension cleared",
                auditor);
        return saved;
    }

    /**
     * Fully deprovisions a user account, scrubbing all sensitive data and severing linkages.
     *
     * The following actions are performed atomically: provisioning status is set to
     * DEPROVISIONED, the account is disabled, all roles are cleared, MFA credentials and
     * recovery codes are removed, the password hash is replaced with a random irreversible
     * value, and the password history is cleared. If the account had a customer party link,
     * that link is removed through CustomerInfoService when the service is available so the
     * customer party can be re-onboarded in the future. A USER_DEPROVISIONED audit event is
     * recorded and a UserAccountDeprovisionedEvent is published to the application event bus
     * for downstream processing.
     *
     * @param userId  the ID of the user to deprovision
     * @param reason  a human-readable explanation for the deprovisioning, included in the audit
     *                record; may be null, in which case a default message is used
     * @param actor   the authenticated actor performing the operation, used for audit recording
     *                and event publishing
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional
    public void deprovisionUser(UUID userId, String reason, AuditActor auditor) {
        BankingUser user = getUser(userId);
        LocalDateTime now = LocalDateTime.now();
        String username = user.getUsername();
        UUID customerPartyId = user.getCustomerPartyId();

        user.setProvisioningStatus(AccountProvisioningStatus.DEPROVISIONED);
        user.setEnabled(false);
        user.setDisabledAt(now);
        user.setAccountLocked(false);
        user.setSuspendedAt(null);
        user.setSuspensionReason(null);
        user.setSuspensionUntil(null);
        user.getRoles().clear();
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.getMfaRecoveryCodes().clear();
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.getPasswordHistory().clear();

        userRepository.save(user);

        // Remove the link stored on the customer party record so the customer can be re-onboarded
        if (customerPartyId != null) {
            CustomerInfoService customerInfoService = customerInfoServiceProvider.getIfAvailable();
            if (customerInfoService != null) {
                customerInfoService.unlinkIdentityUser(customerPartyId);
            }
        }

        auditService.recordParticipating(
                SecurityAuditEventType.USER_DEPROVISIONED,
                userId,
                username,
                reason != null ? reason : "Account deprovisioned",
                auditor);

        eventPublisher.publishEvent(
                new UserAccountDeprovisionedEvent(
                        this,
                        userId,
                        username,
                        now,
                        reason,
                        auditor.userId(),
                        auditor.username()));
    }

    /**
     * Resets the password of a user account on behalf of an administrator.
     *
     * Validates the new password against the current complexity policy and checks it against
     * the user's recent password history before applying the change. The passwordChangedAt
     * timestamp is updated, a new expiry is calculated from the configured maximum age,
     * forcePasswordChange is cleared, and the encoded password is appended to the history. A
     * PASSWORD_CHANGED audit event and an ACCOUNT_EXPIRY_SET audit event are both recorded.
     *
     * @param userId       the ID of the user whose password is being reset
     * @param newPassword  the new raw password to set; must satisfy the current policy and must
     *                     not appear in the recent password history
     * @param auditor        the authenticated administrator performing the reset, used for audit
     *                     recording
     * @throws ResourceNotFoundException        if no user exists with the given ID
     * @throws PasswordPolicyViolationException if the new password violates complexity rules or
     *                                          was recently used
     */
    @Transactional
    public void resetPassword(UUID userId, String newPassword, AuditActor auditor) {
        BankingUser user = getUser(userId);
        passwordPolicyService.validate(newPassword);
        passwordPolicyService.checkHistory(newPassword, user.getPasswordHistory());

        LocalDateTime previousExpiry = user.getPasswordExpiresAt();
        String encoded = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encoded);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordExpiresAt(LocalDateTime.now().plusDays(passwordPolicyService.getMaxAgeDays()));
        user.setForcePasswordChange(false);
        addToPasswordHistory(user, encoded);

        userRepository.save(user);
        auditService.recordParticipating(
                SecurityAuditEventType.PASSWORD_CHANGED,
                userId,
                user.getUsername(),
                "Password reset by admin",
                auditor);
        recordAccountExpirySet(
                userId,
                user.getUsername(),
                auditor,
                previousExpiry,
                user.getPasswordExpiresAt(),
                "Password reset by admin");
    }

    /**
     * Immediately expires a user's password, forcing a new password to be chosen on next login.
     *
     * Sets forcePasswordChange to true and sets passwordExpiresAt to the current timestamp,
     * making the password expired right away. The user will be required to change their password
     * before accessing protected resources. A PASSWORD_FORCE_CHANGE_SET audit event and an
     * ACCOUNT_EXPIRY_SET audit event are both recorded.
     *
     * @param userId  the ID of the user for whom a forced password change is being set
     * @param auditor   the authenticated administrator performing the operation, used for audit
     *                recording
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional
    public void forcePasswordChange(UUID userId, AuditActor auditor) {
        BankingUser user = getUser(userId);
        LocalDateTime previousExpiry = user.getPasswordExpiresAt();
        user.setForcePasswordChange(true);
        user.setPasswordExpiresAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.recordParticipating(
                SecurityAuditEventType.PASSWORD_FORCE_CHANGE_SET,
                userId,
                user.getUsername(),
                "Force password change set by admin",
                auditor);
        recordAccountExpirySet(
                userId,
                user.getUsername(),
                auditor,
                previousExpiry,
                user.getPasswordExpiresAt(),
                "Immediate expiry for forced password change");
    }

    /**
     * Allows a user to change their own password by first verifying their current one.
     *
     * The supplied current password is verified against the stored hash before any change is
     * made. The new password is then validated against the complexity policy and checked against
     * the recent password history. On success, passwordChangedAt is updated, a new expiry is
     * calculated from the configured maximum age, forcePasswordChange is cleared, and the
     * encoded password is appended to the history. A PASSWORD_CHANGED audit event and an
     * ACCOUNT_EXPIRY_SET audit event are both recorded.
     *
     * @param userId           the ID of the user changing their own password
     * @param currentPassword  the user's current raw password, verified before the change is
     *                         allowed to proceed
     * @param newPassword      the new raw password to set; must satisfy the current policy and
     *                         must not appear in the recent password history
     * @param auditor          the authenticated actor representing the user making the change,
     *                         used for audit recording
     * @throws IllegalArgumentException         if the current password does not match the stored
     *                                          hash
     * @throws PasswordPolicyViolationException if the new password violates complexity rules or
     *                                          was recently used
     * @throws ResourceNotFoundException        if no user exists with the given ID
     */
    @Transactional
    public void changeOwnPassword(UUID userId, String currentPassword, String newPassword, AuditActor auditor) {
        BankingUser user = getUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            log.warn("Self-service password change failed: current password mismatch (userId={})", userId);
            throw new IllegalArgumentException("Unable to verify the current password.");
        }
        passwordPolicyService.validate(newPassword);
        passwordPolicyService.checkHistory(newPassword, user.getPasswordHistory());

        LocalDateTime previousExpiry = user.getPasswordExpiresAt();
        String encoded = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encoded);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordExpiresAt(LocalDateTime.now().plusDays(passwordPolicyService.getMaxAgeDays()));
        user.setForcePasswordChange(false);
        addToPasswordHistory(user, encoded);

        userRepository.save(user);
        auditService.recordParticipating(
                SecurityAuditEventType.PASSWORD_CHANGED,
                userId,
                user.getUsername(),
                "Password changed by user",
                auditor);
        recordAccountExpirySet(
                userId,
                user.getUsername(),
                auditor,
                previousExpiry,
                user.getPasswordExpiresAt(),
                "Password changed by user");
    }

    private void recordAccountExpirySet(UUID userId, String username, AuditActor auditor, LocalDateTime previousExpiry,
            LocalDateTime newExpiry, String details) {
        auditService.recordParticipating(
                SecurityAuditEventType.ACCOUNT_EXPIRY_SET,
                userId,
                username,
                details,
                auditor,
                SecurityAuditExtensions.changeTracking(
                        null,
                        null,
                        previousExpiry != null ? previousExpiry.toString() : null,
                        newExpiry != null ? newExpiry.toString() : null));
    }

    private Set<BankingRole> resolveRoles(Set<String> roleNames) {
        Set<BankingRole> found = roleRepository.findByNameIn(roleNames);
        if (found.size() != roleNames.size()) {
            Set<String> foundNames = found.stream().map(BankingRole::getName)
                    .collect(Collectors.toCollection(HashSet::new));
            Set<String> missing = new HashSet<>(roleNames);
            missing.removeAll(foundNames);
            throw ResourceNotFoundException.opaque("Role not found: " + missing, OPAQUE_NOT_FOUND);
        }
        return found;
    }

    private void addToPasswordHistory(BankingUser user, String encodedPassword) {
        user.getPasswordHistory().add(encodedPassword);
        int maxHistory = passwordPolicyService.getHistoryCount();
        while (user.getPasswordHistory().size() > maxHistory) {
            user.getPasswordHistory().remove(0);
        }
    }

    /**
     * Verifies that an APPROVED workflow of type {@link #RESOURCE_TYPE_USER_ROLE_ASSIGNMENT}
     * exists for the given user, enforcing the maker-checker requirement for role assignments.
     *
     * <p>Bypassed when {@code identity.workflow-enforcement.require-approval-for-role-assignment}
     * is {@code false} (e.g. in development or test profiles).
     *
     * @throws IllegalStateException if enforcement is enabled and no approved workflow is found
     */
    private void requireApprovedWorkflowForRoleAssignment(UUID userId) {
        if (!enforcementProperties.isRequireApprovalForRoleAssignment()) {
            return;
        }
        boolean approved = workflowRepository
                .findLatestApprovedByResourceTypeAndResourceId(RESOURCE_TYPE_USER_ROLE_ASSIGNMENT, userId.toString())
                .isPresent();
        if (!approved) {
            throw new IllegalStateException(
                    "A pre-approved workflow is required before assigning roles to a user. "
                            + "Submit and obtain approval for a '" + RESOURCE_TYPE_USER_ROLE_ASSIGNMENT
                            + "' workflow scoped to user " + userId + " first.");
        }
    }

    /**
     * Verifies that the given customer party ID refers to an existing, active customer.
     * Silently passes if the ID is null (not all user types have a linked customer party)
     * or if the CustomerInfoService is unavailable (e.g. in isolated tests).
     *
     * @param customerPartyId the UUID to validate, or null
     * @throws IllegalArgumentException if the customer is not found or is not active
     */
    private void validateCustomerPartyId(UUID customerPartyId) {
        if (customerPartyId == null) {
            return;
        }
        CustomerInfoService customerInfoService = customerInfoServiceProvider.getIfAvailable();
        if (customerInfoService == null) {
            log.warn("CustomerInfoService not available; skipping customerPartyId validation for {}", customerPartyId);
            return;
        }
        if (!customerInfoService.customerExists(customerPartyId)) {
            throw new IllegalArgumentException("Customer party not found: " + customerPartyId);
        }
        if (!customerInfoService.isCustomerActive(customerPartyId)) {
            throw new IllegalArgumentException("Customer party is not active: " + customerPartyId);
        }
    }

    /**
     * OR filter: exact id (UUID), or case-insensitive substring on username / email.
     * Wildcard characters in {@code raw} are stripped to avoid broad LIKE matches.
     */
    private static Specification<BankingUser> qSpecification(String raw) {
        return (root, query, cb) -> {
            try {
                UUID uuid = UUID.fromString(raw);
                return cb.equal(root.get("id"), uuid);
            } catch (IllegalArgumentException ignored) {
                String term = raw.toLowerCase(Locale.ROOT).replace("%", "").replace("_", "").trim();
                if (term.isEmpty()) {
                    return cb.disjunction();
                }
                String like = "%" + term + "%";
                Predicate usernamePred = cb.like(cb.lower(root.get("username")), like);
                Predicate emailPred = cb.and(
                        cb.isNotNull(root.get("email")),
                        cb.like(cb.lower(root.get("email")), like));
                return cb.or(usernamePred, emailPred);
            }
        };
    }
}
