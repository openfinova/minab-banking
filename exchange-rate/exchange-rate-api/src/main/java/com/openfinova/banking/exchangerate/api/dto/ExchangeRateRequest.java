package com.openfinova.banking.exchangerate.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.exchangerate.api.entity.RateType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating or updating exchange rates.
 */
@Schema(description = "Request to create or update an exchange rate")
public class ExchangeRateRequest {

    @NotBlank(message = "Source currency is required")
    @ValidCurrency
    @Schema(description = "Source currency code", example = "USD")
    private String sourceCurrency;

    @NotBlank(message = "Target currency is required")
    @ValidCurrency
    @Schema(description = "Target currency code", example = "EUR")
    private String targetCurrency;

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Rate must be positive")
    @Schema(description = "Mid-market exchange rate", example = "0.85")
    private BigDecimal rate;

    @DecimalMin(value = "0.0", inclusive = false, message = "Bid rate must be positive")
    @Schema(description = "Bank's buying (bid) rate — must be ≤ mid rate. Optional; omit for non-SPOT rate types.", example = "0.848")
    private BigDecimal bidRate;

    @DecimalMin(value = "0.0", inclusive = false, message = "Ask rate must be positive")
    @Schema(description = "Bank's selling (ask) rate — must be ≥ mid rate. Optional; omit for non-SPOT rate types.", example = "0.852")
    private BigDecimal askRate;

    @NotNull(message = "Rate date is required")
    @Schema(description = "Date when the rate is effective", example = "2026-02-14")
    private LocalDate rateDate;

    @JsonProperty("rateType")
    @NotNull(message = "Rate type is required")
    @Schema(description = "Type of exchange rate", example = "SPOT")
    private RateType rateType;

    @Schema(description = "User creating the rate, for audit", example = "treasury-ops")
    private String createdBy;

    @Schema(description = "User updating the rate, for audit", example = "treasury-ops")
    private String updatedBy;

    // Constructors
    public ExchangeRateRequest() {
    }

    public ExchangeRateRequest(String sourceCurrency, String targetCurrency, BigDecimal rate, LocalDate rateDate,
            RateType rateType) {
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
        this.rate = rate;
        this.rateDate = rateDate;
        this.rateType = rateType;
    }

    // Getters and Setters
    public String getSourceCurrency() {
        return sourceCurrency;
    }

    public void setSourceCurrency(String sourceCurrency) {
        this.sourceCurrency = sourceCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public void setTargetCurrency(String targetCurrency) {
        this.targetCurrency = targetCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public LocalDate getRateDate() {
        return rateDate;
    }

    public void setRateDate(LocalDate rateDate) {
        this.rateDate = rateDate;
    }

    public RateType getRateType() {
        return rateType;
    }

    public void setRateType(RateType rateType) {
        this.rateType = rateType;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public BigDecimal getBidRate() {
        return bidRate;
    }

    public void setBidRate(BigDecimal bidRate) {
        this.bidRate = bidRate;
    }

    public BigDecimal getAskRate() {
        return askRate;
    }

    public void setAskRate(BigDecimal askRate) {
        this.askRate = askRate;
    }

    // ---------------------------------------------------------------------------
    // Cross-field spread validation (bid ≤ mid ≤ ask)
    // ---------------------------------------------------------------------------

    /**
     * Bid rate must be ≤ mid rate when present.
     * Enforced by Bean Validation so the controller returns HTTP 400 on violation.
     */
    @AssertTrue(message = "Bid rate must be less than or equal to the mid rate")
    public boolean isBidRateValid() {
        if (bidRate == null || rate == null) {
            return true; // skip when either field is absent
        }
        return bidRate.compareTo(rate) <= 0;
    }

    /**
     * Ask rate must be ≥ mid rate when present.
     * Enforced by Bean Validation so the controller returns HTTP 400 on violation.
     */
    @AssertTrue(message = "Ask rate must be greater than or equal to the mid rate")
    public boolean isAskRateValid() {
        if (askRate == null || rate == null) {
            return true; // skip when either field is absent
        }
        return askRate.compareTo(rate) >= 0;
    }
}