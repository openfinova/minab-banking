package com.openfinova.banking.gl.api.dto;

import com.openfinova.banking.gl.api.entity.BalanceType;
import com.openfinova.banking.gl.api.entity.GLAccountStatus;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "GL Account response")
public class GLAccountResponse {

    @Schema(description = "Account unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Account code", example = "1000")
    private String code;

    @Schema(description = "Account name", example = "Cash in Bank - USD")
    private String name;

    @Schema(description = "Account type", example = "ASSET")
    private GLAccountType type;

    @Schema(description = "Three-letter ISO currency code", example = "USD")
    private String currency;

    @Schema(description = "Parent account ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID parentId;

    @Schema(description = "Parent account code", example = "1000")
    private String parentCode;

    @Schema(description = "Parent account name", example = "Cash Accounts")
    private String parentName;

    @Schema(description = "Account status", example = "ACTIVE")
    private GLAccountStatus status;

    @Schema(description = "Normal balance type", example = "DEBIT")
    private BalanceType normalBalance;

    @Schema(description = "Account description", example = "Primary operating account for USD transactions")
    private String description;

    @Schema(description = "Additional metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00Z")
    private Instant createdAt;

    @Schema(description = "User who created the account", example = "admin")
    private String createdBy;

    @Schema(description = "Whether this account has child accounts")
    private boolean hasChildren;

    @Schema(description = "Whether this is a contra account")
    private boolean contra;

    // Constructors
    public GLAccountResponse() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public GLAccountStatus getStatus() {
        return status;
    }

    public void setStatus(GLAccountStatus status) {
        this.status = status;
    }

    public BalanceType getNormalBalance() {
        return normalBalance;
    }

    public void setNormalBalance(BalanceType normalBalance) {
        this.normalBalance = normalBalance;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public boolean isContra() {
        return contra;
    }

    public void setContra(boolean contra) {
        this.contra = contra;
    }
}
