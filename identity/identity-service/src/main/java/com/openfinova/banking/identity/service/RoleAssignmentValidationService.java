package com.openfinova.banking.identity.service;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.openfinova.banking.identity.api.audit.AuditActor;
import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.config.RbacProperties;
import com.openfinova.banking.identity.entity.BankingRole;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.repository.UserRepository;

/**
 * Validates role assignment requests against separation-of-duties rules, user-type
 * eligibility constraints, and the assigner's privilege hierarchy.
 *
 * Three distinct checks are applied in sequence for every assignment request. First, the
 * assignee's user type is checked: customer users may only receive the configured customer
 * portal role, and staff users may not be assigned that role. Second, the proposed role set
 * is checked against all configured SoD pairs; if any pair is fully satisfied the assignment
 * is rejected and a ROLE_ASSIGNMENT_SOD_VIOLATION audit event is recorded. Third, the
 * assigning actor's own roles are inspected to confirm they hold a role at or above each
 * requested role in the role hierarchy; actors holding the configured privileged-assigner
 * role bypass the hierarchy check entirely. Failures in the second and third checks produce
 * audit trail entries before throwing so that denied assignments are always recorded.
 */
@Service
public class RoleAssignmentValidationService {

    private final UserRepository userRepository;
    private final RbacProperties rbacProperties;
    private final SecurityAuditService auditService;

    public RoleAssignmentValidationService(UserRepository userRepository, RbacProperties rbacProperties,
            SecurityAuditService auditService) {
        this.userRepository = userRepository;
        this.rbacProperties = rbacProperties;
        this.auditService = auditService;
    }

    /**
     * Validates that the given actor may assign the desired roles to the given assignee.
     *
     * Runs three checks in order:
     * 1. Assignee eligibility: customer users may only hold the customer portal role;
     *    non-customer users may not be assigned that role.
     * 2. Separation of duties: the desired role set must not contain both roles in any
     *    configured SoD pair. A ROLE_ASSIGNMENT_SOD_VIOLATION audit event is recorded and
     *    IllegalArgumentException is thrown if a violation is detected.
     * 3. Assigner privilege: the actor must hold a role that is the same as, or an ancestor
     *    of, each desired role in the role hierarchy. Actors holding the configured privileged
     *    assigner role are exempt from this check. A ROLE_ASSIGNMENT_HIERARCHY_DENIED audit
     *    event is recorded and AccessDeniedException is thrown for each denied role.
     *
     * @param actor         the authenticated actor requesting the assignment; must carry a
     *                      non-null userId
     * @param assignee      the BankingUser who will receive the roles
     * @param desiredRoles  the complete set of BankingRole entities to be assigned
     * @throws IllegalArgumentException if the assignee is ineligible for any of the
     *                                  requested roles, or if the combined role set violates
     *                                  a separation-of-duties rule
     * @throws AccessDeniedException    if the actor's own role set does not cover one or
     *                                  more of the desired roles, or if the actor's userId
     *                                  is null
     */
    public void validate(AuditActor actor, BankingUser assignee, Set<BankingRole> desiredRoles) {
        Set<String> roleNamesUpper = desiredRoles.stream().map(r -> r.getName().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));

        validateAssigneeEligibility(assignee, roleNamesUpper);

        if (rbacProperties.violatesSeparationOfDuties(roleNamesUpper)) {
            Set<String> pair = rbacProperties.firstSodViolation(roleNamesUpper);
            String detail = "Conflicting roles: " + String.join(" + ", pair);
            auditService.recordRoleAssignmentRejected(
                    SecurityAuditEventType.ROLE_ASSIGNMENT_SOD_VIOLATION,
                    assignee.getId(),
                    assignee.getUsername(),
                    detail,
                    actor);
            throw new IllegalArgumentException("Separation of duties: these roles cannot be combined: " + pair);
        }

        validateAssignerMayGrant(actor, assignee, desiredRoles);
    }

    /**
     * Validates that the assignee is eligible to receive the requested roles based on their user type.
     *
     * @param assignee the BankingUser who will receive the roles
     * @param roleNamesUpper the set of role names to be assigned, in uppercase
     */
    private void validateAssigneeEligibility(BankingUser assignee, Set<String> roleNamesUpper) {
        String customerRole = rbacProperties.getCustomerPortalRoleName().toUpperCase(Locale.ROOT);
        if (assignee.getUserType() == UserType.CUSTOMER) {
            if (roleNamesUpper.size() != 1 || !roleNamesUpper.contains(customerRole)) {
                throw new IllegalArgumentException("Customer users may only be assigned the " + customerRole + " role");
            }
            return;
        }

        if (roleNamesUpper.contains(customerRole)) {
            throw new IllegalArgumentException("The " + customerRole + " role is reserved for customer portal users");
        }
    }

    /**
     * Validates that the assigning actor holds sufficient privilege to grant the desired roles.
     *
     * @param actor the authenticated actor requesting the assignment
     * @param assignee the BankingUser who will receive the roles
     * @param desiredRoles the complete set of BankingRole entities to be assigned
     */
    private void validateAssignerMayGrant(AuditActor actor, BankingUser assignee, Set<BankingRole> desiredRoles) {
        if (desiredRoles.isEmpty()) {
            return;
        }
        UUID actorId = actor.userId();
        if (actorId == null) {
            throw new AccessDeniedException("Authenticated user id is required to assign roles");
        }

        BankingUser assigner = userRepository.findById(actorId)
                .orElseThrow(() -> new AccessDeniedException("Assigning user not found"));

        String privileged = rbacProperties.getPrivilegedAssignerRoleName().toUpperCase(Locale.ROOT);
        boolean isPrivilegedAssigner = assigner.getRoles().stream()
                .anyMatch(r -> r.getName().toUpperCase(Locale.ROOT).equals(privileged));
        if (isPrivilegedAssigner) {
            return;
        }

        Set<BankingRole> assignerRoles = assigner.getRoles();
        for (BankingRole target : desiredRoles) {
            boolean allowed = assignerRoles.stream().anyMatch(ar -> assignerCoversTarget(ar, target));
            if (!allowed) {
                String detail = "Cannot assign role " + target.getName() + " (assigner=" + actor.username() + ")";
                auditService.recordRoleAssignmentRejected(
                        SecurityAuditEventType.ROLE_ASSIGNMENT_HIERARCHY_DENIED,
                        assignee.getId(),
                        assignee.getUsername(),
                        detail,
                        actor);
                throw new AccessDeniedException("Not permitted to assign role: " + target.getName());
            }
        }
    }

    /**
     * Returns true if assignerRole is the same as targetRole or lies on any ancestor node
     * in targetRole's parent chain, meaning the assigner is at least as privileged.
     *
     * Walks the parent chain of targetRole by following getParentRole until a matching ID
     * is found or the chain is exhausted. Identity is compared by UUID rather than entity
     * equality to avoid requiring all roles to be from the same persistence context.
     *
     * @param assignerRole  the role held by the actor requesting the assignment
     * @param targetRole    the role being requested for the assignee
     * @return true if assignerRole equals targetRole or is an ancestor of targetRole, false
     *         otherwise
     */
    static boolean assignerCoversTarget(BankingRole assignerRole, BankingRole targetRole) {
        if (assignerRole.getId().equals(targetRole.getId())) {
            return true;
        }

        BankingRole p = targetRole.getParentRole();
        while (p != null) {
            if (p.getId().equals(assignerRole.getId())) {
                return true;
            }
            p = p.getParentRole();
        }
        return false;
    }
}
