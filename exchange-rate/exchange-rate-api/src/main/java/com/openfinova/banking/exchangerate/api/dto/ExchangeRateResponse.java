package com.openfinova.banking.exchangerate.api.dto;

import com.openfinova.banking.exchangerate.api.entity.RateType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for exchange rate operations.
 */
@Schema(description = "Exchange rate response with details")
public class ExchangeRateResponse {

    @Schema(description = "Unique exchange rate identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Source currency code", example = "USD")
    private String sourceCurrency;

    @Schema(description = "Target currency code", example = "EUR")
    private String targetCurrency;

    @Schema(description = "Mid-market exchange rate", example = "0.85")
    private BigDecimal rate;

    @Schema(description = "Bank's buying (bid) rate. Null when spread is not applicable.", example = "0.848")
    private BigDecimal bidRate;

    @Schema(description = "Bank's selling (ask) rate. Null when spread is not applicable.", example = "0.852")
    private BigDecimal askRate;

    @Schema(description = "Date when the rate is effective", example = "2026-02-14")
    private LocalDate rateDate;

    @Schema(description = "Type of exchange rate", example = "SPOT")
    private RateType rateType;

    @Schema(description = "Timestamp when the rate was created", example = "2026-02-14T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "User who created the rate")
    private String createdBy;

    @Schema(description = "Timestamp of the last update")
    private LocalDateTime updatedAt;

    @Schema(description = "User who last updated the rate")
    private String updatedBy;

    @Schema(description = "Optimistic-locking version counter")
    private long version;

    // Constructors
    public ExchangeRateResponse() {
    }

    public ExchangeRateResponse(UUID id, String sourceCurrency, String targetCurrency, BigDecimal rate,
            LocalDate rateDate, RateType rateType, LocalDateTime createdAt) {
        this.id = id;
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
        this.rate = rate;
        this.rateDate = rateDate;
        this.rateType = rateType;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}