package com.openfinova.banking.loan.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.openfinova.banking.loan.api.entity.GuarantorStatus;
import com.openfinova.banking.loan.api.entity.GuarantorType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Guarantor entity representing a person or entity guaranteeing loan repayment.
 * Tracks guarantee amount and guarantor obligations.
 */
@Entity
@Table(name = "guarantors", indexes = { @Index(name = "idx_guarantors_account", columnList = "loan_account_id"),
        @Index(name = "idx_guarantors_customer", columnList = "customer_id"),
        @Index(name = "idx_guarantors_type", columnList = "guarantor_type"),
        @Index(name = "idx_guarantors_status", columnList = "status") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Guarantor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The loan application this guarantor is associated with (during application phase).
     * Null after loan account is created.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id")
    private LoanApplication loanApplication;

    /**
     * The loan account this guarantor is guaranteeing (after approval).
     * Null during application phase.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_account_id")
    private LoanAccount loanAccount;

    /**
     * Reference to the customer acting as guarantor.
     */
    @NotNull(message = "Customer ID is required")
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "guarantor_type", nullable = false, length = 30)
    @NotNull(message = "Guarantor type is required")
    private GuarantorType guarantorType;

    /**
     * Maximum amount guaranteed by this guarantor.
     */
    @NotNull(message = "Guaranteed amount is required")
    @DecimalMin(value = "0.0", message = "Guaranteed amount must be positive")
    @Column(name = "guaranteed_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal guaranteedAmount;

    /**
     * Percentage of loan amount guaranteed (alternative to fixed amount).
     */
    @DecimalMin(value = "0.0", message = "Guarantee percentage must be positive")
    @DecimalMax(value = "100.0", message = "Guarantee percentage cannot exceed 100%")
    @Column(name = "guarantee_percentage", precision = 5, scale = 2)
    private BigDecimal guaranteePercentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Status is required")
    private GuarantorStatus status = GuarantorStatus.ACTIVE;

    @Column(length = 1000)
    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String remarks;

    @Column(name = "verified_date")
    private Instant verifiedDate;

    @Column(name = "verified_by", length = 100)
    @Size(max = 100, message = "Verified by must not exceed 100 characters")
    private String verifiedBy;

    @Column(name = "released_date")
    private Instant releasedDate;

    @Column(name = "released_by", length = 100)
    @Size(max = 100, message = "Released by must not exceed 100 characters")
    private String releasedBy;

    @Column(name = "removed_date")
    private Instant removedDate;

    @Column(name = "removed_by", length = 100)
    @Size(max = 100, message = "Removed by must not exceed 100 characters")
    private String removedBy;

    @Column(name = "removal_reason", length = 500)
    @Size(max = 500, message = "Removal reason must not exceed 500 characters")
    private String removalReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public Guarantor() {
    }

    // Business Logic
    public boolean isActive() {
        return GuarantorStatus.ACTIVE.equals(status);
    }

    /**
     * Validates if the guarantor can transition to the specified status.
     *
     * Valid transitions:
     * - PENDING → ACTIVE, REMOVED
     * - ACTIVE → INVOKED, RELEASED, REMOVED
     * - INVOKED → RELEASED (after payment)
     * - RELEASED → (terminal state)
     * - REMOVED → (terminal state)
     *
     * @param newStatus the target status
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(GuarantorStatus newStatus) {
        if (this.status == newStatus) {
            return true; // Same status is always valid
        }

        return switch (this.status) {
            case PENDING -> newStatus == GuarantorStatus.ACTIVE || newStatus == GuarantorStatus.REMOVED;
            case ACTIVE -> newStatus == GuarantorStatus.INVOKED || newStatus == GuarantorStatus.RELEASED
                    || newStatus == GuarantorStatus.REMOVED;
            case INVOKED -> newStatus == GuarantorStatus.RELEASED;
            case RELEASED, REMOVED -> false; // Terminal states
        };
    }

    /**
     * Gets a descriptive error message for invalid status transitions.
     *
     * @param newStatus the attempted target status
     * @return error message describing why the transition is invalid
     */
    public String getTransitionErrorMessage(GuarantorStatus newStatus) {
        if (this.status == newStatus) {
            return String.format("Guarantor is already in %s status", newStatus);
        }

        return switch (this.status) {
            case PENDING ->
                String.format("Cannot transition from PENDING to %s. Valid transitions: ACTIVE, REMOVED", newStatus);
            case ACTIVE -> String.format(
                    "Cannot transition from ACTIVE to %s. Valid transitions: INVOKED, RELEASED, REMOVED",
                    newStatus);
            case INVOKED ->
                String.format("Cannot transition from INVOKED to %s. Valid transition: RELEASED", newStatus);
            case RELEASED -> "Cannot change status of RELEASED guarantor. This is a terminal state";
            case REMOVED -> "Cannot change status of REMOVED guarantor. This is a terminal state";
        };
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LoanApplication getLoanApplication() {
        return loanApplication;
    }

    public void setLoanApplication(LoanApplication loanApplication) {
        this.loanApplication = loanApplication;
    }

    public LoanAccount getLoanAccount() {
        return loanAccount;
    }

    public void setLoanAccount(LoanAccount loanAccount) {
        this.loanAccount = loanAccount;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public GuarantorType getGuarantorType() {
        return guarantorType;
    }

    public void setGuarantorType(GuarantorType guarantorType) {
        this.guarantorType = guarantorType;
    }

    public BigDecimal getGuaranteedAmount() {
        return guaranteedAmount;
    }

    public void setGuaranteedAmount(BigDecimal guaranteedAmount) {
        this.guaranteedAmount = guaranteedAmount;
    }

    public BigDecimal getGuaranteePercentage() {
        return guaranteePercentage;
    }

    public void setGuaranteePercentage(BigDecimal guaranteePercentage) {
        this.guaranteePercentage = guaranteePercentage;
    }

    public GuarantorStatus getStatus() {
        return status;
    }

    public void setStatus(GuarantorStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Instant getVerifiedDate() {
        return verifiedDate;
    }

    public void setVerifiedDate(Instant verifiedDate) {
        this.verifiedDate = verifiedDate;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public Instant getReleasedDate() {
        return releasedDate;
    }

    public void setReleasedDate(Instant releasedDate) {
        this.releasedDate = releasedDate;
    }

    public String getReleasedBy() {
        return releasedBy;
    }

    public void setReleasedBy(String releasedBy) {
        this.releasedBy = releasedBy;
    }

    public Instant getRemovedDate() {
        return removedDate;
    }

    public void setRemovedDate(Instant removedDate) {
        this.removedDate = removedDate;
    }

    public String getRemovedBy() {
        return removedBy;
    }

    public void setRemovedBy(String removedBy) {
        this.removedBy = removedBy;
    }

    public String getRemovalReason() {
        return removalReason;
    }

    public void setRemovalReason(String removalReason) {
        this.removalReason = removalReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
