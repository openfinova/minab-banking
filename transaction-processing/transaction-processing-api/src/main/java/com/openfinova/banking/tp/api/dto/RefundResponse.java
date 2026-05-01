package com.openfinova.banking.tp.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.tp.api.entity.RefundType;

/**
 * Response DTO for refund transaction operations.
 */
public class RefundResponse {

    private UUID refundTransactionId;
    private UUID originalTransactionId;
    private BigDecimal refundAmount;
    private BigDecimal originalAmount;
    private BigDecimal remainingRefundableAmount;
    private RefundType refundType;
    private String status;
    private String reason;
    private LocalDateTime refundedAt;
    private String currency;
    private UUID destinationAccountId;
    private String destinationAccountNumber;

    public RefundResponse() {
    }

    // Getters and setters

    public UUID getRefundTransactionId() {
        return refundTransactionId;
    }

    public void setRefundTransactionId(UUID refundTransactionId) {
        this.refundTransactionId = refundTransactionId;
    }

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

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getRemainingRefundableAmount() {
        return remainingRefundableAmount;
    }

    public void setRemainingRefundableAmount(BigDecimal remainingRefundableAmount) {
        this.remainingRefundableAmount = remainingRefundableAmount;
    }

    public RefundType getRefundType() {
        return refundType;
    }

    public void setRefundType(RefundType refundType) {
        this.refundType = refundType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(UUID destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public void setDestinationAccountNumber(String destinationAccountNumber) {
        this.destinationAccountNumber = destinationAccountNumber;
    }
}
