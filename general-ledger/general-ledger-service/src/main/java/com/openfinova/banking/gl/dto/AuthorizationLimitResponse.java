package com.openfinova.banking.gl.dto;

import com.openfinova.banking.gl.api.entity.GLApprovalRole;
import com.openfinova.banking.gl.api.entity.GLTransactionSource;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for user's authorization limits.
 * Shows what transaction amounts the user can create and approve.
 */
public class AuthorizationLimitResponse {

    private UUID id;
    private GLApprovalRole approvalRole;
    private BigDecimal makerLimit;
    private BigDecimal approvalLimit;
    private String currency;
    private GLTransactionSource transactionSource;
    private Integer requiredApprovals;
    private Boolean isActive;

    // Constructors

    public AuthorizationLimitResponse() {
    }

    public AuthorizationLimitResponse(UUID id, GLApprovalRole approvalRole, BigDecimal makerLimit,
            BigDecimal approvalLimit, String currency, GLTransactionSource transactionSource, Integer requiredApprovals,
            Boolean isActive) {
        this.id = id;
        this.approvalRole = approvalRole;
        this.makerLimit = makerLimit;
        this.approvalLimit = approvalLimit;
        this.currency = currency;
        this.transactionSource = transactionSource;
        this.requiredApprovals = requiredApprovals;
        this.isActive = isActive;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GLApprovalRole getApprovalRole() {
        return approvalRole;
    }

    public void setApprovalRole(GLApprovalRole approvalRole) {
        this.approvalRole = approvalRole;
    }

    public BigDecimal getMakerLimit() {
        return makerLimit;
    }

    public void setMakerLimit(BigDecimal makerLimit) {
        this.makerLimit = makerLimit;
    }

    public BigDecimal getApprovalLimit() {
        return approvalLimit;
    }

    public void setApprovalLimit(BigDecimal approvalLimit) {
        this.approvalLimit = approvalLimit;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public GLTransactionSource getTransactionSource() {
        return transactionSource;
    }

    public void setTransactionSource(GLTransactionSource transactionSource) {
        this.transactionSource = transactionSource;
    }

    public Integer getRequiredApprovals() {
        return requiredApprovals;
    }

    public void setRequiredApprovals(Integer requiredApprovals) {
        this.requiredApprovals = requiredApprovals;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
