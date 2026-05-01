package com.openfinova.banking.gl.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.openfinova.banking.gl.api.entity.ApprovalAction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Approval record for GL transaction maker-checker workflow.
 *
 * Each transaction may have multiple approval records for multi-level authorization.
 * Approval records are immutable once created - they form the audit trail of who
 * approved what, when, and from where.
 *
 * Example multi-level approval flow:
 *   Transaction Amount: $500,000
 *   Level 1: Senior Accountant approves
 *   Level 2: Accounting Manager approves
 *   Level 3: Controller approves → Transaction posted
 */
@Entity
@Table(name = "gl_transaction_approvals", indexes = {
        @Index(name = "idx_approvals_transaction", columnList = "transaction_id"),
        @Index(name = "idx_approvals_approver", columnList = "approved_by"),
        @Index(name = "idx_approvals_timestamp", columnList = "approval_timestamp") })
public class GLTransactionApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Transaction being approved.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    @NotNull(message = "Transaction is required")
    private GLTransaction transaction;

    /**
     * Approval level for multi-level authorization.
     * Level 1 is first approver, level 2 is second approver, etc.
     * For single-approval workflows, this is always 1.
     */
    @Column(name = "approval_level", nullable = false)
    @NotNull(message = "Approval level is required")
    private Integer approvalLevel;

    /**
     * Action taken by the approver.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Approval action is required")
    private ApprovalAction action;

    /**
     * User who performed the approval action.
     * Should be validated against authorization limits.
     */
    @Column(name = "approved_by", nullable = false, length = 100)
    @NotBlank(message = "Approved by is required")
    @Size(max = 100, message = "Approved by must not exceed 100 characters")
    private String approvedBy;

    /**
     * When the approval action was taken.
     */
    @Column(name = "approval_timestamp", nullable = false)
    @NotNull(message = "Approval timestamp is required")
    private LocalDateTime approvalTimestamp;

    /**
     * Comments from approver explaining the decision.
     * Especially important for rejections - should document the reason.
     */
    @Column(length = 500)
    @Size(max = 500, message = "Comments must not exceed 500 characters")
    private String comments;

    /**
     * IP address from which approval was submitted.
     * Part of security audit trail.
     */
    @Column(name = "ip_address", length = 50)
    @Size(max = 50, message = "IP address must not exceed 50 characters")
    private String ipAddress;

    /**
     * User agent (browser/device) used for approval.
     * Part of security audit trail.
     */
    @Column(name = "user_agent", length = 255)
    @Size(max = 255, message = "User agent must not exceed 255 characters")
    private String userAgent;

    // Constructors

    public GLTransactionApproval() {
    }

    public GLTransactionApproval(GLTransaction transaction, Integer approvalLevel, ApprovalAction action,
            String approvedBy, String comments) {
        this.transaction = transaction;
        this.approvalLevel = approvalLevel;
        this.action = action;
        this.approvedBy = approvedBy;
        this.approvalTimestamp = LocalDateTime.now();
        this.comments = comments;
    }

    // Business logic methods

    /**
     * Check if this is an approval (vs rejection/return).
     */
    public boolean isApproval() {
        return ApprovalAction.APPROVED.equals(action);
    }

    /**
     * Check if this is a rejection.
     */
    public boolean isRejection() {
        return ApprovalAction.REJECTED.equals(action);
    }

    /**
     * Check if this is a return for corrections.
     */
    public boolean isReturn() {
        return ApprovalAction.RETURNED.equals(action);
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GLTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(GLTransaction transaction) {
        this.transaction = transaction;
    }

    public Integer getApprovalLevel() {
        return approvalLevel;
    }

    public void setApprovalLevel(Integer approvalLevel) {
        this.approvalLevel = approvalLevel;
    }

    public ApprovalAction getAction() {
        return action;
    }

    public void setAction(ApprovalAction action) {
        this.action = action;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovalTimestamp() {
        return approvalTimestamp;
    }

    public void setApprovalTimestamp(LocalDateTime approvalTimestamp) {
        this.approvalTimestamp = approvalTimestamp;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    // equals, hashCode, toString

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof GLTransactionApproval that))
            return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "GLTransactionApproval{" + "id=" + id + ", approvalLevel=" + approvalLevel + ", action=" + action
                + ", approvedBy='" + approvedBy + '\'' + ", approvalTimestamp=" + approvalTimestamp + '}';
    }
}
