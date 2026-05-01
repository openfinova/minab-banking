package com.openfinova.banking.gl.entity;

import com.openfinova.banking.gl.api.entity.AgingBracket;
import com.openfinova.banking.gl.api.entity.SuspenseReasonCode;
import com.openfinova.banking.gl.api.entity.SuspenseStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Suspense item tracking entity.
 *
 * BUSINESS CONTEXT:
 * A suspense account is a temporary holding account used when transactions cannot be posted
 * to their correct final destination due to missing or unclear information.
 *
 * REGULATORY REQUIREMENTS:
 * - Basel Committee: Active management required, aged balances = control weakness
 * - Most regulators: Resolution within 30-90 days
 * - AML/CFT: Unidentified deposits require enhanced due diligence
 * - Audit: Daily reconciliation, executive oversight of aged items
 *
 * GL INTEGRATION:
 * - The actual accounting entry is in GL (normal transaction posting)
 * - This entity provides operational tracking and workflow management
 * - GL transaction posted to SUSPENSE operational account
 * - When cleared, offsetting GL transaction moves amount to correct account
 *
 * DECISION FLOW:
 * - Transaction Processing (TP) Module decides to use suspense
 * - GL Module records the transaction as instructed
 * - This entity tracks metadata for resolution workflow
 */
@Entity
@Table(name = "gl_suspense_items", indexes = { @Index(name = "idx_suspense_status", columnList = "status"),
        @Index(name = "idx_suspense_posting_date", columnList = "posting_date"),
        @Index(name = "idx_suspense_gl_transaction", columnList = "gl_transaction_id"),
        @Index(name = "idx_suspense_assigned_to", columnList = "assigned_to"),
        @Index(name = "idx_suspense_reason", columnList = "reason_code") })
public class SuspenseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the GL transaction that posted to suspense account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_transaction_id", nullable = false)
    @NotNull(message = "GL transaction reference is required")
    private GLTransaction glTransaction;

    /**
     * Amount in suspense (denormalized for reporting performance).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    /**
     * Currency of the suspense item.
     */
    @Column(nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @Size(max = 3, message = "Currency must be 3 characters")
    private String currency;

    /**
     * Current status of the suspense item.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @NotNull(message = "Status is required")
    private SuspenseStatus status = SuspenseStatus.PENDING;

    /**
     * Reason why transaction was posted to suspense.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 50)
    @NotNull(message = "Reason code is required")
    private SuspenseReasonCode reasonCode;

    /**
     * Detailed description of the suspense item.
     */
    @Column(length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Source system that originated the transaction.
     */
    @Column(name = "source_system", length = 100)
    @Size(max = 100, message = "Source system must not exceed 100 characters")
    private String sourceSystem;

    /**
     * External reference from source system.
     */
    @Column(name = "external_reference", length = 100)
    @Size(max = 100, message = "External reference must not exceed 100 characters")
    private String externalReference;

    /**
     * Date the transaction was posted to suspense.
     */
    @Column(name = "posting_date", nullable = false)
    @NotNull(message = "Posting date is required")
    private LocalDate postingDate;

    /**
     * User/team assigned to investigate and resolve.
     */
    @Column(name = "assigned_to", length = 100)
    @Size(max = 100, message = "Assigned to must not exceed 100 characters")
    private String assignedTo;

    /**
     * Investigation notes and progress updates.
     */
    @Column(name = "investigation_notes", length = 2000)
    @Size(max = 2000, message = "Investigation notes must not exceed 2000 characters")
    private String investigationNotes;

    /**
     * Target account identified for clearing (if known).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    private GLAccount targetAccount;

    /**
     * When the item was cleared from suspense.
     */
    @Column(name = "cleared_date")
    private LocalDate clearedDate;

    /**
     * User who cleared the suspense item.
     */
    @Column(name = "cleared_by", length = 100)
    @Size(max = 100, message = "Cleared by must not exceed 100 characters")
    private String clearedBy;

    /**
     * GL transaction that cleared the suspense (offsetting entry).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clearing_transaction_id")
    private GLTransaction clearingTransaction;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 100)
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    // Constructors

    public SuspenseItem() {
    }

    public SuspenseItem(GLTransaction glTransaction, BigDecimal amount, String currency, SuspenseReasonCode reasonCode,
            String description) {
        this.glTransaction = glTransaction;
        this.amount = amount;
        this.currency = currency;
        this.reasonCode = reasonCode;
        this.description = description;
        this.postingDate = LocalDate.now();
        this.status = SuspenseStatus.PENDING;
    }

    // Business Logic Methods

    /**
     * Calculate age of suspense item in days from posting date.
     */
    public long getAgeDays() {
        return ChronoUnit.DAYS.between(postingDate, LocalDate.now());
    }

    /**
     * Get the aging bracket this item falls into.
     */
    public AgingBracket getAgingBracket() {
        return AgingBracket.fromAgeDays(getAgeDays());
    }

    /**
     * Check if item requires AML review based on reason code.
     */
    public boolean requiresAMLReview() {
        return reasonCode.requiresAMLReview();
    }

    /**
     * Mark item as under investigation.
     */
    public void startInvestigation(String assignee) {
        this.status = SuspenseStatus.UNDER_INVESTIGATION;
        this.assignedTo = assignee;
    }

    /**
     * Mark item as escalated.
     */
    public void escalate() {
        this.status = SuspenseStatus.ESCALATED;
    }

    /**
     * Mark item as cleared.
     */
    public void markCleared(String clearedBy, GLTransaction clearingTransaction) {
        this.status = SuspenseStatus.CLEARED;
        this.clearedDate = LocalDate.now();
        this.clearedBy = clearedBy;
        this.clearingTransaction = clearingTransaction;
    }

    /**
     * Mark item as auto-cleared.
     */
    public void markAutoCleared(GLTransaction clearingTransaction) {
        this.status = SuspenseStatus.AUTO_CLEARED;
        this.clearedDate = LocalDate.now();
        this.clearedBy = "SYSTEM";
        this.clearingTransaction = clearingTransaction;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GLTransaction getGlTransaction() {
        return glTransaction;
    }

    public void setGlTransaction(GLTransaction glTransaction) {
        this.glTransaction = glTransaction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public SuspenseStatus getStatus() {
        return status;
    }

    public void setStatus(SuspenseStatus status) {
        this.status = status;
    }

    public SuspenseReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(SuspenseReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getInvestigationNotes() {
        return investigationNotes;
    }

    public void setInvestigationNotes(String investigationNotes) {
        this.investigationNotes = investigationNotes;
    }

    public GLAccount getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(GLAccount targetAccount) {
        this.targetAccount = targetAccount;
    }

    public LocalDate getClearedDate() {
        return clearedDate;
    }

    public void setClearedDate(LocalDate clearedDate) {
        this.clearedDate = clearedDate;
    }

    public String getClearedBy() {
        return clearedBy;
    }

    public void setClearedBy(String clearedBy) {
        this.clearedBy = clearedBy;
    }

    public GLTransaction getClearingTransaction() {
        return clearingTransaction;
    }

    public void setClearingTransaction(GLTransaction clearingTransaction) {
        this.clearingTransaction = clearingTransaction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // equals, hashCode, toString

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SuspenseItem that))
            return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SuspenseItem{" + "id=" + id + ", amount=" + amount + ", currency='" + currency + '\'' + ", status="
                + status + ", reasonCode=" + reasonCode + ", postingDate=" + postingDate + ", ageDays=" + getAgeDays()
                + '}';
    }
}
