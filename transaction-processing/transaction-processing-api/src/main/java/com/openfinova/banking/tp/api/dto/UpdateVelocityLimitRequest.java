package com.openfinova.banking.tp.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Request to update velocity limit")
public class UpdateVelocityLimitRequest {

    @Schema(description = "Maximum amount")
    private BigDecimal maxAmount;

    @Schema(description = "Maximum transaction count")
    private Integer maxCount;

    // Getters and setters
    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }
}
