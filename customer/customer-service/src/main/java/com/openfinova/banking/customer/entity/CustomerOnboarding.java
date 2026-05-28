package com.openfinova.banking.customer.entity;

import com.openfinova.banking.customer.api.entity.OnboardingStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity tracking a customer's onboarding journey.
 *
 * <p>Captures every onboarding step — from initial registration through
 * KYC, account setup, and welcome-kit delivery — with timestamps and
 * responsible parties for each transition. Separate from {@link KYCWorkflow}
 * which focuses only on identity verification.</p>
 *
 * <p>A customer has at most one onboarding record. Re-onboarding (e.g., after
 * re-activation from ABANDONED) creates a new record and archives the old one.</p>
 */
@Entity
@Table(name = "customer_onboardings", indexes = { @Index(name = "idx_onboarding_customer", columnList = "customer_id"),
        @Index(name = "idx_onboarding_status", columnList = "status"),
        @Index(name = "idx_onboarding_started_at", columnList = "started_at"),
        @Index(name = "idx_onboarding_channel", columnList = "onboarding_channel") })
public class CustomerOnboarding {

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
    @Column(nullable = false, length = 30)
    @NotNull(message = "Onboarding status is required")
    private OnboardingStatus status = OnboardingStatus.INITIATED;

    /**
     * Channel through which the customer initiated onboarding.
     * (e.g., "BRANCH", "MOBILE_APP", "WEB_PORTAL", "AGENT", "API")
     */
    @Column(name = "onboarding_channel", length = 50)
    private String onboardingChannel;

    /**
     * Branch or office code where onboarding was initiated (if applicable).
     */
    @Column(name = "originating_branch", length = 20)
    private String originatingBranch;

    /**
     * User or system that initiated the onboarding.
     */
    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;

    /**
     * Timestamp when KYC step was completed.
     */
    @Column(name = "kyc_completed_at")
    private LocalDateTime kycCompletedAt;

    /**
     * Timestamp when account setup was completed.
     */
    @Column(name = "account_setup_at")
    private LocalDateTime accountSetupAt;

    /**
     * Timestamp when the welcome kit was dispatched.
     */
    @Column(name = "welcome_kit_sent_at")
    private LocalDateTime welcomeKitSentAt;

    /**
     * Timestamp when onboarding was fully completed.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Reason for rejection or abandonment.
     */
    @Column(name = "outcome_reason", columnDefinition = "TEXT")
    private String outcomeReason;

    /**
     * Reference ID in an external CRM or lead-management system.
     */
    @Column(name = "external_reference", length = 100)
    private String externalReference;

    /**
     * Referral code or campaign code used during onboarding.
     */
    @Column(name = "referral_code", length = 50)
    private String referralCode;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CustomerOnboarding() {
    }

    public CustomerOnboarding(Customer customer, String onboardingChannel, String initiatedBy) {
        this.customer = customer;
        this.onboardingChannel = onboardingChannel;
        this.initiatedBy = initiatedBy;
    }

    // Business logic

    public void advanceTo(OnboardingStatus newStatus, String outcomeReason, LocalDateTime changedAt) {
        OnboardingStatus current = this.status;

        switch (newStatus) {
            case KYC_IN_PROGRESS -> {
                if (current != OnboardingStatus.INITIATED)
                    throw new IllegalStateException("Can only advance to KYC_IN_PROGRESS from INITIATED");
            }
            case KYC_COMPLETED -> {
                if (current != OnboardingStatus.KYC_IN_PROGRESS)
                    throw new IllegalStateException("Can only advance to KYC_COMPLETED from KYC_IN_PROGRESS");
                this.kycCompletedAt = changedAt;
            }
            case ACCOUNT_SETUP -> {
                if (current != OnboardingStatus.KYC_COMPLETED)
                    throw new IllegalStateException("Can only advance to ACCOUNT_SETUP from KYC_COMPLETED");
            }
            case WELCOME_KIT_SENT -> {
                if (current != OnboardingStatus.ACCOUNT_SETUP)
                    throw new IllegalStateException("Can only advance to WELCOME_KIT_SENT from ACCOUNT_SETUP");
                this.welcomeKitSentAt = changedAt;
            }
            case COMPLETED -> {
                if (current != OnboardingStatus.WELCOME_KIT_SENT && current != OnboardingStatus.ACCOUNT_SETUP)
                    throw new IllegalStateException("Cannot complete onboarding from status: " + current);
                this.completedAt = changedAt;
            }
            case ABANDONED, REJECTED -> this.outcomeReason = outcomeReason;
            default -> throw new IllegalArgumentException("Invalid onboarding status transition: " + newStatus);
        }

        this.status = newStatus;
    }

    public void advanceTo(OnboardingStatus newStatus, LocalDateTime changedAt) {
        advanceTo(newStatus, null, changedAt);
    }

    public boolean isCompleted() {
        return status == OnboardingStatus.COMPLETED;
    }

    public boolean isTerminal() {
        return status == OnboardingStatus.COMPLETED || status == OnboardingStatus.ABANDONED
                || status == OnboardingStatus.REJECTED;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public OnboardingStatus getStatus() {
        return status;
    }

    public void setStatus(OnboardingStatus status) {
        this.status = status;
    }

    public String getOnboardingChannel() {
        return onboardingChannel;
    }

    public void setOnboardingChannel(String onboardingChannel) {
        this.onboardingChannel = onboardingChannel;
    }

    public String getOriginatingBranch() {
        return originatingBranch;
    }

    public void setOriginatingBranch(String originatingBranch) {
        this.originatingBranch = originatingBranch;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public LocalDateTime getKycCompletedAt() {
        return kycCompletedAt;
    }

    public void setKycCompletedAt(LocalDateTime kycCompletedAt) {
        this.kycCompletedAt = kycCompletedAt;
    }

    public LocalDateTime getAccountSetupAt() {
        return accountSetupAt;
    }

    public void setAccountSetupAt(LocalDateTime accountSetupAt) {
        this.accountSetupAt = accountSetupAt;
    }

    public LocalDateTime getWelcomeKitSentAt() {
        return welcomeKitSentAt;
    }

    public void setWelcomeKitSentAt(LocalDateTime welcomeKitSentAt) {
        this.welcomeKitSentAt = welcomeKitSentAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getOutcomeReason() {
        return outcomeReason;
    }

    public void setOutcomeReason(String outcomeReason) {
        this.outcomeReason = outcomeReason;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
