package com.openfinova.banking.gl.api.dto;

import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request to configure an operational GL account")
public class ConfigureOperationalAccountRequest {

    @NotNull(message = "Operational account type is required")
    @Schema(description = "Operational account type", example = "FEE_INCOME")
    private OperationalGLAccountType type;

    @NotNull(message = "GL account ID is required")
    @Schema(description = "GL account ID to use for this operational account")
    private UUID glAccountId;

    @NotBlank(message = "Created by is required")
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    @Schema(description = "User creating the configuration", example = "admin")
    private String createdBy;

    // Constructors
    public ConfigureOperationalAccountRequest() {
    }

    // Getters and Setters
    public OperationalGLAccountType getType() {
        return type;
    }

    public void setType(OperationalGLAccountType type) {
        this.type = type;
    }

    public UUID getGlAccountId() {
        return glAccountId;
    }

    public void setGlAccountId(UUID glAccountId) {
        this.glAccountId = glAccountId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
