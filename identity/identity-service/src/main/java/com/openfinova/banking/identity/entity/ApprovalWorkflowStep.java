package com.openfinova.banking.identity.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One level in an {@link ApprovalWorkflowInstance} chain. {@link #requiredGlApprovalRoleCode} is
 * the minimum GL approval tier required to satisfy this step.
 */
@Entity
@Table(name = "identity_approval_workflow_steps", indexes = {
        @Index(name = "idx_appr_step_workflow", columnList = "workflow_id"),
        @Index(name = "idx_appr_step_order", columnList = "workflow_id,step_order") })
public class ApprovalWorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private ApprovalWorkflowInstance workflow;

    @NotNull
    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @NotNull
    @Size(max = 30)
    @Column(name = "required_gl_role", nullable = false, length = 30)
    private String requiredGlApprovalRoleCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalWorkflowStepStatus stepStatus = ApprovalWorkflowStepStatus.PENDING;

    @Column(name = "acted_by_user_id")
    private UUID actedByUserId;

    @Column(name = "acted_at")
    private LocalDateTime actedAt;

    @Size(max = 500)
    @Column(length = 500)
    private String comments;

    protected ApprovalWorkflowStep() {
    }

    public ApprovalWorkflowStep(int stepOrder, String requiredGlApprovalRoleCode) {
        this.stepOrder = stepOrder;
        this.requiredGlApprovalRoleCode = requiredGlApprovalRoleCode;
    }

    public UUID getId() {
        return id;
    }

    public ApprovalWorkflowInstance getWorkflow() {
        return workflow;
    }

    public void setWorkflow(ApprovalWorkflowInstance w) {
        this.workflow = w;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(int v) {
        this.stepOrder = v;
    }

    public String getRequiredGlApprovalRoleCode() {
        return requiredGlApprovalRoleCode;
    }

    public void setRequiredGlApprovalRoleCode(String v) {
        this.requiredGlApprovalRoleCode = v;
    }

    public ApprovalWorkflowStepStatus getStepStatus() {
        return stepStatus;
    }

    public void setStepStatus(ApprovalWorkflowStepStatus v) {
        this.stepStatus = v;
    }

    public UUID getActedByUserId() {
        return actedByUserId;
    }

    public void setActedByUserId(UUID v) {
        this.actedByUserId = v;
    }

    public LocalDateTime getActedAt() {
        return actedAt;
    }

    public void setActedAt(LocalDateTime v) {
        this.actedAt = v;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String v) {
        this.comments = v;
    }
}
