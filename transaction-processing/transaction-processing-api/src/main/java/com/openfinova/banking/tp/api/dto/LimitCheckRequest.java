package com.openfinova.banking.tp.api.dto;

import com.openfinova.banking.tp.api.entity.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for batch limit checking requests.
 */
public class LimitCheckRequest {
    private UUID accountId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String currency;

    public LimitCheckRequest() {
    }

    public LimitCheckRequest(UUID accountId, TransactionType transactionType, BigDecimal amount, String currency) {
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.currency = currency;
    }

    // Getters and setters
    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
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

    @Override
    public String toString() {
        return "LimitCheckRequest{" + "accountId=" + accountId + ", transactionType=" + transactionType + ", amount="
                + amount + ", currency='" + currency + '\'' + '}';
    }
}