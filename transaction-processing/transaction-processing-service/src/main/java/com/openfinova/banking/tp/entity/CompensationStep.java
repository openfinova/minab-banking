package com.openfinova.banking.tp.entity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.openfinova.banking.tp.api.entity.CompensationStepStatus;
import com.openfinova.banking.tp.api.entity.CompensationStepType;

/**
 * Represents a single step in a compensation workflow.
 * Each step defines an action to be taken to compensate for a failed
 * transaction.
 *
 * Requirements addressed:
 * - Maintain compensation workflow state for each transaction
 * - Create reversal transactions in GL with appropriate audit trails
 * - Log all compensation activities with detailed error context
 */
public class CompensationStep {

    private String stepId;
    private CompensationStepType stepType;
    private String description;
    private Map<String, Object> parameters;
    private int order;
    private CompensationStepStatus status;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private int retryCount;
    private Map<String, Object> result;

    // Default constructor for frameworks
    public CompensationStep() {
        this.status = CompensationStepStatus.PENDING;
        this.retryCount = 0;
    }

    @JsonCreator
    public CompensationStep(@JsonProperty("stepId") String stepId,
            @JsonProperty("stepType") CompensationStepType stepType, @JsonProperty("description") String description,
            @JsonProperty("parameters") Map<String, Object> parameters, @JsonProperty("order") int order) {
        this.stepId = stepId;
        this.stepType = stepType;
        this.description = description;
        this.parameters = parameters;
        this.order = order;
        this.status = CompensationStepStatus.PENDING;
        this.retryCount = 0;
    }

    /**
     * Marks the step as started
     */
    public void markStarted() {
        this.status = CompensationStepStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * Marks the step as completed successfully
     *
     * @param result the result of the step execution
     */
    public void markCompleted(Map<String, Object> result) {
        this.status = CompensationStepStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.result = result;
        this.errorMessage = null;
    }

    /**
     * Marks the step as failed
     *
     * @param errorMessage the error message
     */
    public void markFailed(String errorMessage) {
        this.status = CompensationStepStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    /**
     * Marks the step as skipped
     *
     * @param reason the reason for skipping
     */
    public void markSkipped(String reason) {
        this.status = CompensationStepStatus.SKIPPED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = reason;
    }

    /**
     * Resets the step for retry
     */
    public void resetForRetry() {
        this.status = CompensationStepStatus.PENDING;
        this.startedAt = null;
        this.completedAt = null;
        this.result = null;
        // Keep errorMessage and retryCount for audit trail
    }

    // Getters
    public String getStepId() {
        return stepId;
    }

    public String getId() {
        return stepId;
    }

    public CompensationStepType getStepType() {
        return stepType;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public int getOrder() {
        return order;
    }

    public CompensationStepStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    // Setters for JSON deserialization
    @JsonProperty("status")
    public void setStatus(CompensationStepStatus status) {
        this.status = status;
    }

    @JsonProperty("errorMessage")
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @JsonProperty("startedAt")
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    @JsonProperty("completedAt")
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @JsonProperty("retryCount")
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    @JsonProperty("result")
    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

    // Additional setters for mutable fields
    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public void setStepType(CompensationStepType stepType) {
        this.stepType = stepType;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setFailureReason(String failureReason) {
        this.errorMessage = failureReason;
    }

    public void setFailedAt(LocalDateTime failedAt) {
        this.completedAt = failedAt;
    }

    public void setExecutionDetails(String executionDetails) {
        if (this.result == null) {
            this.result = new java.util.HashMap<>();
        }
        this.result.put("executionDetails", executionDetails);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CompensationStep that))
            return false;
        return Objects.equals(stepId, that.stepId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stepId);
    }

    @Override
    public String toString() {
        return "CompensationStep{" + "stepId='" + stepId + '\'' + ", stepType='" + stepType + '\'' + ", description='"
                + description + '\'' + ", order=" + order + ", status=" + status + ", retryCount=" + retryCount + '}';
    }
}