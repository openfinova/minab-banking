package com.openfinova.banking.tp.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.openfinova.banking.tp.api.entity.RefundType;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for initiating a refund transaction.
 * Supports both full and partial refunds of a previous transaction.
 */
public class RefundRequest {

    @NotNull(message = "Original transaction ID is required")
    private UUID originalTransactionId;

    private BigDecimal refundAmount;

    @NotNull(message = "Refund reason is required")
    private String reason;

    private RefundType refundType;

    private String initiatedBy;

    private String customerReference;

    public RefundRequest() {
    }

    public RefundRequest(UUID originalTransactionId, BigDecimal refundAmount, String reason) {
        this.originalTransactionId = originalTransactionId;
        this.refundAmount = refundAmount;
        this.reason = reason;
    }

    // Getters and setters

    public UUID getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(UUID originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public RefundType getRefundType() {
        return refundType;
    }

    public void setRefundType(RefundType refundType) {
        this.refundType = refundType;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public void setCustomerReference(String customerReference) {
        this.customerReference = customerReference;
    }

    @Override
    public String toString() {
        return "RefundRequest{" + "originalTransactionId=" + originalTransactionId + ", refundAmount=" + refundAmount
                + ", reason='" + reason + '\'' + ", refundType='" + refundType + '\'' + '}';
    }
}
