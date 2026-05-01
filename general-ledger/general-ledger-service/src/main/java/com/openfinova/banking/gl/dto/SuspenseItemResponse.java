package com.openfinova.banking.gl.dto;

import com.openfinova.banking.gl.api.entity.AgingBracket;
import com.openfinova.banking.gl.api.entity.SuspenseReasonCode;
import com.openfinova.banking.gl.api.entity.SuspenseStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * API response for suspense item details.
 * Used by controllers in the service module.
 */
public class SuspenseItemResponse {

    private UUID id;
    private UUID glTransactionId;
    private String transactionReference;
    private BigDecimal amount;
    private String currency;
    private SuspenseStatus status;
    private SuspenseReasonCode reasonCode;
    private String description;
    private String sourceSystem;
    private String externalReference;
    private LocalDate postingDate;
    private Long ageDays;
    private AgingBracket agingBracket;
    private String assignedTo;
    private String investigationNotes;
    private UUID targetAccountId;
    private String targetAccountNumber;
    private String targetAccountName;
    private LocalDate clearedDate;
    private String clearedBy;
    private UUID clearingTransactionId;
    private Boolean requiresAMLReview;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;

    // Constructors

    public SuspenseItemResponse() {
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getGlTransactionId() {
        return glTransactionId;
    }

    public void setGlTransactionId(UUID glTransactionId) {
        this.glTransactionId = glTransactionId;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public SuspenseStatus getStatus() {
        return status;
    }

    public void setStatus(SuspenseStatus status) {
        this.status = status;
    }

    public SuspenseReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(SuspenseReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public Long getAgeDays() {
        return ageDays;
    }

    public void setAgeDays(Long ageDays) {
        this.ageDays = ageDays;
    }

    public AgingBracket getAgingBracket() {
        return agingBracket;
    }

    public void setAgingBracket(AgingBracket agingBracket) {
        this.agingBracket = agingBracket;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getInvestigationNotes() {
        return investigationNotes;
    }

    public void setInvestigationNotes(String investigationNotes) {
        this.investigationNotes = investigationNotes;
    }

    public UUID getTargetAccountId() {
        return targetAccountId;
    }

    public void setTargetAccountId(UUID targetAccountId) {
        this.targetAccountId = targetAccountId;
    }

    public String getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public void setTargetAccountNumber(String targetAccountNumber) {
        this.targetAccountNumber = targetAccountNumber;
    }

    public String getTargetAccountName() {
        return targetAccountName;
    }

    public void setTargetAccountName(String targetAccountName) {
        this.targetAccountName = targetAccountName;
    }

    public LocalDate getClearedDate() {
        return clearedDate;
    }

    public void setClearedDate(LocalDate clearedDate) {
        this.clearedDate = clearedDate;
    }

    public String getClearedBy() {
        return clearedBy;
    }

    public void setClearedBy(String clearedBy) {
        this.clearedBy = clearedBy;
    }

    public UUID getClearingTransactionId() {
        return clearingTransactionId;
    }

    public void setClearingTransactionId(UUID clearingTransactionId) {
        this.clearingTransactionId = clearingTransactionId;
    }

    public Boolean getRequiresAMLReview() {
        return requiresAMLReview;
    }

    public void setRequiresAMLReview(Boolean requiresAMLReview) {
        this.requiresAMLReview = requiresAMLReview;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String toString() {
        return "SuspenseItemResponse{" + "id=" + id + ", transactionReference='" + transactionReference + '\''
                + ", amount=" + amount + ", currency='" + currency + '\'' + ", status=" + status + ", ageDays="
                + ageDays + '}';
    }
}
