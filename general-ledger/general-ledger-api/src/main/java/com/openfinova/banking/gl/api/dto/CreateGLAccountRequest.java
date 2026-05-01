package com.openfinova.banking.gl.api.dto;

import java.util.Map;
import java.util.UUID;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.gl.api.entity.GLAccountType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a new GL account")
public class CreateGLAccountRequest {
    @NotBlank(message = "GLAccount code is required")
    @Size(max = 50, message = "GLAccount code must not exceed 50 characters")
    @Schema(description = "Unique account code", example = "1000")
    private String code;

    @NotBlank(message = "GLAccount name is required")
    @Size(max = 255, message = "GLAccount name must not exceed 255 characters")
    @Schema(description = "Account name", example = "Cash in Bank - USD")
    private String name;

    @NotNull(message = "GLAccount type is required")
    @Schema(description = "Account type", example = "ASSET")
    private GLAccountType type;

    @NotBlank(message = "Currency is required")
    @ValidCurrency
    @Schema(description = "Three-letter ISO currency code", example = "USD")
    private String currency;

    @Schema(description = "Parent account ID for hierarchical structure", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID parentId;

    @NotBlank(message = "Created by is required")
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    @Schema(description = "User creating the account", example = "admin")
    private String createdBy;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Account description", example = "Primary operating account for USD transactions")
    private String description;

    @Schema(description = "Additional metadata as key-value pairs")
    private Map<String, Object> metadata;

    @Schema(description = "Whether this is a contra account (normal balance opposite to its type). "
            + "e.g. Allowance for Credit Losses is a contra-ASSET with a CREDIT normal balance.", example = "false")
    private boolean contra = false;

    public CreateGLAccountRequest() {
    }

    public CreateGLAccountRequest(String code, String name, GLAccountType type, String currency, UUID parentId,
            String createdBy, String description, Map<String, Object> metadata) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.parentId = parentId;
        this.createdBy = createdBy;
        this.description = description;
        this.metadata = metadata;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GLAccountType getType() {
        return type;
    }

    public void setType(GLAccountType type) {
        this.type = type;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public boolean isContra() {
        return contra;
    }

    public void setContra(boolean contra) {
        this.contra = contra;
    }

}
