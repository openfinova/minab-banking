package com.openfinova.banking.gl.dto;

import java.util.UUID;

/**
 * Response DTO indicating whether a user can approve a specific transaction.
 * Includes reason if approval is not allowed.
 */
public class CanApproveResponse {

    private UUID transactionId;
    private boolean canApprove;
    private String reason;

    // Constructors

    public CanApproveResponse() {
    }

    public CanApproveResponse(UUID transactionId, boolean canApprove, String reason) {
        this.transactionId = transactionId;
        this.canApprove = canApprove;
        this.reason = reason;
    }

    // Static factory methods for convenience

    public static CanApproveResponse allowed(UUID transactionId) {
        return new CanApproveResponse(transactionId, true, "User can approve this transaction");
    }

    public static CanApproveResponse denied(UUID transactionId, String reason) {
        return new CanApproveResponse(transactionId, false, reason);
    }

    // Getters and Setters

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public boolean isCanApprove() {
        return canApprove;
    }

    public void setCanApprove(boolean canApprove) {
        this.canApprove = canApprove;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
