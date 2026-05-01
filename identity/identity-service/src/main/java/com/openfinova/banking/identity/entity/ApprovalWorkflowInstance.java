package com.openfinova.banking.identity.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Generic approval chain for a logical resource (identified by type + id string). Downstream
 * domains may mirror correlation ids here for supervisory workflows.
 */
@Entity
@Table(name = "identity_approval_workflows", indexes = {
        @Index(name = "idx_appr_wf_resource", columnList = "resource_type,resource_id"),
        @Index(name = "idx_appr_wf_status", columnList = "status") })
public class ApprovalWorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @NotNull
    @Size(max = 80)
    @Column(name = "resource_type", nullable = false, length = 80)
    private String resourceType;

    @NotNull
    @Size(max = 120)
    @Column(name = "resource_id", nullable = false, length = 120)
    private String resourceId;

    /** The user who initiated (submitted) this workflow — the "maker" in a maker-checker flow. */
    @Column(name = "initiator_id")
    private UUID initiatorId;

    @Size(max = 80)
    @Column(name = "initiator_username", length = 80)
    private String initiatorUsername;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalWorkflowStatus status = ApprovalWorkflowStatus.PENDING;

    @Size(max = 500)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<ApprovalWorkflowStep> steps = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ApprovalWorkflowInstance() {
    }

    public ApprovalWorkflowInstance(String resourceType, String resourceId) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String v) {
        this.resourceType = v;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String v) {
        this.resourceId = v;
    }

    public UUID getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(UUID v) {
        this.initiatorId = v;
    }

    public String getInitiatorUsername() {
        return initiatorUsername;
    }

    public void setInitiatorUsername(String v) {
        this.initiatorUsername = v;
    }

    public ApprovalWorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalWorkflowStatus v) {
        this.status = v;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String v) {
        this.rejectionReason = v;
    }

    public List<ApprovalWorkflowStep> getSteps() {
        return steps;
    }

    public void setSteps(List<ApprovalWorkflowStep> v) {
        this.steps = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void addStep(ApprovalWorkflowStep step) {
        step.setWorkflow(this);
        steps.add(step);
    }
}
