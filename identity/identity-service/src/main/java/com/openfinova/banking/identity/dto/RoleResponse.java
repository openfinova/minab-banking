package com.openfinova.banking.identity.dto;

import com.openfinova.banking.identity.api.permission.BankingPermission;
import com.openfinova.banking.identity.entity.BankingRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Schema(description = "Role with resolved permission authorities")
public class RoleResponse {

    private UUID id;
    private String name;
    private String displayName;
    private String description;
    private boolean systemRole;
    private boolean enabled;
    private Set<String> permissions;
    /** More-privileged parent role in the assignment hierarchy; null for roots such as ADMIN. */
    private String parentRoleName;

    public static RoleResponse from(BankingRole role) {
        RoleResponse r = new RoleResponse();
        r.id = role.getId();
        r.name = role.getName();
        r.displayName = role.getDisplayName();
        r.description = role.getDescription();
        r.systemRole = role.isSystemRole();
        r.enabled = role.isEnabled();
        r.permissions = role.getPermissions().stream().map(BankingPermission::getAuthority).collect(Collectors.toSet());
        r.parentRoleName = role.getParentRole() != null ? role.getParentRole().getName() : null;
        return r;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSystemRole() {
        return systemRole;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public String getParentRoleName() {
        return parentRoleName;
    }
}
