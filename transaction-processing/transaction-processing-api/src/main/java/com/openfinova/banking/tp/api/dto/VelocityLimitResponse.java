package com.openfinova.banking.tp.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.api.entity.VelocityLimitPeriod;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Velocity limit response")
public class VelocityLimitResponse {

    @Schema(description = "Limit ID")
    private UUID id;

    @Schema(description = "Account ID")
    private UUID accountId;

    @Schema(description = "Transaction type")
    private TransactionType transactionType;

    @Schema(description = "Limit period")
    private VelocityLimitPeriod period;

    @Schema(description = "Maximum amount")
    private BigDecimal maxAmount;

    @Schema(description = "Maximum transaction count")
    private Integer maxCount;

    @Schema(description = "Current amount used")
    private BigDecimal currentAmount;

    @Schema(description = "Current transaction count")
    private Integer currentCount;

    @Schema(description = "Period start time")
    private LocalDateTime periodStart;

    @Schema(description = "Period end time")
    private LocalDateTime periodEnd;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

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

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = currentAmount;
    }

    public Integer getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(Integer currentCount) {
        this.currentCount = currentCount;
    }

    public LocalDateTime getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDateTime periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
