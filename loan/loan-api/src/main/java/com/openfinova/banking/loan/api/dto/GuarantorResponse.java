package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.GuarantorStatus;
import com.openfinova.banking.loan.api.entity.GuarantorType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for guarantors.
 */
public class GuarantorResponse {

    private UUID id;
    private UUID loanAccountId;
    private UUID loanApplicationId;
    private UUID customerId;
    private GuarantorType guarantorType;
    private BigDecimal guaranteedAmount;
    private BigDecimal guaranteePercentage;
    private GuarantorStatus status;
    private String remarks;
    private Instant verifiedDate;
    private String verifiedBy;
    private Instant releasedDate;
    private String releasedBy;
    private Instant removedDate;
    private String removedBy;
    private String removalReason;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLoanAccountId() {
        return loanAccountId;
    }

    public void setLoanAccountId(UUID loanAccountId) {
        this.loanAccountId = loanAccountId;
    }

    public UUID getLoanApplicationId() {
        return loanApplicationId;
    }

    public void setLoanApplicationId(UUID loanApplicationId) {
        this.loanApplicationId = loanApplicationId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public GuarantorType getGuarantorType() {
        return guarantorType;
    }

    public void setGuarantorType(GuarantorType guarantorType) {
        this.guarantorType = guarantorType;
    }

    public BigDecimal getGuaranteedAmount() {
        return guaranteedAmount;
    }

    public void setGuaranteedAmount(BigDecimal guaranteedAmount) {
        this.guaranteedAmount = guaranteedAmount;
    }

    public BigDecimal getGuaranteePercentage() {
        return guaranteePercentage;
    }

    public void setGuaranteePercentage(BigDecimal guaranteePercentage) {
        this.guaranteePercentage = guaranteePercentage;
    }

    public GuarantorStatus getStatus() {
        return status;
    }

    public void setStatus(GuarantorStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Instant getVerifiedDate() {
        return verifiedDate;
    }

    public void setVerifiedDate(Instant verifiedDate) {
        this.verifiedDate = verifiedDate;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public Instant getReleasedDate() {
        return releasedDate;
    }

    public void setReleasedDate(Instant releasedDate) {
        this.releasedDate = releasedDate;
    }

    public String getReleasedBy() {
        return releasedBy;
    }

    public void setReleasedBy(String releasedBy) {
        this.releasedBy = releasedBy;
    }

    public Instant getRemovedDate() {
        return removedDate;
    }

    public void setRemovedDate(Instant removedDate) {
        this.removedDate = removedDate;
    }

    public String getRemovedBy() {
        return removedBy;
    }

    public void setRemovedBy(String removedBy) {
        this.removedBy = removedBy;
    }

    public String getRemovalReason() {
        return removalReason;
    }

    public void setRemovalReason(String removalReason) {
        this.removalReason = removalReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
