package com.openfinova.banking.tp.api.dto;

import com.openfinova.banking.tp.api.entity.CompensationStatus;
import com.openfinova.banking.tp.api.entity.CompensationType;
import com.openfinova.banking.tp.api.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for summarizing individual compensation workflows.
 * Provides key information about a workflow without full entity details.
 */
public class CompensationWorkflowSummary {

    private UUID workflowId;
    private UUID originalTransactionId;
    private TransactionType transactionType;
    private CompensationStatus status;
    private CompensationType compensationType;
    private BigDecimal compensationAmount;
    private String failureReason;
    private int totalSteps;
    private int completedSteps;
    private int failedSteps;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Duration processingDuration;
    private String escalatedBy;
    private LocalDateTime escalatedAt;
    private String escalationReason;

    // Default constructor
    public CompensationWorkflowSummary() {
    }

    // Constructor with essential fields
    public CompensationWorkflowSummary(UUID workflowId, UUID originalTransactionId, CompensationStatus status,
            LocalDateTime createdAt) {
        this.workflowId = workflowId;
        this.originalTransactionId = originalTransactionId;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public UUID getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(UUID workflowId) {
        this.workflowId = workflowId;
    }

    public UUID getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(UUID originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public CompensationStatus getStatus() {
        return status;
    }

    public void setStatus(CompensationStatus status) {
        this.status = status;
    }

    public CompensationType getCompensationType() {
        return compensationType;
    }

    public void setCompensationType(CompensationType compensationType) {
        this.compensationType = compensationType;
    }

    public BigDecimal getCompensationAmount() {
        return compensationAmount;
    }

    public void setCompensationAmount(BigDecimal compensationAmount) {
        this.compensationAmount = compensationAmount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public int getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(int completedSteps) {
        this.completedSteps = completedSteps;
    }

    public int getFailedSteps() {
        return failedSteps;
    }

    public void setFailedSteps(int failedSteps) {
        this.failedSteps = failedSteps;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Duration getProcessingDuration() {
        return processingDuration;
    }

    public void setProcessingDuration(Duration processingDuration) {
        this.processingDuration = processingDuration;
    }

    public String getEscalatedBy() {
        return escalatedBy;
    }

    public void setEscalatedBy(String escalatedBy) {
        this.escalatedBy = escalatedBy;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public String getEscalationReason() {
        return escalationReason;
    }

    public void setEscalationReason(String escalationReason) {
        this.escalationReason = escalationReason;
    }

    @Override
    public String toString() {
        return "CompensationWorkflowSummary{" + "workflowId=" + workflowId + ", originalTransactionId="
                + originalTransactionId + ", status=" + status + ", compensationType=" + compensationType
                + ", totalSteps=" + totalSteps + ", completedSteps=" + completedSteps + ", retryCount=" + retryCount
                + '}';
    }
}