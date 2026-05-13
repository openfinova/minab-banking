package com.openfinova.banking.customer.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.openfinova.banking.customer.api.entity.KYCStatus;

/**
 * API projection for {@link com.openfinova.banking.customer.entity.KYCWorkflow}.
 * Avoids serializing the {@code Customer} entity graph (bidirectional collections caused runaway Jackson nesting).
 */
public class KYCWorkflowResponse {

    private UUID id;
    private UUID customerId;
    private KYCStatus status;
    private String initiatedBy;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String comments;
    private String rejectionReason;
    private String reVerificationReason;
    private Long version;
    private LocalDateTime updatedAt;
    private List<KYCReviewStepResponse> reviewSteps = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public KYCStatus getStatus() {
        return status;
    }

    public void setStatus(KYCStatus status) {
        this.status = status;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public LocalDateTime getInitiatedAt() {
        return initiatedAt;
    }

    public void setInitiatedAt(LocalDateTime initiatedAt) {
        this.initiatedAt = initiatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getReVerificationReason() {
        return reVerificationReason;
    }

    public void setReVerificationReason(String reVerificationReason) {
        this.reVerificationReason = reVerificationReason;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<KYCReviewStepResponse> getReviewSteps() {
        return reviewSteps;
    }

    public void setReviewSteps(List<KYCReviewStepResponse> reviewSteps) {
        this.reviewSteps = reviewSteps;
    }
}
