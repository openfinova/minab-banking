package com.openfinova.banking.tp.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for batch transaction processing results.
 * Contains the results of processing multiple transactions in a batch.
 */
public class BatchTransactionResult {

    private String batchReference;
    private LocalDateTime processingStartTime;
    private LocalDateTime processingEndTime;
    private int totalRequests;
    private int successfulTransactions;
    private int failedTransactions;
    private List<TransactionResponse> successfulResults;
    private Map<UUID, String> failedResults; // Transaction ID -> Error message
    private long processingDurationMs;

    // Constructors
    public BatchTransactionResult() {
    }

    public BatchTransactionResult(String batchReference, int totalRequests, LocalDateTime processingStartTime) {
        this.batchReference = batchReference;
        this.totalRequests = totalRequests;
        this.processingStartTime = processingStartTime;
    }

    // Helper methods
    public void markProcessingComplete(LocalDateTime processingEndTime) {
        this.processingEndTime = processingEndTime;
        if (processingStartTime != null) {
            this.processingDurationMs = java.time.Duration.between(processingStartTime, processingEndTime).toMillis();
        }
    }

    public double getSuccessRate() {
        if (totalRequests == 0)
            return 0.0;
        return (double) successfulTransactions / totalRequests * 100.0;
    }

    public boolean isFullySuccessful() {
        return failedTransactions == 0 && successfulTransactions == totalRequests;
    }

    public boolean hasFailures() {
        return failedTransactions > 0;
    }

    // Getters and Setters
    public String getBatchReference() {
        return batchReference;
    }

    public void setBatchReference(String batchReference) {
        this.batchReference = batchReference;
    }

    public LocalDateTime getProcessingStartTime() {
        return processingStartTime;
    }

    public void setProcessingStartTime(LocalDateTime processingStartTime) {
        this.processingStartTime = processingStartTime;
    }

    public LocalDateTime getProcessingEndTime() {
        return processingEndTime;
    }

    public void setProcessingEndTime(LocalDateTime processingEndTime) {
        this.processingEndTime = processingEndTime;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public int getSuccessfulTransactions() {
        return successfulTransactions;
    }

    public void setSuccessfulTransactions(int successfulTransactions) {
        this.successfulTransactions = successfulTransactions;
    }

    public int getFailedTransactions() {
        return failedTransactions;
    }

    public void setFailedTransactions(int failedTransactions) {
        this.failedTransactions = failedTransactions;
    }

    public List<TransactionResponse> getSuccessfulResults() {
        return successfulResults;
    }

    public void setSuccessfulResults(List<TransactionResponse> successfulResults) {
        this.successfulResults = successfulResults;
    }

    public Map<UUID, String> getFailedResults() {
        return failedResults;
    }

    public void setFailedResults(Map<UUID, String> failedResults) {
        this.failedResults = failedResults;
    }

    public long getProcessingDurationMs() {
        return processingDurationMs;
    }

    public void setProcessingDurationMs(long processingDurationMs) {
        this.processingDurationMs = processingDurationMs;
    }

    @Override
    public String toString() {
        return "BatchTransactionResult{" + "batchReference='" + batchReference + '\'' + ", totalRequests="
                + totalRequests + ", successfulTransactions=" + successfulTransactions + ", failedTransactions="
                + failedTransactions + ", successRate=" + String.format("%.2f", getSuccessRate()) + "%"
                + ", processingDurationMs=" + processingDurationMs + '}';
    }
}