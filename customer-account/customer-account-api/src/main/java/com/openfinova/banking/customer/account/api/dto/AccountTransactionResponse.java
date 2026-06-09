package com.openfinova.banking.customer.account.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.customer.account.api.entity.AccountTransactionDirection;
import com.openfinova.banking.customer.account.api.entity.AccountTransactionType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Account transaction response")
public class AccountTransactionResponse {

    @Schema(description = "Transaction ID")
    private UUID id;

    @Schema(description = "Account ID")
    private UUID accountId;

    @Schema(description = "Account number for display when listing across multiple accounts")
    private String accountNumber;

    @Schema(description = "Transaction type")
    private AccountTransactionType transactionType;

    @Schema(description = "Debit or credit indicator derived from transaction type")
    private AccountTransactionDirection direction;

    @Schema(description = "Transaction status (e.g. PENDING, POSTED)")
    private String status;

    @Schema(description = "Transaction amount")
    private BigDecimal amount;

    @Schema(description = "Currency")
    private String currency;

    @Schema(description = "Transaction date/time")
    private LocalDateTime transactionDate;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Reference ID")
    private String referenceId;

    @Schema(description = "GL transaction ID")
    private UUID glTransactionId;

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(AccountTransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public AccountTransactionDirection getDirection() {
        return direction;
    }

    public void setDirection(AccountTransactionDirection direction) {
        this.direction = direction;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public UUID getGlTransactionId() {
        return glTransactionId;
    }

    public void setGlTransactionId(UUID glTransactionId) {
        this.glTransactionId = glTransactionId;
    }
}
