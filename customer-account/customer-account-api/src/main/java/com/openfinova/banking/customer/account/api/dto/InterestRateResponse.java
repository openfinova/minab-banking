package com.openfinova.banking.customer.account.api.dto;

import com.openfinova.banking.customer.account.api.entity.InterestRateType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Interest rate response")
public class InterestRateResponse {

    @Schema(description = "Rate ID")
    private UUID id;

    @Schema(description = "Account ID")
    private UUID accountId;

    @Schema(description = "Rate type")
    private InterestRateType rateType;

    @Schema(description = "Annual percentage rate")
    private BigDecimal annualPercentageRate;

    @Schema(description = "Effective from date/time")
    private LocalDateTime effectiveFrom;

    @Schema(description = "Effective until date/time")
    private LocalDateTime effectiveUntil;

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

    public InterestRateType getRateType() {
        return rateType;
    }

    public void setRateType(InterestRateType rateType) {
        this.rateType = rateType;
    }

    public BigDecimal getAnnualPercentageRate() {
        return annualPercentageRate;
    }

    public void setAnnualPercentageRate(BigDecimal annualPercentageRate) {
        this.annualPercentageRate = annualPercentageRate;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getEffectiveUntil() {
        return effectiveUntil;
    }

    public void setEffectiveUntil(LocalDateTime effectiveUntil) {
        this.effectiveUntil = effectiveUntil;
    }
}
