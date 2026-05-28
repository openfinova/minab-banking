package com.openfinova.banking.exchangerate.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.openfinova.banking.common.lib.validation.ValidCurrency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for currency conversion operations.
 */
@Schema(description = "Request to convert an amount from one currency to another")
public class CurrencyConversionRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be positive")
    @Schema(description = "Amount to convert", example = "1000.00")
    private BigDecimal amount;

    @NotBlank(message = "Source currency is required")
    @ValidCurrency(message = "Source currency must be a valid ISO 4217 code")
    @Schema(description = "Source currency code", example = "USD")
    private String fromCurrency;

    @NotBlank(message = "Target currency is required")
    @ValidCurrency(message = "Target currency must be a valid ISO 4217 code")
    @Schema(description = "Target currency code", example = "EUR")
    private String toCurrency;

    @Schema(description = "Conversion date (optional, defaults to current date)", example = "2026-02-14")
    private LocalDate conversionDate; // Optional, defaults to current date

    // Constructors
    public CurrencyConversionRequest() {
    }

    public CurrencyConversionRequest(BigDecimal amount, String fromCurrency, String toCurrency,
            LocalDate conversionDate) {
        this.amount = amount;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.conversionDate = conversionDate;
    }

    // Getters and Setters
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public LocalDate getConversionDate() {
        return conversionDate;
    }

    public void setConversionDate(LocalDate conversionDate) {
        this.conversionDate = conversionDate;
    }
}