package com.openfinova.banking.gl.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Repair result with statistics.
 */
public class RepairResult {
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalSnapshotsChecked;
    private int snapshotsRepaired;
    private int snapshotsRecreated;
    private List<String> repairActions;
    private long processingTimeMs;

    public RepairResult() {
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

    public int getTotalSnapshotsChecked() {
        return totalSnapshotsChecked;
    }

    public void setTotalSnapshotsChecked(int totalSnapshotsChecked) {
        this.totalSnapshotsChecked = totalSnapshotsChecked;
    }

    public int getSnapshotsRepaired() {
        return snapshotsRepaired;
    }

    public void setSnapshotsRepaired(int snapshotsRepaired) {
        this.snapshotsRepaired = snapshotsRepaired;
    }

    public int getSnapshotsRecreated() {
        return snapshotsRecreated;
    }

    public void setSnapshotsRecreated(int snapshotsRecreated) {
        this.snapshotsRecreated = snapshotsRecreated;
    }

    public List<String> getRepairActions() {
        return repairActions;
    }

    public void setRepairActions(List<String> repairActions) {
        this.repairActions = repairActions;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
}
