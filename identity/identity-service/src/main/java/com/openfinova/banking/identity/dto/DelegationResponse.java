package com.openfinova.banking.identity.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.identity.entity.DelegationOfAuthority;
import com.openfinova.banking.identity.entity.DelegationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stored delegation of authority")
public class DelegationResponse {

    private UUID id;
    private UUID delegatedFromUserId;
    private String delegatedFromUsername;
    private UUID delegatedToUserId;
    private String delegatedToUsername;
    private BigDecimal approvalLimit;
    private String currency;
    private String transactionType;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private DelegationStatus status;
    private String actingGlApprovalRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DelegationResponse from(DelegationOfAuthority d) {
        DelegationResponse r = new DelegationResponse();
        r.id = d.getId();
        r.delegatedFromUserId = d.getDelegatedFrom().getId();
        r.delegatedFromUsername = d.getDelegatedFrom().getUsername();
        r.delegatedToUserId = d.getDelegatedTo().getId();
        r.delegatedToUsername = d.getDelegatedTo().getUsername();
        r.approvalLimit = d.getApprovalLimit();
        r.currency = d.getCurrency();
        r.transactionType = d.getTransactionType();
        r.validFrom = d.getValidFrom();
        r.validUntil = d.getValidUntil();
        r.status = d.getStatus();
        r.actingGlApprovalRole = d.getActingGlApprovalRole();
        r.createdAt = d.getCreatedAt();
        r.updatedAt = d.getUpdatedAt();
        return r;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDelegatedFromUserId() {
        return delegatedFromUserId;
    }

    public String getDelegatedFromUsername() {
        return delegatedFromUsername;
    }

    public UUID getDelegatedToUserId() {
        return delegatedToUserId;
    }

    public String getDelegatedToUsername() {
        return delegatedToUsername;
    }

    public BigDecimal getApprovalLimit() {
        return approvalLimit;
    }

    public String getCurrency() {
        return currency;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public DelegationStatus getStatus() {
        return status;
    }

    public String getActingGlApprovalRole() {
        return actingGlApprovalRole;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
