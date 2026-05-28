package com.openfinova.banking.gl.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.gl.api.entity.GLTransactionSource;
import com.openfinova.banking.gl.api.entity.GLTransactionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Transaction entity representing a financial transaction in the general
 * ledger.
 * Each transaction contains multiple journal entries that must balance (debits = credits).
 */
@Entity(name = "GeneralLedgerTransaction")
@EntityListeners(AuditingEntityListener.class)
@Table(name = "gl_transactions", indexes = {
        @Index(name = "idx_gl_transactions_reference", columnList = "reference_id"),
        @Index(name = "idx_gl_transactions_date", columnList = "transaction_date"),
        @Index(name = "idx_gl_transactions_status", columnList = "status") })
public class GLTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Version field for optimistic locking.
     * Prevents race conditions when multiple users/processes modify the same transaction.
     * Automatically incremented by JPA on each update.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Unique reference ID for idempotency and external tracking.
     */
    @Column(name = "reference_id", unique = true, nullable = false, length = 100)
    @NotBlank(message = "Reference ID is required")
    @Size(max = 100, message = "Reference ID must not exceed 100 characters")
    private String referenceId;

    /**
     * Sequential transaction number for audit trail (gapless).
     * Set during the post step; never null once a transaction is POSTED.
     */
    @Column(name = "transaction_number", nullable = false)
    private Long transactionNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Description is required")
    private String description;

    /**
     * Date of the business event (value date).
     */
    @Column(name = "transaction_date", nullable = false)
    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @Column(name = "currency", length = 3, nullable = false)
    @NotBlank(message = "Transaction currency is required")
    @ValidCurrency
    private String currency;

    /**
     * Date/Time when the transaction was actually posted to the ledger.
     * Stored as UTC instant for precise audit trail across timezones.
     */
    @Column(name = "posting_date")
    private Instant postingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Transaction status is required")
    private GLTransactionStatus status = GLTransactionStatus.DRAFT;

    /**
     * Source of this transaction (manual entry, system-generated, batch, etc.).
     * Determines approval workflow requirements.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 50, nullable = false)
    @NotNull(message = "Transaction source is required")
    private GLTransactionSource source = GLTransactionSource.MANUAL_ENTRY;

    /**
     * User who submitted this transaction for approval.
     * Set when transaction moves from DRAFT to PENDING_APPROVAL status.
     */
    @Column(name = "submitted_by", length = 100)
    @Size(max = 100, message = "Submitted by must not exceed 100 characters")
    private String submittedBy;

    /**
     * Timestamp when transaction was submitted for approval.
     */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /**
     * User who approved and posted this transaction.
     * For system transactions, this may be "SYSTEM".
     */
    @Column(name = "posted_by", length = 100)
    @Size(max = 100, message = "Posted by must not exceed 100 characters")
    private String postedBy;

    /**
     * List of journal entries that make up this transaction.
     * Each journal entry represents either a debit or credit to a specific account.
     *
     * Key characteristics:
     * - Ordered by lineNumber (ascending) as specified by @OrderBy annotation
     * - Each entry has a sequential lineNumber starting from 1
     * - The sum of all debit amounts must equal the sum of all credit amounts
     * - Cascade operations ensure entries are persisted/deleted with the transaction
     * - Lazy loading for performance (entries loaded only when accessed)
     * - Orphan removal ensures deleted entries are removed from database
     * - Line numbers are automatically maintained when entries are added/removed
     *
     * The ordering is critical for:
     * - Consistent presentation in reports and audit trails
     * - Maintaining the logical sequence of accounting entries
     * - Ensuring reproducible transaction display across different sessions
     */
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<GLJournalEntry> journalEntries = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 100)
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    /**
     * Transaction that reversed this transaction (if any).
     * This field is set when this transaction is reversed by another transaction.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversed_by")
    private GLTransaction reversedBy;

    /**
     * List of transactions that this transaction has reversed.
     * This is the inverse side of the reversedBy relationship.
     *
     * When this transaction acts as a reversal, this list contains all the original
     * transactions that were reversed by this transaction. In typical accounting scenarios,
     * a reversal transaction reverses one original transaction, but this design allows
     * flexibility for batch reversals if needed.
     *
     * Example:
     * - Transaction A (original) is reversed by Transaction B (reversal)
     * - A.reversedBy = B
     * - B.reversalTransactions = [A]
     */
    @OneToMany(mappedBy = "reversedBy", fetch = FetchType.LAZY)
    private List<GLTransaction> reversalTransactions = new ArrayList<>();

    // Constructors
    public GLTransaction() {
    }

    public GLTransaction(String referenceId, String description, LocalDate transactionDate) {
        this.referenceId = referenceId;
        this.description = description;
        this.transactionDate = transactionDate;
        this.status = GLTransactionStatus.DRAFT;
        this.source = GLTransactionSource.MANUAL_ENTRY;
    }

    // Business logic methods

    /**
     * Checks if this transaction is a draft that can be edited.
     *
     * @return true if status is DRAFT, false otherwise
     */
    public boolean isDraft() {
        return GLTransactionStatus.DRAFT.equals(status);
    }

    /**
     * Checks if this transaction is pending approval.
     *
     * @return true if status is PENDING_APPROVAL, false otherwise
     */
    public boolean isPendingApproval() {
        return GLTransactionStatus.PENDING_APPROVAL.equals(status);
    }

    /**
     * Checks if this transaction is posted
     *
     * @return true if status is POSTED, false otherwise
     */
    public boolean isPosted() {
        return GLTransactionStatus.POSTED.equals(status);
    }

    /**
     * Checks if this transaction is reversed
     *
     * @return true if status is REVERSED, false otherwise
     */
    public boolean isReversed() {
        return GLTransactionStatus.REVERSED.equals(status);
    }

    /**
     * Checks if this transaction was rejected during approval.
     *
     * @return true if status is REJECTED, false otherwise
     */
    public boolean isRejected() {
        return GLTransactionStatus.REJECTED.equals(status);
    }

    /**
     * Checks if this transaction was cancelled by the creator.
     *
     * @return true if status is CANCELLED, false otherwise
     */
    public boolean isCancelled() {
        return GLTransactionStatus.CANCELLED.equals(status);
    }

    /**
     * Checks if this transaction is pending (legacy compatibility).
     * Now maps to DRAFT status.
     *
     * @return true if status is DRAFT, false otherwise
     * @deprecated Use {@link #isDraft()} instead
     */
    @Deprecated
    public boolean isPending() {
        return isDraft();
    }

    /**
     * Checks if this is a system-generated transaction (auto-posted).
     * System transactions bypass approval workflow.
     *
     * @return true if source indicates system-generated
     */
    public boolean isSystemGenerated() {
        return source != null && source.isSystemGenerated();
    }

    /**
     * Checks if this is a manual transaction requiring approval.
     *
     * @return true if source indicates manual entry
     */
    public boolean isManualEntry() {
        return source != null && source.isManual();
    }

    /**
     * Checks if this transaction requires maker-checker approval.
     *
     * @return true if approval workflow is required
     */
    public boolean requiresApproval() {
        return source != null && source.requiresApproval();
    }

    /**
     * Submit this draft transaction for approval.
     *
     * @param submitter username of the person submitting for approval
     * @throws IllegalStateException if transaction is not in DRAFT status
     */
    public void submitForApproval(String submitter, LocalDateTime submittedAt) {
        if (!isDraft()) {
            throw new IllegalStateException("Only draft transactions can be submitted for approval");
        }
        this.status = GLTransactionStatus.PENDING_APPROVAL;
        this.submittedBy = submitter;
        this.submittedAt = submittedAt;
    }

    /**
     * Approve and post this transaction to the general ledger.
     *
     * @param approver username of the person approving (or "SYSTEM" for auto-approved)
     * @throws IllegalStateException if transaction is not ready to post
     */
    public void approveAndPost(String approver, Instant postingDate) {
        if (!isPendingApproval() && !isDraft()) {
            throw new IllegalStateException(
                    "Only pending approval or draft transactions can be approved. Current status: " + status);
        }
        this.status = GLTransactionStatus.POSTED;
        this.postedBy = approver;
        this.postingDate = postingDate;
    }

    /**
     * Reject this transaction during approval.
     *
     * @throws IllegalStateException if transaction is not pending approval
     */
    public void reject() {
        if (!isPendingApproval()) {
            throw new IllegalStateException("Only transactions pending approval can be rejected");
        }
        this.status = GLTransactionStatus.REJECTED;
    }

    /**
     * Cancel this draft transaction before submission.
     *
     * @throws IllegalStateException if transaction is not in DRAFT status
     */
    public void cancel() {
        if (!isDraft()) {
            throw new IllegalStateException("Only draft transactions can be cancelled");
        }
        this.status = GLTransactionStatus.CANCELLED;
    }

    /**
     * Marks this transaction as reversed
     *
     * @param reversalTransaction the transaction that reverses this one
     */
    public void markReversed(GLTransaction reversalTransaction) {
        if (!isPosted()) {
            throw new IllegalStateException("Only posted transactions can be reversed");
        }
        if (isReversed()) {
            throw new IllegalStateException("Transaction is already reversed");
        }
        this.status = GLTransactionStatus.REVERSED;
        this.reversedBy = reversalTransaction;
    }

    /**
     * Adds a journal entry to this transaction
     *
     * @param journalEntry the journal entry to add
     */
    public void addGLJournalEntry(GLJournalEntry journalEntry) {
        journalEntries.add(journalEntry);
        journalEntry.setTransaction(this);
        journalEntry.setLineNumber(journalEntries.size());
    }

    /**
     * Removes a journal entry from this transaction
     *
     * @param journalEntry the journal entry to remove
     */
    public void removeGLJournalEntry(GLJournalEntry journalEntry) {
        journalEntries.remove(journalEntry);
        journalEntry.setTransaction(null);
        // Renumber remaining entries
        for (int i = 0; i < journalEntries.size(); i++) {
            journalEntries.get(i).setLineNumber(i + 1);
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Long getTransactionNumber() {
        return transactionNumber;
    }

    public void setTransactionNumber(Long transactionNumber) {
        this.transactionNumber = transactionNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(Instant postingDate) {
        this.postingDate = postingDate;
    }

    public GLTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(GLTransactionStatus status) {
        this.status = status;
    }

    public GLTransactionSource getSource() {
        return source;
    }

    public void setSource(GLTransactionSource source) {
        this.source = source;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    public List<GLJournalEntry> getJournalEntries() {
        return journalEntries;
    }

    public void setJournalEntries(List<GLJournalEntry> journalEntries) {
        this.journalEntries = journalEntries;
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

    public GLTransaction getReversedBy() {
        return reversedBy;
    }

    public void setReversedBy(GLTransaction reversedBy) {
        this.reversedBy = reversedBy;
    }

    public List<GLTransaction> getReversalTransactions() {
        return reversalTransactions;
    }

    public void setReversalTransactions(List<GLTransaction> reversalTransactions) {
        this.reversalTransactions = reversalTransactions;
    }

    // equals, hashCode, and toString

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof GLTransaction transaction))
            return false;
        return referenceId != null && referenceId.equals(transaction.referenceId);
    }

    @Override
    public int hashCode() {
        return referenceId != null ? referenceId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Transaction{" + "id=" + id + ", referenceId='" + referenceId + '\'' + ", transactionNumber="
                + transactionNumber + ", description='" + description + '\'' + ", transactionDate=" + transactionDate
                + ", currency='" + currency + '\'' + ", status=" + status + '}';
    }
}
