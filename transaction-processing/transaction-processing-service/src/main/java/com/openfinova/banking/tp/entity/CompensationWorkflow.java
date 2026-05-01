package com.openfinova.banking.tp.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.openfinova.banking.tp.api.entity.CompensationStepStatus;
import com.openfinova.banking.tp.api.entity.CompensationType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.tp.api.entity.CompensationStatus;
import com.openfinova.banking.tp.converter.CompensationStepListConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing a compensation workflow for handling transaction
 * failures.
 * Implements the Saga pattern for distributed transaction management with
 * Spring orchestration.
 */
@Entity
@Table(name = "compensation_workflows", indexes = {
        @Index(name = "idx_compensation_workflows_original", columnList = "original_transaction_id"),
        @Index(name = "idx_compensation_workflows_status", columnList = "workflow_status"),
        @Index(name = "idx_compensation_workflows_retry", columnList = "next_retry_at"),
        @Index(name = "idx_compensation_workflows_created", columnList = "created_at") })
public class CompensationWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_transaction_id", nullable = false)
    @NotNull(message = "Original transaction is required")
    private Transaction originalTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compensation_transaction_id")
    private Transaction compensationTransaction;

    @Column(name = "gl_reversal_transaction_id")
    private UUID glReversalTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_type", length = 20)
    private CompensationType compensationType;

    @Column(name = "compensation_amount", precision = 19, scale = 4)
    private BigDecimal compensationAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_status", nullable = false, length = 50)
    @NotNull(message = "Workflow status is required")
    private CompensationStatus workflowStatus = CompensationStatus.INITIATED;

    @Column(name = "failure_reason", nullable = false, columnDefinition = "TEXT")
    @NotNull(message = "Failure reason is required")
    private String failureReason;

    @Convert(converter = CompensationStepListConverter.class)
    @Column(name = "compensation_steps", columnDefinition = "jsonb")
    private List<CompensationStep> compensationSteps = new ArrayList<>();

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "escalation_reason", columnDefinition = "TEXT")
    private String escalationReason;

    @Column(name = "escalated_by", length = 100)
    private String escalatedBy;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    private Long version;

    // Constructors
    public CompensationWorkflow() {
    }

    public CompensationWorkflow(Transaction originalTransaction, String failureReason) {
        this.originalTransaction = originalTransaction;
        this.failureReason = failureReason;
    }

    // Business logic methods

    /**
     * Transitions the workflow to a new status with validation
     *
     * @param newStatus the target status
     * @param context   additional context for the transition
     * @throws IllegalStateException if transition is not valid
     */
    public void transitionTo(CompensationStatus newStatus, String context) {
        if (!workflowStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid workflow state transition from %s to %s", workflowStatus, newStatus));
        }

        this.workflowStatus = newStatus;

        // Update timing fields based on new status
        switch (newStatus) {
            case INITIATED, IN_PROGRESS -> {
                // No specific timing update for these states
            }
            case COMPLETED -> this.completedAt = LocalDateTime.now();
            case ESCALATED -> {
                this.escalatedAt = LocalDateTime.now();
                this.escalationReason = context;
            }
            case FAILED -> calculateNextRetryTime();
            case CANCELLED -> this.completedAt = LocalDateTime.now();
        }
    }

    /**
     * Adds a compensation step to the workflow
     *
     * @param step the compensation step to add
     */
    public void addCompensationStep(CompensationStep step) {
        if (compensationSteps == null) {
            compensationSteps = new ArrayList<>();
        }
        compensationSteps.add(step);
    }

    /**
     * Increments the retry count and calculates next retry time using exponential
     * backoff
     */
    public void incrementRetryCount() {
        this.retryCount++;
        calculateNextRetryTime();
    }

    /**
     * Calculates the next retry time using exponential backoff
     * Base delay: 1 minute, max delay: 60 minutes
     */
    private void calculateNextRetryTime() {
        if (retryCount >= maxRetries) {
            this.nextRetryAt = null;
            return;
        }

        // Exponential backoff: 1min, 2min, 4min, 8min, etc., capped at 60min
        long delayMinutes = Math.min(60, (long) Math.pow(2, retryCount));
        this.nextRetryAt = LocalDateTime.now().plusMinutes(delayMinutes);
    }

    /**
     * Checks if the workflow can be retried
     *
     * @return true if retry is possible
     */
    public boolean canRetry() {
        return workflowStatus.canRetry() && retryCount < maxRetries;
    }

    /**
     * Checks if the workflow is ready for retry
     *
     * @return true if ready for retry
     */
    public boolean isReadyForRetry() {
        return canRetry() && nextRetryAt != null && LocalDateTime.now().isAfter(nextRetryAt);
    }

    /**
     * Resets the workflow for retry
     */
    public void resetForRetry() {
        this.workflowStatus = CompensationStatus.INITIATED;
        this.nextRetryAt = null;

        // Reset failed steps for retry
        if (compensationSteps != null) {
            compensationSteps.stream().filter(step -> step.getStatus().isFailed())
                    .forEach(CompensationStep::resetForRetry);
        }
    }

    /**
     * Escalates the workflow to manual review
     *
     * @param reason      the reason for escalation
     * @param escalatedBy who escalated the workflow
     */
    public void escalate(String reason, String escalatedBy) {
        transitionTo(CompensationStatus.ESCALATED, reason);
        this.escalatedBy = escalatedBy;
    }

    /**
     * Gets the current step being processed
     *
     * @return the current step or null if none
     */
    public CompensationStep getCurrentStep() {
        if (compensationSteps == null || compensationSteps.isEmpty()) {
            return null;
        }

        return compensationSteps.stream().filter(step -> step.getStatus() == CompensationStepStatus.IN_PROGRESS)
                .findFirst().orElse(null);
    }

    /**
     * Gets the next pending step to execute
     *
     * @return the next step or null if none
     */
    public CompensationStep getNextPendingStep() {
        if (compensationSteps == null || compensationSteps.isEmpty()) {
            return null;
        }

        return compensationSteps.stream().filter(step -> step.getStatus() == CompensationStepStatus.PENDING)
                .min((s1, s2) -> Integer.compare(s1.getOrder(), s2.getOrder())).orElse(null);
    }

    /**
     * Checks if all steps are completed
     *
     * @return true if all steps are in terminal successful state
     */
    public boolean areAllStepsCompleted() {
        if (compensationSteps == null || compensationSteps.isEmpty()) {
            return false;
        }

        return compensationSteps.stream().allMatch(
                step -> step.getStatus().isTerminal()
                        && (step.getStatus().isSuccessful() || step.getStatus() == CompensationStepStatus.SKIPPED));
    }

    /**
     * Checks if any step has failed
     *
     * @return true if any step is in failed state
     */
    public boolean hasFailedSteps() {
        if (compensationSteps == null || compensationSteps.isEmpty()) {
            return false;
        }

        return compensationSteps.stream().anyMatch(step -> step.getStatus().isFailed());
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Transaction getOriginalTransaction() {
        return originalTransaction;
    }

    public void setOriginalTransaction(Transaction originalTransaction) {
        this.originalTransaction = originalTransaction;
    }

    public Transaction getCompensationTransaction() {
        return compensationTransaction;
    }

    public void setCompensationTransaction(Transaction compensationTransaction) {
        this.compensationTransaction = compensationTransaction;
    }

    public CompensationStatus getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(CompensationStatus workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public UUID getGlReversalTransactionId() {
        return glReversalTransactionId;
    }

    public void setGlReversalTransactionId(UUID glReversalTransactionId) {
        this.glReversalTransactionId = glReversalTransactionId;
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

    public List<CompensationStep> getCompensationSteps() {
        return compensationSteps;
    }

    public void setCompensationSteps(List<CompensationStep> compensationSteps) {
        this.compensationSteps = compensationSteps;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getEscalationReason() {
        return escalationReason;
    }

    public void setEscalationReason(String escalationReason) {
        this.escalationReason = escalationReason;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getVersion() {
        return version;
    }

    // equals, hashCode, and toString

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CompensationWorkflow that))
            return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CompensationWorkflow{" + "id=" + id + ", workflowStatus=" + workflowStatus + ", failureReason='"
                + failureReason + '\'' + ", retryCount=" + retryCount + ", maxRetries=" + maxRetries + ", stepsCount="
                + (compensationSteps != null ? compensationSteps.size() : 0) + '}';
    }
}