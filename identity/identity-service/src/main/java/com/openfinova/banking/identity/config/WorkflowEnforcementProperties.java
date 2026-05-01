package com.openfinova.banking.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Controls which sensitive identity operations require a pre-approved
 * {@code ApprovalWorkflowInstance} before they are permitted to execute.
 *
 * <p>
 * Defaults to {@code true} for both settings. Disable only in non-production profiles where the
 * approval overhead is impractical (e.g. automated tests).
 */
@ConfigurationProperties(prefix = "identity.workflow-enforcement")
public class WorkflowEnforcementProperties {

    /**
     * When {@code true}, mutating role permissions (setPermissions, addPermissions,
     * removePermissions) and deleting a role require an APPROVED workflow of type
     * {@code ROLE_PERMISSION_CHANGE} scoped to the role ID to exist first.
     */
    private boolean requireApprovalForRolePermissionChanges = true;

    /**
     * When {@code true}, assigning roles to a user requires an APPROVED workflow of type
     * {@code USER_ROLE_ASSIGNMENT} scoped to the user ID to exist first.
     */
    private boolean requireApprovalForRoleAssignment = true;

    public boolean isRequireApprovalForRolePermissionChanges() {
        return requireApprovalForRolePermissionChanges;
    }

    public void setRequireApprovalForRolePermissionChanges(boolean requireApprovalForRolePermissionChanges) {
        this.requireApprovalForRolePermissionChanges = requireApprovalForRolePermissionChanges;
    }

    public boolean isRequireApprovalForRoleAssignment() {
        return requireApprovalForRoleAssignment;
    }

    public void setRequireApprovalForRoleAssignment(boolean requireApprovalForRoleAssignment) {
        this.requireApprovalForRoleAssignment = requireApprovalForRoleAssignment;
    }
}
