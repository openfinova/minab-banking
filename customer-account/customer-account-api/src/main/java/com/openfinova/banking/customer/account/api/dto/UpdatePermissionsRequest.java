package com.openfinova.banking.customer.account.api.dto;

import com.openfinova.banking.customer.account.api.entity.AccountPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

@Schema(description = "Request to update relationship permissions")
public class UpdatePermissionsRequest {

    @NotEmpty(message = "Permissions set cannot be empty")
    @Schema(description = "Set of permissions", required = true)
    private Set<AccountPermission> permissions;

    // Getters and setters
    public Set<AccountPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<AccountPermission> permissions) {
        this.permissions = permissions;
    }
}
