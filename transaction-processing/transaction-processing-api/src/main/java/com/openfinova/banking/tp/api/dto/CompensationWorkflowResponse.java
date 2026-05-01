package com.openfinova.banking.tp.api.dto;

import com.openfinova.banking.tp.api.entity.CompensationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Compensation workflow response")
public class CompensationWorkflowResponse {

    @Schema(description = "Workflow ID")
    private UUID id;

    @Schema(description = "Transaction ID")
    private UUID transactionId;

    @Schema(description = "Workflow status")
    private CompensationStatus status;

    @Schema(description = "Total steps")
    private Integer totalSteps;

    @Schema(description = "Completed steps")
    private Integer completedSteps;

    @Schema(description = "Failed steps")
    private Integer failedSteps;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Completion timestamp")
    private LocalDateTime completedAt;

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public CompensationStatus getStatus() {
        return status;
    }

    public void setStatus(CompensationStatus status) {
        this.status = status;
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }

    public Integer getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(Integer completedSteps) {
        this.completedSteps = completedSteps;
    }

    public Integer getFailedSteps() {
        return failedSteps;
    }

    public void setFailedSteps(Integer failedSteps) {
        this.failedSteps = failedSteps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
