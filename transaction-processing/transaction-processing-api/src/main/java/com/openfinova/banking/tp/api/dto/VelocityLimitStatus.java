package com.openfinova.banking.tp.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.api.entity.VelocityLimitPeriod;

/**
 * DTO representing the current status of velocity limits for an account.
 * Provides comprehensive information about limit usage and remaining capacity.
 */
public class VelocityLimitStatus {
    private UUID accountId;
    private TransactionType transactionType;
    private Map<VelocityLimitPeriod, LimitUsage> currentUsage;
    private Map<VelocityLimitPeriod, BigDecimal> remainingAmounts;
    private Map<VelocityLimitPeriod, Integer> remainingCounts;
    private List<VelocityLimitBreachDTO> recentBreaches;
    private LocalDateTime statusTimestamp;

    public VelocityLimitStatus() {
    }

    public VelocityLimitStatus(UUID accountId, TransactionType transactionType, LocalDateTime statusTimestamp) {
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.statusTimestamp = statusTimestamp;
    }

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

    public Map<VelocityLimitPeriod, LimitUsage> getCurrentUsage() {
        return currentUsage;
    }

    public void setCurrentUsage(Map<VelocityLimitPeriod, LimitUsage> currentUsage) {
        this.currentUsage = currentUsage;
    }

    public Map<VelocityLimitPeriod, BigDecimal> getRemainingAmounts() {
        return remainingAmounts;
    }

    public void setRemainingAmounts(Map<VelocityLimitPeriod, BigDecimal> remainingAmounts) {
        this.remainingAmounts = remainingAmounts;
    }

    public Map<VelocityLimitPeriod, Integer> getRemainingCounts() {
        return remainingCounts;
    }

    public void setRemainingCounts(Map<VelocityLimitPeriod, Integer> remainingCounts) {
        this.remainingCounts = remainingCounts;
    }

    public List<VelocityLimitBreachDTO> getRecentBreaches() {
        return recentBreaches;
    }

    public void setRecentBreaches(List<VelocityLimitBreachDTO> recentBreaches) {
        this.recentBreaches = recentBreaches;
    }

    public LocalDateTime getStatusTimestamp() {
        return statusTimestamp;
    }

    public void setStatusTimestamp(LocalDateTime statusTimestamp) {
        this.statusTimestamp = statusTimestamp;
    }

    @Override
    public String toString() {
        return "VelocityLimitStatus{" + "accountId=" + accountId + ", transactionType=" + transactionType
                + ", statusTimestamp=" + statusTimestamp + '}';
    }
}