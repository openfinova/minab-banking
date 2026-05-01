package com.openfinova.banking.gl.dto;

import java.time.LocalDate;

/**
 * Statistics about snapshot generation for a specific date.
 */
public class SnapshotStatistics {
    private LocalDate date;
    private int totalAccounts;
    private int snapshotsCreated;
    private int snapshotsMissing;
    private int snapshotsWithErrors;
    private long processingTimeMs;

    public SnapshotStatistics() {
    }

    public SnapshotStatistics(LocalDate date, int totalAccounts, int snapshotsCreated, int snapshotsMissing,
            int snapshotsWithErrors, long processingTimeMs) {
        this.date = date;
        this.totalAccounts = totalAccounts;
        this.snapshotsCreated = snapshotsCreated;
        this.snapshotsMissing = snapshotsMissing;
        this.snapshotsWithErrors = snapshotsWithErrors;
        this.processingTimeMs = processingTimeMs;
    }

    // Getters and setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(int totalAccounts) {
        this.totalAccounts = totalAccounts;
    }

    public int getSnapshotsCreated() {
        return snapshotsCreated;
    }

    public void setSnapshotsCreated(int snapshotsCreated) {
        this.snapshotsCreated = snapshotsCreated;
    }

    public int getSnapshotsMissing() {
        return snapshotsMissing;
    }

    public void setSnapshotsMissing(int snapshotsMissing) {
        this.snapshotsMissing = snapshotsMissing;
    }

    public int getSnapshotsWithErrors() {
        return snapshotsWithErrors;
    }

    public void setSnapshotsWithErrors(int snapshotsWithErrors) {
        this.snapshotsWithErrors = snapshotsWithErrors;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
}
