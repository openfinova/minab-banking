package com.openfinova.banking.tp.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for batch transaction processing requests.
 * Contains multiple transaction requests to be processed together for improved performance.
 */
public class BatchTransactionRequest {

    @NotNull(message = "Transaction requests list cannot be null")
    @NotEmpty(message = "Transaction requests list cannot be empty")
    @Size(max = 100, message = "Batch size cannot exceed 100 transactions")
    @Valid
    private List<TransactionRequestDTO> transactionRequests;

    @Size(max = 255, message = "Batch reference must not exceed 255 characters")
    private String batchReference;

    private boolean failOnFirstError = true;

    private int maxConcurrency = 5;

    // Constructors
    public BatchTransactionRequest() {
    }

    public BatchTransactionRequest(List<TransactionRequestDTO> transactionRequests) {
        this.transactionRequests = transactionRequests;
    }

    public BatchTransactionRequest(List<TransactionRequestDTO> transactionRequests, String batchReference) {
        this.transactionRequests = transactionRequests;
        this.batchReference = batchReference;
    }

    // Getters and Setters
    public List<TransactionRequestDTO> getTransactionRequests() {
        return transactionRequests;
    }

    public void setTransactionRequests(List<TransactionRequestDTO> transactionRequests) {
        this.transactionRequests = transactionRequests;
    }

    public String getBatchReference() {
        return batchReference;
    }

    public void setBatchReference(String batchReference) {
        this.batchReference = batchReference;
    }

    public boolean isFailOnFirstError() {
        return failOnFirstError;
    }

    public void setFailOnFirstError(boolean failOnFirstError) {
        this.failOnFirstError = failOnFirstError;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    @Override
    public String toString() {
        return "BatchTransactionRequest{" + "transactionRequestsCount="
                + (transactionRequests != null ? transactionRequests.size() : 0) + ", batchReference='" + batchReference
                + '\'' + ", failOnFirstError=" + failOnFirstError + ", maxConcurrency=" + maxConcurrency + '}';
    }
}