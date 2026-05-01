package com.openfinova.banking.tp.api.dto;

import java.util.Map;
import java.util.UUID;

/**
 * DTO for fee calculation requests with dynamic parameters.
 */
public class FeeCalculationRequest {

    private UUID transactionId;
    private TransactionResponse transaction;
    private Map<String, Object> parameters;
    private boolean includeDetailedBreakdown;

    // Constructors
    public FeeCalculationRequest() {
    }

    public FeeCalculationRequest(TransactionResponse transaction) {
        this.transaction = transaction;
        this.transactionId = transaction != null ? transaction.getId() : null;
    }

    public FeeCalculationRequest(UUID transactionId, Map<String, Object> parameters) {
        this.transactionId = transactionId;
        this.parameters = parameters;
    }

    // Getters and setters
    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public TransactionResponse getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionResponse transaction) {
        this.transaction = transaction;
        if (transaction != null && this.transactionId == null) {
            this.transactionId = transaction.getId();
        }
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public boolean isIncludeDetailedBreakdown() {
        return includeDetailedBreakdown;
    }

    public void setIncludeDetailedBreakdown(boolean includeDetailedBreakdown) {
        this.includeDetailedBreakdown = includeDetailedBreakdown;
    }

    @Override
    public String toString() {
        return "FeeCalculationRequest{" + "transactionId=" + transactionId + ", parameters=" + parameters
                + ", includeDetailedBreakdown=" + includeDetailedBreakdown + '}';
    }
}