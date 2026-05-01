package com.openfinova.banking.gl.api.dto;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Journal entry within a transaction")
public class JournalEntryRequest {

    @NotNull(message = "Account ID is required")
    @Schema(description = "GL account ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID accountId;

    @NotNull(message = "Debit amount is required")
    @DecimalMin(value = "0.0", message = "Debit amount must be non-negative")
    @Schema(description = "Debit amount (0 if credit entry)", example = "1000.00")
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @NotNull(message = "Credit amount is required")
    @DecimalMin(value = "0.0", message = "Credit amount must be non-negative")
    @Schema(description = "Credit amount (0 if debit entry)", example = "0.00")
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @NotNull(message = "Exchange rate is required")
    @DecimalMin(value = "0.0001", message = "Exchange rate must be positive")
    @Schema(description = "Exchange rate to base currency (1.0 for same-currency entries)", example = "1.0")
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @NotBlank(message = "Currency is required")
    @ValidCurrency
    @Schema(description = "Three-letter ISO currency code", example = "USD")
    private String currency;

    @Schema(description = "Entry description", example = "Cash deposit")
    private String description;

    // Constructors
    public JournalEntryRequest() {
    }

    public JournalEntryRequest(UUID accountId, BigDecimal debitAmount, BigDecimal creditAmount, String currency) {
        this.accountId = accountId;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.currency = currency;
    }

    // Getters and Setters
    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
