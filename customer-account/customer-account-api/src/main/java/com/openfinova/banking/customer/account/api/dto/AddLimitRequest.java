package com.openfinova.banking.customer.account.api.dto;

import com.openfinova.banking.customer.account.api.entity.LimitPeriod;
import com.openfinova.banking.customer.account.api.entity.LimitType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request to add a limit to an account")
public class AddLimitRequest {

    @NotNull(message = "Limit type is required")
    @Schema(description = "Limit type", required = true)
    private LimitType limitType;

    @NotNull(message = "Limit period is required")
    @Schema(description = "Limit period", required = true)
    private LimitPeriod limitPeriod;

    @Schema(description = "Maximum amount")
    private BigDecimal maxAmount;

    @Schema(description = "Maximum transaction count")
    private Integer maxCount;

    // Getters and setters
    public LimitType getLimitType() {
        return limitType;
    }

    public void setLimitType(LimitType limitType) {
        this.limitType = limitType;
    }

    public LimitPeriod getLimitPeriod() {
        return limitPeriod;
    }

    public void setLimitPeriod(LimitPeriod limitPeriod) {
        this.limitPeriod = limitPeriod;
    }

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
