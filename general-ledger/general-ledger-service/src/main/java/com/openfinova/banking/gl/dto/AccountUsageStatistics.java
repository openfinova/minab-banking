package com.openfinova.banking.gl.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Account usage statistics.
 */
public class AccountUsageStatistics {
    private UUID accountId;
    private int transactionCount;
    private BigDecimal totalActivity;
    private LocalDate lastActivityDate;
    private BigDecimal averageDailyBalance;

    public AccountUsageStatistics() {
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getTotalActivity() {
        return totalActivity;
    }

    public void setTotalActivity(BigDecimal totalActivity) {
        this.totalActivity = totalActivity;
    }

    public LocalDate getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(LocalDate lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public BigDecimal getAverageDailyBalance() {
        return averageDailyBalance;
    }

    public void setAverageDailyBalance(BigDecimal averageDailyBalance) {
        this.averageDailyBalance = averageDailyBalance;
    }
}
