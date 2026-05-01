package com.openfinova.banking.gl.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Reconciliation report for multiple accounts or a period.
 */
public class BalanceReconciliationReport {
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalAccountsChecked;
    private int consistentAccounts;
    private int inconsistentAccounts;
    private List<AccountReconciliationResult> inconsistencies;
    private long processingTimeMs;

    public BalanceReconciliationReport() {
    }

    // Getters and setters
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

    public int getTotalAccountsChecked() {
        return totalAccountsChecked;
    }

    public void setTotalAccountsChecked(int totalAccountsChecked) {
        this.totalAccountsChecked = totalAccountsChecked;
    }

    public int getConsistentAccounts() {
        return consistentAccounts;
    }

    public void setConsistentAccounts(int consistentAccounts) {
        this.consistentAccounts = consistentAccounts;
    }

    public int getInconsistentAccounts() {
        return inconsistentAccounts;
    }

    public void setInconsistentAccounts(int inconsistentAccounts) {
        this.inconsistentAccounts = inconsistentAccounts;
    }

    public List<AccountReconciliationResult> getInconsistencies() {
        return inconsistencies;
    }

    public void setInconsistencies(List<AccountReconciliationResult> inconsistencies) {
        this.inconsistencies = inconsistencies;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
}
