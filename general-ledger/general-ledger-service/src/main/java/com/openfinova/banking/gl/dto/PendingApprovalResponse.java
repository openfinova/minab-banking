package com.openfinova.banking.gl.dto;

import com.openfinova.banking.gl.api.entity.GLTransactionSource;
import com.openfinova.banking.gl.api.entity.GLTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for pending approval transactions in approval queue.
 * Contains transaction summary and approval workflow information.
 */
public class PendingApprovalResponse {

    private UUID transactionId;
    private String referenceId;
    private LocalDate transactionDate;
    private String description;
    private String currency;
    private BigDecimal totalAmount;
    private GLTransactionSource source;
    private GLTransactionStatus status;

    // Submission info
    private String submittedBy;
    private LocalDateTime submittedAt;

    // Approval info
    private Integer currentApprovalLevel;
    private Integer requiredApprovals;
    private Integer receivedApprovals;

    // Constructors

    public PendingApprovalResponse() {
    }

    // Getters and Setters

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public GLTransactionSource getSource() {
        return source;
    }

    public void setSource(GLTransactionSource source) {
        this.source = source;
    }

    public GLTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(GLTransactionStatus status) {
        this.status = status;
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

    public Integer getCurrentApprovalLevel() {
        return currentApprovalLevel;
    }

    public void setCurrentApprovalLevel(Integer currentApprovalLevel) {
        this.currentApprovalLevel = currentApprovalLevel;
    }

    public Integer getRequiredApprovals() {
        return requiredApprovals;
    }

    public void setRequiredApprovals(Integer requiredApprovals) {
        this.requiredApprovals = requiredApprovals;
    }

    public Integer getReceivedApprovals() {
        return receivedApprovals;
    }

    public void setReceivedApprovals(Integer receivedApprovals) {
        this.receivedApprovals = receivedApprovals;
    }
}
