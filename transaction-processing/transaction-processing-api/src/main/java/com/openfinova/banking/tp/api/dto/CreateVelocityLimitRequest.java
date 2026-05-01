package com.openfinova.banking.tp.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.api.entity.VelocityLimitPeriod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to create velocity limit")
public class CreateVelocityLimitRequest {

    @NotNull(message = "Account ID is required")
    @Schema(description = "Account ID", required = true)
    private UUID accountId;

    @NotNull(message = "Transaction type is required")
    @Schema(description = "Transaction type", required = true)
    private TransactionType transactionType;

    @NotNull(message = "Period is required")
    @Schema(description = "Limit period", required = true)
    private VelocityLimitPeriod period;

    @Schema(description = "Maximum amount")
    private BigDecimal maxAmount;

    @Schema(description = "Maximum transaction count")
    private Integer maxCount;

    // Getters and setters
    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public VelocityLimitPeriod getPeriod() {
        return period;
    }

    public void setPeriod(VelocityLimitPeriod period) {
        this.period = period;
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
