package com.openfinova.banking.tp.api.dto;

import java.time.Duration;
import java.time.LocalDate;

/**
 * DTO for daily compensation workflow metrics.
 * Provides day-by-day breakdown of workflow performance.
 */
public class DailyWorkflowMetrics {

    private LocalDate date;
    private int totalWorkflows;
    private int completedWorkflows;
    private int failedWorkflows;
    private int escalatedWorkflows;
    private Duration averageCompletionTime;
    private double successRate;

    // Default constructor
    public DailyWorkflowMetrics() {
    }

    // Constructor with essential fields
    public DailyWorkflowMetrics(LocalDate date, int totalWorkflows, int completedWorkflows, int failedWorkflows) {
        this.date = date;
        this.totalWorkflows = totalWorkflows;
        this.completedWorkflows = completedWorkflows;
        this.failedWorkflows = failedWorkflows;
        this.successRate = totalWorkflows > 0 ? (double) completedWorkflows / totalWorkflows : 0.0;
    }

    // Getters and setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getTotalWorkflows() {
        return totalWorkflows;
    }

    public void setTotalWorkflows(int totalWorkflows) {
        this.totalWorkflows = totalWorkflows;
    }

    public int getCompletedWorkflows() {
        return completedWorkflows;
    }

    public void setCompletedWorkflows(int completedWorkflows) {
        this.completedWorkflows = completedWorkflows;
    }

    public int getFailedWorkflows() {
        return failedWorkflows;
    }

    public void setFailedWorkflows(int failedWorkflows) {
        this.failedWorkflows = failedWorkflows;
    }

    public int getEscalatedWorkflows() {
        return escalatedWorkflows;
    }

    public void setEscalatedWorkflows(int escalatedWorkflows) {
        this.escalatedWorkflows = escalatedWorkflows;
    }

    public Duration getAverageCompletionTime() {
        return averageCompletionTime;
    }

    public void setAverageCompletionTime(Duration averageCompletionTime) {
        this.averageCompletionTime = averageCompletionTime;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    @Override
    public String toString() {
        return "DailyWorkflowMetrics{" + "date=" + date + ", totalWorkflows=" + totalWorkflows + ", completedWorkflows="
                + completedWorkflows + ", failedWorkflows=" + failedWorkflows + ", successRate=" + successRate + '}';
    }
}