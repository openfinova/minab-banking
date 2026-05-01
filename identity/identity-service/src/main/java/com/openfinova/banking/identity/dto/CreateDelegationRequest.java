package com.openfinova.banking.identity.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Register a delegation of authority between two staff users")
public class CreateDelegationRequest {

    @NotNull
    private UUID delegatedFromUserId;

    @NotNull
    private UUID delegatedToUserId;

    @Size(max = 3)
    private String currency;

    private BigDecimal approvalLimit;

    @NotNull
    @Size(max = 80)
    private String transactionType;

    @NotNull
    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    @Size(max = 30)
    private String actingGlApprovalRole;

    public UUID getDelegatedFromUserId() {
        return delegatedFromUserId;
    }

    public void setDelegatedFromUserId(UUID v) {
        this.delegatedFromUserId = v;
    }

    public UUID getDelegatedToUserId() {
        return delegatedToUserId;
    }

    public void setDelegatedToUserId(UUID v) {
        this.delegatedToUserId = v;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String v) {
        this.currency = v != null ? v.strip().toUpperCase() : null;
    }

    public BigDecimal getApprovalLimit() {
        return approvalLimit;
    }

    public void setApprovalLimit(BigDecimal v) {
        this.approvalLimit = v;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String v) {
        this.transactionType = v != null ? v.strip() : null;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime v) {
        this.validFrom = v;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime v) {
        this.validUntil = v;
    }

    public String getActingGlApprovalRole() {
        return actingGlApprovalRole;
    }

    public void setActingGlApprovalRole(String v) {
        this.actingGlApprovalRole = v != null ? v.strip() : null;
    }
}
