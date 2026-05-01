package com.openfinova.banking.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Update role display metadata")
public class UpdateRoleRequest {

    @Size(max = 120)
    private String displayName;

    @Size(max = 500)
    private String description;

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
}
