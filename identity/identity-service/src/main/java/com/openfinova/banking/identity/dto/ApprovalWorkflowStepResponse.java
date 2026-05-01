package com.openfinova.banking.identity.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.identity.entity.ApprovalWorkflowStep;
import com.openfinova.banking.identity.entity.ApprovalWorkflowStepStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One step in an identity approval workflow")
public class ApprovalWorkflowStepResponse {

    private UUID id;
    private int stepOrder;
    private String requiredGlApprovalRoleCode;
    private ApprovalWorkflowStepStatus stepStatus;
    private UUID actedByUserId;
    private LocalDateTime actedAt;
    private String comments;

    public static ApprovalWorkflowStepResponse from(ApprovalWorkflowStep s) {
        ApprovalWorkflowStepResponse r = new ApprovalWorkflowStepResponse();
        r.id = s.getId();
        r.stepOrder = s.getStepOrder();
        r.requiredGlApprovalRoleCode = s.getRequiredGlApprovalRoleCode();
        r.stepStatus = s.getStepStatus();
        r.actedByUserId = s.getActedByUserId();
        r.actedAt = s.getActedAt();
        r.comments = s.getComments();
        return r;
    }

    public UUID getId() {
        return id;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public String getRequiredGlApprovalRoleCode() {
        return requiredGlApprovalRoleCode;
    }

    public ApprovalWorkflowStepStatus getStepStatus() {
        return stepStatus;
    }

    public UUID getActedByUserId() {
        return actedByUserId;
    }

    public LocalDateTime getActedAt() {
        return actedAt;
    }

    public String getComments() {
        return comments;
    }
}
