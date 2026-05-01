package com.openfinova.banking.tp.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing current usage statistics for a velocity limit period.
 */
public class LimitUsage {
    private Integer currentCount;
    private BigDecimal currentAmount;
    private Integer maxCount;
    private BigDecimal maxAmount;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime lastTransactionAt;

    public LimitUsage() {
    }

    public LimitUsage(Integer currentCount, BigDecimal currentAmount, Integer maxCount, BigDecimal maxAmount,
            LocalDateTime periodStart, LocalDateTime periodEnd) {
        this.currentCount = currentCount;
        this.currentAmount = currentAmount;
        this.maxCount = maxCount;
        this.maxAmount = maxAmount;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    // Getters and setters
    public Integer getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(Integer currentCount) {
        this.currentCount = currentCount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = currentAmount;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
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

    public LocalDateTime getLastTransactionAt() {
        return lastTransactionAt;
    }

    public void setLastTransactionAt(LocalDateTime lastTransactionAt) {
        this.lastTransactionAt = lastTransactionAt;
    }

    /**
     * Calculates the usage percentage for count limits
     */
    public Double getCountUsagePercentage() {
        if (maxCount == null || maxCount == 0) {
            return null;
        }
        return (currentCount.doubleValue() / maxCount.doubleValue()) * 100.0;
    }

    /**
     * Calculates the usage percentage for amount limits
     */
    public Double getAmountUsagePercentage() {
        if (maxAmount == null || maxAmount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return currentAmount.divide(maxAmount, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    @Override
    public String toString() {
        return "LimitUsage{" + "currentCount=" + currentCount + ", currentAmount=" + currentAmount + ", maxCount="
                + maxCount + ", maxAmount=" + maxAmount + ", periodStart=" + periodStart + ", periodEnd=" + periodEnd
                + '}';
    }
}