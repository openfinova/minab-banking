package com.openfinova.banking.identity.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.identity.api.audit.AuditActor;
import com.openfinova.banking.identity.api.permission.BankingPermission;
import com.openfinova.banking.identity.audit.AuditEventDetail;
import com.openfinova.banking.identity.audit.SecurityAuditExtensions;
import com.openfinova.banking.identity.config.WorkflowEnforcementProperties;
import com.openfinova.banking.identity.entity.BankingRole;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.repository.ApprovalWorkflowInstanceRepository;
import com.openfinova.banking.identity.repository.RoleRepository;

/**
 * Full CRUD and permission manipulation for BankingRole entities.
 *
 * Covers role creation, metadata updates, deletion, and fine-grained permission management
 * including full replacement, additive grant, and selective removal of BankingPermission
 * values. System roles seeded at startup are protected from deletion and from having their
 * permissions entirely replaced or removed. Permission-mutating operations on non-system roles
 * additionally require a pre-approved ROLE_PERMISSION_CHANGE workflow when enforcement is enabled,
 * implementing a change-management control. All state transitions are recorded through
 * SecurityAuditService.
 */
@Service
public class RoleManagementService {

    private static final Logger log = LoggerFactory.getLogger(RoleManagementService.class);

    private static final String OPAQUE_NOT_FOUND = "The requested resource was not found.";

    /** Resource type used when creating an approval workflow for a role permission change. */
    public static final String RESOURCE_TYPE_ROLE_PERMISSION_CHANGE = "ROLE_PERMISSION_CHANGE";

    private final RoleRepository roleRepository;
    private final SecurityAuditService auditService;
    private final ApprovalWorkflowInstanceRepository workflowRepository;
    private final WorkflowEnforcementProperties enforcementProperties;

    public RoleManagementService(RoleRepository roleRepository, SecurityAuditService auditService,
            ApprovalWorkflowInstanceRepository workflowRepository,
            WorkflowEnforcementProperties enforcementProperties) {
        this.roleRepository = roleRepository;
        this.auditService = auditService;
        this.workflowRepository = workflowRepository;
        this.enforcementProperties = enforcementProperties;
    }

    /**
     * Returns all roles in the system with no filtering applied.
     *
     * Includes both system roles and custom roles. Intended for administrative listing
     * and for role-picker UI components.
     *
     * @return a list of all persisted BankingRole entities
     */
    @Transactional(readOnly = true)
    public List<BankingRole> listRoles() {
        return roleRepository.findAll();
    }

    /**
     * Retrieves a single role by its internal UUID.
     *
     * @param id  the unique identifier of the role to look up
     * @return the matching BankingRole entity
     * @throws ResourceNotFoundException if no role exists with the given ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "bankingRoles", key = "'id_' + #id")
    public BankingRole getRole(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id, OPAQUE_NOT_FOUND));
    }

    /** Loads a managed entity for writes; bypasses read cache so mutations see current state. */
    private BankingRole requireRoleForMutation(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id, OPAQUE_NOT_FOUND));
    }

    /**
     * Creates a new custom role with the given name, display name, and initial permissions.
     *
     * The role name must be unique across all roles. The new role is always created as a
     * non-system role. If the permissions set is null an empty permission set is used. A
     * ROLE_CREATED audit event is recorded with a snapshot of the initial permissions.
     *
     * @param name         the unique internal identifier name for the role
     * @param displayName  the human-readable label shown in the UI
     * @param description  optional free-text description of the role's purpose
     * @param permissions  the initial set of BankingPermission values to assign; null is
     *                     treated as an empty set
     * @param actor        the authenticated actor performing the operation, used for audit
     *                     recording
     * @return the newly persisted BankingRole entity
     * @throws IllegalArgumentException if a role with the given name already exists
     */
    @Transactional
    @CacheEvict(value = "bankingRoles", allEntries = true)
    public BankingRole createRole(String name, String displayName, String description,
            Set<BankingPermission> permissions, AuditActor actor) {
        if (roleRepository.existsByName(name)) {
            log.warn("Role create rejected: name already exists (name={})", name);
            throw new IllegalArgumentException("Unable to create the role. Check the submitted data and try again.");
        }
        BankingRole role = new BankingRole(name, displayName);
        role.setDescription(description);
        role.setSystemRole(false);
        role.setPermissions(
                permissions != null ? EnumSet.copyOf(permissions) : EnumSet.noneOf(BankingPermission.class));
        BankingRole saved = roleRepository.save(role);
        String permSnapshot = saved.getPermissions().toString();
        auditService.recordParticipating(
                SecurityAuditEventType.ROLE_CREATED,
                null,
                null,
                "Created role: " + name,
                actor,
                SecurityAuditExtensions.changeTracking(null, null, null, permSnapshot),
                AuditEventDetail.roleCreated(
                        name,
                        saved.getPermissions().stream().map(BankingPermission::name)
                                .collect(java.util.stream.Collectors.toList())));
        return saved;
    }

    /**
     * Updates the displayName and description of an existing role.
     *
     * Only non-null values in the supplied parameters are applied. The role name and system
     * role flag are not modifiable through this method. A ROLE_UPDATED audit event is recorded
     * with a before-and-after snapshot of the changed fields.
     *
     * @param id           the UUID of the role to update
     * @param displayName  the new human-readable label; null leaves the existing value
     * @param description  the new description text; null leaves the existing value
     * @param actor        the authenticated actor performing the operation, used for audit
     *                     recording
     * @return the updated and persisted BankingRole entity
     * @throws ResourceNotFoundException if no role exists with the given ID
     */
    @Transactional
    @CacheEvict(value = "bankingRoles", allEntries = true)
    public BankingRole updateRole(UUID id, String displayName, String description, AuditActor actor) {
        BankingRole role = requireRoleForMutation(id);
        String prev = "displayName=" + role.getDisplayName() + ";description=" + role.getDescription();
        if (displayName != null) {
            role.setDisplayName(displayName);
        }
        if (description != null) {
            role.setDescription(description);
        }
        BankingRole saved = roleRepository.save(role);
        String curr = "displayName=" + saved.getDisplayName() + ";description=" + saved.getDescription();
        auditService.recordParticipating(
                SecurityAuditEventType.ROLE_UPDATED,
                null,
                null,
                "Updated role: " + role.getName(),
                actor,
                SecurityAuditExtensions.changeTracking(null, null, prev, curr));
        return saved;
    }

    /**
     * Permanently deletes a custom role from the system.
     *
     * System roles cannot be deleted. A ROLE_DELETED audit event is recorded.
     *
     * @param id     the UUID of the role to delete
     * @param actor  the authenticated actor performing the deletion, used for audit recording
     * @throws ResourceNotFoundException if no role exists with the given ID
     * @throws IllegalArgumentException  if the role is a system role
     */
    @Transactional
    @CacheEvict(value = "bankingRoles", allEntries = true)
    public void deleteRole(UUID id, AuditActor actor) {
        BankingRole role = requireRoleForMutation(id);
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("Cannot delete system role: " + role.getName());
        }
        String roleName = role.getName();
        roleRepository.delete(role);
        auditService.recordParticipating(
                SecurityAuditEventType.ROLE_DELETED,
                null,
                null,
                "Deleted role: " + roleName,
                actor,
                SecurityAuditExtensions.changeTracking(null, null, roleName, null),
                AuditEventDetail.roleDeleted(roleName));
    }

    /**
     * Replaces the entire permission set of a role with the supplied set.
     *
     * This is a full replacement: all existing permissions are removed and replaced with the
     * provided set. System roles cannot have their permissions replaced. When workflow
     * enforcement is enabled a pre-approved workflow must exist for the role. A
     * ROLE_PERMISSIONS_CHANGED audit event is recorded with a before-and-after snapshot.
     *
     * @param id           the UUID of the role whose permissions are being replaced
     * @param permissions  the complete new permission set; null is treated as an empty set
     * @param actor        the authenticated actor performing the operation, used for audit
     *                     recording
     * @return the updated and persisted BankingRole entity
     * @throws ResourceNotFoundException if no role exists with the given ID
     * @throws IllegalArgumentException  if the role is a system role
     * @throws IllegalStateException     if workflow enforcement is active and no approved
     *                                   workflow exists for this role
     */
    @Transactional
    @CacheEvict(value = "bankingRoles", allEntries = true)
    public BankingRole setPermissions(UUID id, Set<BankingPermission> permissions, AuditActor actor) {
        BankingRole role = requireRoleForMutation(id);
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("Cannot replace permissions on system role: " + role.getName());
        }
        requireApprovedWorkflow(id);
        String prev = role.getPermissions().toString();
        role.setPermissions(
                permissions != null ? EnumSet.copyOf(permissions) : EnumSet.noneOf(BankingPermission.class));
        BankingRole saved = roleRepository.save(role);
        auditService.recordParticipating(
                SecurityAuditEventType.ROLE_PERMISSIONS_CHANGED,
                null,
                null,
                "Replaced permissions on role: " + role.getName(),
                actor,
                SecurityAuditExtensions.changeTracking(null, null, prev, saved.getPermissions().toString()),
                AuditEventDetail.permissionsReplaced(role.getName(), prev, saved.getPermissions().toString()));
        return saved;
    }

    /**
     * Grants additional permissions to a role without affecting existing ones.
     *
     * Permissions already present on the role are left unchanged. System roles cannot receive new
     * permissions via this API. When workflow enforcement is enabled a pre-approved workflow must
     * exist for the role. A PERMISSION_ADDED audit event is recorded with a before-and-after snapshot.
     *
     * @param id      the UUID of the role to grant permissions to
     * @param toAdd   the set of BankingPermission values to add
     * @param actor   the authenticated actor performing the operation, used for audit recording
     * @return the updated and persisted BankingRole entity
     * @throws ResourceNotFoundException if no role exists with the given ID
     * @throws IllegalArgumentException  if the role is a system role
     * @throws IllegalStateException     if workflow enforcement is active and no approved
     *                                   workflow exists for this role
     */
    @Transactional
    @CacheEvict(value = "bankingRoles", allEntries = true)
    public BankingRole addPermissions(UUID id, Set<BankingPermission> toAdd, AuditActor actor) {
        BankingRole role = requireRoleForMutation(id);
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("Cannot add permissions to system role: " + role.getName());
        }
        requireApprovedWorkflow(id);
        String prev = role.getPermissions().toString();
        role.getPermissions().addAll(toAdd);
        BankingRole saved = roleRepository.save(role);
        auditService.recordParticipating(
                SecurityAuditEventType.PERMISSION_ADDED,
                null,
                null,
                "Added permissions to role " + role.getName() + ": " + toAdd,
                actor,
                SecurityAuditExtensions.changeTracking(null, null, prev, saved.getPermissions().toString()),
                AuditEventDetail.permissionsAdded(
                        role.getName(),
                        toAdd.stream().map(BankingPermission::name).collect(java.util.stream.Collectors.toList())));
        return saved;
    }

    /**
     * Removes specific permissions from a role without affecting other existing permissions.
     *
     * System roles cannot have permissions removed. When workflow enforcement is enabled a
     * pre-approved workflow must exist for the role. A PERMISSION_REMOVED audit event is
     * recorded with a before-and-after snapshot.
     *
     * @param id        the UUID of the role to remove permissions from
     * @param toRemove  the set of BankingPermission values to remove
     * @param actor     the authenticated actor performing the operation, used for audit
     *                  recording
     * @return the updated and persisted BankingRole entity
     * @throws ResourceNotFoundException if no role exists with the given ID
     * @throws IllegalArgumentException  if the role is a system role
     * @throws IllegalStateException     if workflow enforcement is active and no approved
     *                                   workflow exists for this role
     */
    @Transactional
    @CacheEvict(value = "bankingRoles", allEntries = true)
    public BankingRole removePermissions(UUID id, Set<BankingPermission> toRemove, AuditActor actor) {
        BankingRole role = requireRoleForMutation(id);
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("Cannot remove permissions from system role: " + role.getName());
        }
        requireApprovedWorkflow(id);
        String prev = role.getPermissions().toString();
        role.getPermissions().removeAll(toRemove);
        BankingRole saved = roleRepository.save(role);
        auditService.recordParticipating(
                SecurityAuditEventType.PERMISSION_REMOVED,
                null,
                null,
                "Removed permissions from role " + role.getName() + ": " + toRemove,
                actor,
                SecurityAuditExtensions.changeTracking(null, null, prev, saved.getPermissions().toString()),
                AuditEventDetail.permissionsRemoved(
                        role.getName(),
                        toRemove.stream().map(BankingPermission::name).collect(java.util.stream.Collectors.toList())));
        return saved;
    }

    /**
     * Verifies that an APPROVED workflow of type {@link #RESOURCE_TYPE_ROLE_PERMISSION_CHANGE}
     * exists for the given role, enforcing the change-management requirement.
     *
     * <p>Bypassed when {@code identity.workflow-enforcement.require-approval-for-role-permission-changes}
     * is {@code false} (e.g. in development or test profiles).
     *
     * @throws IllegalStateException if enforcement is enabled and no approved workflow is found
     */
    private void requireApprovedWorkflow(UUID roleId) {
        if (!enforcementProperties.isRequireApprovalForRolePermissionChanges()) {
            return;
        }
        boolean approved = workflowRepository
                .findLatestApprovedByResourceTypeAndResourceId(RESOURCE_TYPE_ROLE_PERMISSION_CHANGE, roleId.toString())
                .isPresent();
        if (!approved) {
            throw new IllegalStateException(
                    "A pre-approved change request is required for role permission modifications. "
                            + "Submit and obtain approval for a '" + RESOURCE_TYPE_ROLE_PERMISSION_CHANGE
                            + "' workflow scoped to role " + roleId + " first.");
        }
    }
}
