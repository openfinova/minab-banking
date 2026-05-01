package com.openfinova.banking.exchangerate.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for currency conversion operations.
 */
@Schema(description = "Currency conversion result with exchange rate details")
public class CurrencyConversionResponse {

    @Schema(description = "Original amount before conversion", example = "1000.00")
    private BigDecimal originalAmount;

    @Schema(description = "Source currency code", example = "USD")
    private String fromCurrency;

    @Schema(description = "Converted amount", example = "850.00")
    private BigDecimal convertedAmount;

    @Schema(description = "Target currency code", example = "EUR")
    private String toCurrency;

    @Schema(description = "Exchange rate used for conversion", example = "0.85")
    private BigDecimal exchangeRate;

    @Schema(description = "Date of the conversion", example = "2026-02-14")
    private LocalDate conversionDate;

    // Constructors
    public CurrencyConversionResponse() {
    }

    public CurrencyConversionResponse(BigDecimal originalAmount, String fromCurrency, BigDecimal convertedAmount,
            String toCurrency, BigDecimal exchangeRate, LocalDate conversionDate) {
        this.originalAmount = originalAmount;
        this.fromCurrency = fromCurrency;
        this.convertedAmount = convertedAmount;
        this.toCurrency = toCurrency;
        this.exchangeRate = exchangeRate;
        this.conversionDate = conversionDate;
    }

    // Getters and Setters
    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public BigDecimal getConvertedAmount() {
        return convertedAmount;
    }

    public void setConvertedAmount(BigDecimal convertedAmount) {
        this.convertedAmount = convertedAmount;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public LocalDate getConversionDate() {
        return conversionDate;
    }

    public void setConversionDate(LocalDate conversionDate) {
        this.conversionDate = conversionDate;
    }
}