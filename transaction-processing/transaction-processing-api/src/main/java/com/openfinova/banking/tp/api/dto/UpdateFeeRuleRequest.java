package com.openfinova.banking.tp.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Request to update fee rule")
public class UpdateFeeRuleRequest {

    @Schema(description = "Fixed fee amount")
    private BigDecimal fixedAmount;

    @Schema(description = "Percentage rate")
    private BigDecimal percentageRate;

    @Schema(description = "Minimum fee")
    private BigDecimal minFee;

    @Schema(description = "Maximum fee")
    private BigDecimal maxFee;

    @Schema(description = "Is active")
    private Boolean isActive;

    // Getters and setters
    public BigDecimal getFixedAmount() {
        return fixedAmount;
    }

    public void setFixedAmount(BigDecimal fixedAmount) {
        this.fixedAmount = fixedAmount;
    }

    public BigDecimal getPercentageRate() {
        return percentageRate;
    }

    public void setPercentageRate(BigDecimal percentageRate) {
        this.percentageRate = percentageRate;
    }

    public BigDecimal getMinFee() {
        return minFee;
    }

    public void setMinFee(BigDecimal minFee) {
        this.minFee = minFee;
    }

    public BigDecimal getMaxFee() {
        return maxFee;
    }

    public void setMaxFee(BigDecimal maxFee) {
        this.maxFee = maxFee;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
