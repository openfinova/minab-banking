package com.openfinova.banking.gl.dto;

import com.openfinova.banking.gl.api.entity.ApprovalAction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for approval record details.
 * Used to return approval history and audit trail information.
 */
public class ApprovalResponse {

    private UUID id;
    private UUID transactionId;
    private Integer approvalLevel;
    private ApprovalAction action;
    private String approvedBy;
    private LocalDateTime approvalTimestamp;
    private String comments;
    private String ipAddress;

    // Constructors

    public ApprovalResponse() {
    }

    public ApprovalResponse(UUID id, UUID transactionId, Integer approvalLevel, ApprovalAction action,
            String approvedBy, LocalDateTime approvalTimestamp, String comments) {
        this.id = id;
        this.transactionId = transactionId;
        this.approvalLevel = approvalLevel;
        this.action = action;
        this.approvedBy = approvedBy;
        this.approvalTimestamp = approvalTimestamp;
        this.comments = comments;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
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
}
