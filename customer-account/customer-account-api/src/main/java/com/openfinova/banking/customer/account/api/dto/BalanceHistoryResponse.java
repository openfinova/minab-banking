package com.openfinova.banking.customer.account.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for balance history and trend analysis.
 * Provides a comprehensive view of balance movements over time,
 * including statistical analysis and trend detection.
 */
public class BalanceHistoryResponse {

    private UUID customerAccountId;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<BalanceHistoryEntry> balanceHistory;
    private BalanceTrendAnalysis trendAnalysis;

    public BalanceHistoryResponse() {
    }

    public BalanceHistoryResponse(UUID customerAccountId, LocalDate startDate, LocalDate endDate) {
        this.customerAccountId = customerAccountId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Individual entry for a specific date's balance state.
     */
    public static class BalanceHistoryEntry {
        private LocalDate date;
        private BigDecimal balance;
        private BigDecimal change;
        private String changeReason;

        public BalanceHistoryEntry() {
        }

        public BalanceHistoryEntry(LocalDate date, BigDecimal balance) {
            this.date = date;
            this.balance = balance;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }

        public BigDecimal getChange() {
            return change;
        }

        public void setChange(BigDecimal change) {
            this.change = change;
        }

        public String getChangeReason() {
            return changeReason;
        }

        public void setChangeReason(String changeReason) {
            this.changeReason = changeReason;
        }
    }

    /**
     * Statistical analysis of balance movements over the requested period.
     */
    public static class BalanceTrendAnalysis {
        private BigDecimal averageBalance;
        private BigDecimal minimumBalance;
        private BigDecimal maximumBalance;
        private BigDecimal totalChange;
        private BigDecimal averageDailyChange;

        /**
         * Calculated trend indicator (e.g., INCREASING, DECREASING, STABLE).
         */
        private String trend;

        public BalanceTrendAnalysis() {
        }

        public BigDecimal getAverageBalance() {
            return averageBalance;
        }

        public void setAverageBalance(BigDecimal averageBalance) {
            this.averageBalance = averageBalance;
        }

        public BigDecimal getMinimumBalance() {
            return minimumBalance;
        }

        public void setMinimumBalance(BigDecimal minimumBalance) {
            this.minimumBalance = minimumBalance;
        }

        public BigDecimal getMaximumBalance() {
            return maximumBalance;
        }

        public void setMaximumBalance(BigDecimal maximumBalance) {
            this.maximumBalance = maximumBalance;
        }

        public BigDecimal getTotalChange() {
            return totalChange;
        }

        public void setTotalChange(BigDecimal totalChange) {
            this.totalChange = totalChange;
        }

        public BigDecimal getAverageDailyChange() {
            return averageDailyChange;
        }

        public void setAverageDailyChange(BigDecimal averageDailyChange) {
            this.averageDailyChange = averageDailyChange;
        }

        public String getTrend() {
            return trend;
        }

        public void setTrend(String trend) {
            this.trend = trend;
        }
    }

    public UUID getCustomerAccountId() {
        return customerAccountId;
    }

    public void setCustomerAccountId(UUID customerAccountId) {
        this.customerAccountId = customerAccountId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<BalanceHistoryEntry> getBalanceHistory() {
        return balanceHistory;
    }

    public void setBalanceHistory(List<BalanceHistoryEntry> balanceHistory) {
        this.balanceHistory = balanceHistory;
    }

    public BalanceTrendAnalysis getTrendAnalysis() {
        return trendAnalysis;
    }

    public void setTrendAnalysis(BalanceTrendAnalysis trendAnalysis) {
        this.trendAnalysis = trendAnalysis;
    }
}
