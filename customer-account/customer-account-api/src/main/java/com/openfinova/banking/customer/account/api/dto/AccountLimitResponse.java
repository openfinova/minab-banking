package com.openfinova.banking.customer.account.api.dto;

import com.openfinova.banking.customer.account.api.entity.LimitPeriod;
import com.openfinova.banking.customer.account.api.entity.LimitType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Account limit response")
public class AccountLimitResponse {

    @Schema(description = "Limit ID")
    private UUID id;

    @Schema(description = "Account ID")
    private UUID accountId;

    @Schema(description = "Limit type")
    private LimitType limitType;

    @Schema(description = "Limit period")
    private LimitPeriod limitPeriod;

    @Schema(description = "Maximum amount")
    private BigDecimal maxAmount;

    @Schema(description = "Maximum transaction count")
    private Integer maxCount;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Created by")
    private String createdBy;

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
