package com.openfinova.banking.customer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import com.openfinova.banking.customer.api.entity.KYCDecision;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a single review step in the KYC workflow.
 * Tracks individual verification actions and decisions.
 */
@Entity
@Table(name = "kyc_review_steps", indexes = { @Index(name = "idx_kyc_step_workflow", columnList = "kyc_workflow_id"),
        @Index(name = "idx_kyc_step_decision", columnList = "decision"),
        @Index(name = "idx_kyc_step_reviewed", columnList = "reviewed_at") })
public class KYCReviewStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kyc_workflow_id", nullable = false)
    @NotNull(message = "KYC workflow is required")
    private KYCWorkflow kycWorkflow;

    @Column(name = "step_name", nullable = false, length = 100)
    @NotBlank(message = "Step name is required")
    @Size(max = 100, message = "Step name must not exceed 100 characters")
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @NotNull(message = "Decision is required")
    private KYCDecision decision;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @CreationTimestamp
    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private LocalDateTime reviewedAt;

    // Constructors
    public KYCReviewStep() {
    }

    public KYCReviewStep(KYCWorkflow kycWorkflow, String stepName, KYCDecision decision, String reviewedBy,
            String comments) {
        this.kycWorkflow = kycWorkflow;
        this.stepName = stepName;
        this.decision = decision;
        this.reviewedBy = reviewedBy;
        this.comments = comments;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public KYCWorkflow getKycWorkflow() {
        return kycWorkflow;
    }

    public void setKycWorkflow(KYCWorkflow kycWorkflow) {
        this.kycWorkflow = kycWorkflow;
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
