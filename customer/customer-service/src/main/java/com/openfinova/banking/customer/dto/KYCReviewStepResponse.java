package com.openfinova.banking.customer.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.customer.api.entity.KYCDecision;

/** API projection for {@link com.openfinova.banking.customer.entity.KYCReviewStep} (no workflow back-reference). */
public class KYCReviewStepResponse {

    private UUID id;
    private String stepName;
    private KYCDecision decision;
    private String comments;
    private String reviewedBy;
    private LocalDateTime reviewedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public KYCDecision getDecision() {
        return decision;
    }

    public void setDecision(KYCDecision decision) {
        this.decision = decision;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
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
}
