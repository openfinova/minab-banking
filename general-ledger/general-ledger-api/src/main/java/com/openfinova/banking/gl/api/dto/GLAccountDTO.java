package com.openfinova.banking.gl.api.dto;

import com.openfinova.banking.gl.api.entity.BalanceType;
import com.openfinova.banking.gl.api.entity.GLAccountStatus;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO for GL Account information exposed to external modules.
 * Contains only the essential fields needed for cross-module communication.
 */
@Schema(description = "General Ledger account details")
public class GLAccountDTO {
    @Schema(description = "Unique account identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Account code", example = "1000")
    private String code;

    @Schema(description = "Account name", example = "Cash in Bank - USD")
    private String name;

    @Schema(description = "Account type", example = "ASSET")
    private GLAccountType type;

    @Schema(description = "Account currency", example = "USD")
    private String currency;

    @Schema(description = "Account status", example = "ACTIVE")
    private GLAccountStatus status;

    @Schema(description = "Normal balance direction", example = "DEBIT")
    private BalanceType normalBalance;

    @Schema(description = "Parent account ID for hierarchical structure", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID parentId;

    @Schema(description = "Account description", example = "Primary operating account for USD transactions")
    private String description;

    public GLAccountDTO() {
    }

    public GLAccountDTO(UUID id, String code, String name, GLAccountType type, String currency, GLAccountStatus status,
            BalanceType normalBalance, UUID parentId, String description) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.status = status;
        this.normalBalance = normalBalance;
        this.parentId = parentId;
        this.description = description;
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

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
