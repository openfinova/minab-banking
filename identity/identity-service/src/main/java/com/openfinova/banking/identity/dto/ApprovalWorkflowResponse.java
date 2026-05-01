package com.openfinova.banking.identity.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.openfinova.banking.identity.entity.ApprovalWorkflowInstance;
import com.openfinova.banking.identity.entity.ApprovalWorkflowStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cross-domain approval workflow instance with ordered steps")
public class ApprovalWorkflowResponse {

    private UUID id;
    private String resourceType;
    private String resourceId;
    private ApprovalWorkflowStatus status;
    private String rejectionReason;
    private List<ApprovalWorkflowStepResponse> steps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ApprovalWorkflowResponse from(ApprovalWorkflowInstance w) {
        ApprovalWorkflowResponse r = new ApprovalWorkflowResponse();
        r.id = w.getId();
        r.resourceType = w.getResourceType();
        r.resourceId = w.getResourceId();
        r.status = w.getStatus();
        r.rejectionReason = w.getRejectionReason();
        r.steps = w.getSteps().stream().map(ApprovalWorkflowStepResponse::from).toList();
        r.createdAt = w.getCreatedAt();
        r.updatedAt = w.getUpdatedAt();
        return r;
    }

    public UUID getId() {
        return id;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public ApprovalWorkflowStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public List<ApprovalWorkflowStepResponse> getSteps() {
        return steps;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
