package com.openfinova.banking.customer.account.api.dto;

import com.openfinova.banking.customer.account.api.entity.AccountTransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Request to record a transaction")
public class RecordTransactionRequest {

    @NotNull(message = "Transaction type is required")
    @Schema(description = "Transaction type", required = true)
    private AccountTransactionType transactionType;

    @NotNull(message = "Amount is required")
    @Schema(description = "Transaction amount", required = true)
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Schema(description = "Currency code", required = true)
    private String currency;

    @NotNull(message = "Transaction date is required")
    @Schema(description = "Transaction date/time", required = true)
    private LocalDateTime transactionDate;

    @Schema(description = "Transaction description")
    private String description;

    @Schema(description = "External reference ID")
    private String referenceId;

    // Getters and setters
    public AccountTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(AccountTransactionType transactionType) {
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
}
