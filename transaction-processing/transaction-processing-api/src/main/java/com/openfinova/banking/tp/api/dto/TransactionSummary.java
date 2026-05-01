package com.openfinova.banking.tp.api.dto;

import com.openfinova.banking.tp.api.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for transaction summary data used in optimized reporting queries.
 * Contains minimal transaction information to reduce memory usage and improve performance.
 */
public class TransactionSummary {

    private UUID transactionId;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private LocalDateTime createdAt;

    // Constructors
    public TransactionSummary() {
    }

    public TransactionSummary(UUID transactionId, BigDecimal amount, String currency, TransactionStatus status,
            LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
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

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TransactionSummary{" + "transactionId=" + transactionId + ", amount=" + amount + ", currency='"
                + currency + '\'' + ", status=" + status + ", createdAt=" + createdAt + '}';
    }
}