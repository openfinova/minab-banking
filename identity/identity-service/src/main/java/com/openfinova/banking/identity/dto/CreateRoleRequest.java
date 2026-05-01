package com.openfinova.banking.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Create a custom role with an initial permission set")
public class CreateRoleRequest {

    @NotBlank
    @Size(max = 60)
    private String name;

    @Size(max = 120)
    private String displayName;

    @Size(max = 500)
    private String description;

    private Set<String> permissions = Set.of();

    public String getName() {
        return name;
    }

    public void setName(String v) {
        this.name = v;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String v) {
        this.displayName = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> v) {
        this.permissions = v;
    }
}
