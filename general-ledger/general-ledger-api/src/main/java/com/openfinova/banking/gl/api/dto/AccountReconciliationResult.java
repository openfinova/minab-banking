package com.openfinova.banking.gl.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Reconciliation result for a single account.
 */
public class AccountReconciliationResult {
    private UUID accountId;
    private LocalDate date;
    private boolean consistent;
    private String snapshotBalance;
    private String calculatedBalance;
    private String difference;
    private List<String> issues;

    public AccountReconciliationResult() {
    }

    public AccountReconciliationResult(UUID accountId, LocalDate date, boolean consistent) {
        this.accountId = accountId;
        this.date = date;
        this.consistent = consistent;
    }

    // Getters and setters
    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isConsistent() {
        return consistent;
    }

    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }

    public String getSnapshotBalance() {
        return snapshotBalance;
    }

    public void setSnapshotBalance(String snapshotBalance) {
        this.snapshotBalance = snapshotBalance;
    }

    public String getCalculatedBalance() {
        return calculatedBalance;
    }

    public void setCalculatedBalance(String calculatedBalance) {
        this.calculatedBalance = calculatedBalance;
    }

    public String getDifference() {
        return difference;
    }

    public void setDifference(String difference) {
        this.difference = difference;
    }

    public List<String> getIssues() {
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues;
    }
}
