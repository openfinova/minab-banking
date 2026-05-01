package com.openfinova.banking.loan.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for collection activity reports.
 */
public class CollectionActivityReportResponse {

    private UUID loanAccountId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private int totalActivities;
    private int completedActivities;
    private int pendingActivities;
    private List<CollectionActivityResponse> activities;

    public UUID getLoanAccountId() {
        return loanAccountId;
    }

    public void setLoanAccountId(UUID loanAccountId) {
        this.loanAccountId = loanAccountId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public int getTotalActivities() {
        return totalActivities;
    }

    public void setTotalActivities(int totalActivities) {
        this.totalActivities = totalActivities;
    }

    public int getCompletedActivities() {
        return completedActivities;
    }

    public void setCompletedActivities(int completedActivities) {
        this.completedActivities = completedActivities;
    }

    public int getPendingActivities() {
        return pendingActivities;
    }

    public void setPendingActivities(int pendingActivities) {
        this.pendingActivities = pendingActivities;
    }

    public List<CollectionActivityResponse> getActivities() {
        return activities;
    }

    public void setActivities(List<CollectionActivityResponse> activities) {
        this.activities = activities;
    }
}
