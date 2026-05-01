package com.openfinova.banking.gl.api.dto;

import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Operational GL Account Configuration response")
public class OperationalGLConfigResponse {

    @Schema(description = "Configuration unique identifier")
    private UUID id;

    @Schema(description = "Operational account type")
    private OperationalGLAccountType type;

    @Schema(description = "GL account ID")
    private UUID glAccountId;

    @Schema(description = "GL account code")
    private String glAccountCode;

    @Schema(description = "GL account name")
    private String glAccountName;

    @Schema(description = "Configuration status")
    private boolean active;

    @Schema(description = "User who created the configuration")
    private String createdBy;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    // Constructors
    public OperationalGLConfigResponse() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public String getGlAccountCode() {
        return glAccountCode;
    }

    public void setGlAccountCode(String glAccountCode) {
        this.glAccountCode = glAccountCode;
    }

    public String getGlAccountName() {
        return glAccountName;
    }

    public void setGlAccountName(String glAccountName) {
        this.glAccountName = glAccountName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
