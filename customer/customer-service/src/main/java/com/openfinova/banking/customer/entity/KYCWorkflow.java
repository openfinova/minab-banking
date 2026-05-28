package com.openfinova.banking.customer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.openfinova.banking.customer.api.entity.KYCStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a KYC (Know Your Customer) workflow instance.
 * Tracks the complete lifecycle of customer verification process.
 */
@Entity
@Table(name = "kyc_workflows", indexes = { @Index(name = "idx_kyc_workflow_customer", columnList = "customer_id"),
        @Index(name = "idx_kyc_workflow_status", columnList = "status"),
        @Index(name = "idx_kyc_workflow_initiated", columnList = "initiated_at") })
public class KYCWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Customer is required")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "KYC status is required")
    private KYCStatus status = KYCStatus.PENDING;

    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;

    @CreationTimestamp
    @Column(name = "initiated_at", nullable = false, updatable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "re_verification_reason", columnDefinition = "TEXT")
    private String reVerificationReason;

    @OneToMany(mappedBy = "kycWorkflow", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<KYCReviewStep> reviewSteps = new ArrayList<>();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public KYCWorkflow() {
    }

    public KYCWorkflow(Customer customer, String initiatedBy) {
        this.customer = customer;
        this.initiatedBy = initiatedBy;
        this.status = KYCStatus.PENDING;
    }

    // Business Logic
    public void submitForReview(String submittedBy) {
        this.status = KYCStatus.IN_REVIEW;
        this.initiatedBy = submittedBy;
    }

    public void approve(String reviewedBy, String comments, LocalDateTime reviewedAt) {
        this.status = KYCStatus.VERIFIED;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.completedAt = reviewedAt;
        this.comments = comments;
    }

    public void reject(String reviewedBy, String rejectionReason, LocalDateTime reviewedAt) {
        this.status = KYCStatus.REJECTED;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.completedAt = reviewedAt;
        this.rejectionReason = rejectionReason;
    }

    public void expire() {
        this.status = KYCStatus.EXPIRED;
    }

    public boolean isCompleted() {
        return status == KYCStatus.VERIFIED || status == KYCStatus.REJECTED;
    }

    public void addReviewStep(KYCReviewStep step) {
        reviewSteps.add(step);
        step.setKycWorkflow(this);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
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

    public List<KYCReviewStep> getReviewSteps() {
        return reviewSteps;
    }

    public void setReviewSteps(List<KYCReviewStep> reviewSteps) {
        this.reviewSteps = reviewSteps;
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
}
