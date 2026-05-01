package com.openfinova.banking.gl.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Recovery result from failed operations.
 */
public class SnapshotRecoveryResult {
    private LocalDate date;
    private boolean successful;
    private List<String> recoveryActions;
    private int snapshotsRecovered;
    private List<String> remainingIssues;

    public SnapshotRecoveryResult() {
    }

    // Getters and setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public List<String> getRecoveryActions() {
        return recoveryActions;
    }

    public void setRecoveryActions(List<String> recoveryActions) {
        this.recoveryActions = recoveryActions;
    }

    public int getSnapshotsRecovered() {
        return snapshotsRecovered;
    }

    public void setSnapshotsRecovered(int snapshotsRecovered) {
        this.snapshotsRecovered = snapshotsRecovered;
    }

    public List<String> getRemainingIssues() {
        return remainingIssues;
    }

    public void setRemainingIssues(List<String> remainingIssues) {
        this.remainingIssues = remainingIssues;
    }
}
