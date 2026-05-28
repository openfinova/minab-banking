package com.openfinova.banking.tp.api.dto;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.openfinova.banking.tp.api.entity.CompensationStatus;
import com.openfinova.banking.tp.api.entity.TransactionType;

/**
 * DTO for compensation workflow reporting and analytics.
 * Provides comprehensive metrics and summaries for workflow monitoring.
 */
public class CompensationWorkflowReport {

    private LocalDate reportStartDate;
    private LocalDate reportEndDate;
    private LocalDate reportGeneratedDate;

    // Summary metrics
    private int totalWorkflows;
    private int completedWorkflows;
    private int failedWorkflows;
    private int activeWorkflows;
    private int escalatedWorkflows;
    private int cancelledWorkflows;

    // Performance metrics
    private Duration averageCompletionTime;
    private Duration medianCompletionTime;
    private Duration maxCompletionTime;
    private Duration minCompletionTime;

    // Success rates
    private double successRate;
    private double escalationRate;
    private double retryRate;

    // Breakdown by transaction type
    private Map<TransactionType, Integer> workflowsByTransactionType;
    private Map<TransactionType, Duration> averageCompletionTimeByType;
    private Map<TransactionType, Double> successRateByType;

    // Breakdown by status
    private Map<CompensationStatus, Integer> workflowsByStatus;

    // Detailed workflow summaries
    private List<CompensationWorkflowSummary> workflowSummaries;

    // Trend data
    private List<DailyWorkflowMetrics> dailyMetrics;

    // Top failure reasons
    private Map<String, Integer> topFailureReasons;

    // Default constructor
    public CompensationWorkflowReport() {
    }

    // Constructor with date range
    public CompensationWorkflowReport(LocalDate startDate, LocalDate endDate, LocalDate reportGeneratedDate) {
        this.reportStartDate = startDate;
        this.reportEndDate = endDate;
        this.reportGeneratedDate = reportGeneratedDate;
    }

    // Getters and setters
    public LocalDate getReportStartDate() {
        return reportStartDate;
    }

    public void setReportStartDate(LocalDate reportStartDate) {
        this.reportStartDate = reportStartDate;
    }

    public LocalDate getReportEndDate() {
        return reportEndDate;
    }

    public void setReportEndDate(LocalDate reportEndDate) {
        this.reportEndDate = reportEndDate;
    }

    public LocalDate getReportGeneratedDate() {
        return reportGeneratedDate;
    }

    public void setReportGeneratedDate(LocalDate reportGeneratedDate) {
        this.reportGeneratedDate = reportGeneratedDate;
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

    public int getActiveWorkflows() {
        return activeWorkflows;
    }

    public void setActiveWorkflows(int activeWorkflows) {
        this.activeWorkflows = activeWorkflows;
    }

    public int getEscalatedWorkflows() {
        return escalatedWorkflows;
    }

    public void setEscalatedWorkflows(int escalatedWorkflows) {
        this.escalatedWorkflows = escalatedWorkflows;
    }

    public int getCancelledWorkflows() {
        return cancelledWorkflows;
    }

    public void setCancelledWorkflows(int cancelledWorkflows) {
        this.cancelledWorkflows = cancelledWorkflows;
    }

    public Duration getAverageCompletionTime() {
        return averageCompletionTime;
    }

    public void setAverageCompletionTime(Duration averageCompletionTime) {
        this.averageCompletionTime = averageCompletionTime;
    }

    public Duration getMedianCompletionTime() {
        return medianCompletionTime;
    }

    public void setMedianCompletionTime(Duration medianCompletionTime) {
        this.medianCompletionTime = medianCompletionTime;
    }

    public Duration getMaxCompletionTime() {
        return maxCompletionTime;
    }

    public void setMaxCompletionTime(Duration maxCompletionTime) {
        this.maxCompletionTime = maxCompletionTime;
    }

    public Duration getMinCompletionTime() {
        return minCompletionTime;
    }

    public void setMinCompletionTime(Duration minCompletionTime) {
        this.minCompletionTime = minCompletionTime;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public double getEscalationRate() {
        return escalationRate;
    }

    public void setEscalationRate(double escalationRate) {
        this.escalationRate = escalationRate;
    }

    public double getRetryRate() {
        return retryRate;
    }

    public void setRetryRate(double retryRate) {
        this.retryRate = retryRate;
    }

    public Map<TransactionType, Integer> getWorkflowsByTransactionType() {
        return workflowsByTransactionType;
    }

    public void setWorkflowsByTransactionType(Map<TransactionType, Integer> workflowsByTransactionType) {
        this.workflowsByTransactionType = workflowsByTransactionType;
    }

    public Map<TransactionType, Duration> getAverageCompletionTimeByType() {
        return averageCompletionTimeByType;
    }

    public void setAverageCompletionTimeByType(Map<TransactionType, Duration> averageCompletionTimeByType) {
        this.averageCompletionTimeByType = averageCompletionTimeByType;
    }

    public Map<TransactionType, Double> getSuccessRateByType() {
        return successRateByType;
    }

    public void setSuccessRateByType(Map<TransactionType, Double> successRateByType) {
        this.successRateByType = successRateByType;
    }

    public Map<CompensationStatus, Integer> getWorkflowsByStatus() {
        return workflowsByStatus;
    }

    public void setWorkflowsByStatus(Map<CompensationStatus, Integer> workflowsByStatus) {
        this.workflowsByStatus = workflowsByStatus;
    }

    public List<CompensationWorkflowSummary> getWorkflowSummaries() {
        return workflowSummaries;
    }

    public void setWorkflowSummaries(List<CompensationWorkflowSummary> workflowSummaries) {
        this.workflowSummaries = workflowSummaries;
    }

    public List<DailyWorkflowMetrics> getDailyMetrics() {
        return dailyMetrics;
    }

    public void setDailyMetrics(List<DailyWorkflowMetrics> dailyMetrics) {
        this.dailyMetrics = dailyMetrics;
    }

    public Map<String, Integer> getTopFailureReasons() {
        return topFailureReasons;
    }

    public void setTopFailureReasons(Map<String, Integer> topFailureReasons) {
        this.topFailureReasons = topFailureReasons;
    }

    @Override
    public String toString() {
        return "CompensationWorkflowReport{" + "reportStartDate=" + reportStartDate + ", reportEndDate=" + reportEndDate
                + ", totalWorkflows=" + totalWorkflows + ", completedWorkflows=" + completedWorkflows
                + ", failedWorkflows=" + failedWorkflows + ", activeWorkflows=" + activeWorkflows + ", successRate="
                + successRate + '}';
    }
}