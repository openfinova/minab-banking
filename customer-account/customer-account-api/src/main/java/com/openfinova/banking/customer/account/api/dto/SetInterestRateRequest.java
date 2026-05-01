package com.openfinova.banking.customer.account.api.dto;

import com.openfinova.banking.customer.account.api.entity.InterestRateType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Request to set interest rate")
public class SetInterestRateRequest {

    @NotNull(message = "Rate type is required")
    @Schema(description = "Rate type (CREDIT or DEBIT)", required = true)
    private InterestRateType rateType;

    @NotNull(message = "Annual percentage rate is required")
    @Schema(description = "Annual percentage rate (e.g., 5.0 for 5%)", required = true)
    private BigDecimal annualPercentageRate;

    @NotNull(message = "Effective from date is required")
    @Schema(description = "Date/time when the rate becomes effective", required = true)
    private LocalDateTime effectiveFrom;

    // Getters and setters
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
}
