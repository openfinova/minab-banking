package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for bulk transaction posting.
 * Allows posting multiple transactions in a single operation with a correlation
 * ID for traceability.
 */
@Schema(description = "Request for posting multiple transactions in bulk")
public class BulkPostTransactionRequest {

    @Schema(description = "List of transactions to post", required = true)
    private List<PostTransactionCommand> transactions;

    @Schema(description = "Correlation ID for tracking related transactions across the batch", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID correlationId;

    @Schema(description = "Whether to validate all transactions before posting any (fail-fast)", example = "true", defaultValue = "true")
    private boolean validateFirst = true;

    // Constructors
    public BulkPostTransactionRequest() {
    }

    public BulkPostTransactionRequest(List<PostTransactionCommand> transactions, UUID correlationId) {
        this.transactions = transactions;
        this.correlationId = correlationId;
    }

    public BulkPostTransactionRequest(List<PostTransactionCommand> transactions, UUID correlationId,
            boolean validateFirst) {
        this.transactions = transactions;
        this.correlationId = correlationId;
        this.validateFirst = validateFirst;
    }

    // Getters and Setters
    public List<PostTransactionCommand> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<PostTransactionCommand> transactions) {
        this.transactions = transactions;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public boolean isValidateFirst() {
        return validateFirst;
    }

    public void setValidateFirst(boolean validateFirst) {
        this.validateFirst = validateFirst;
    }

    @Override
    public String toString() {
        return "BulkPostTransactionRequest{" + "transactions="
                + (transactions != null ? transactions.size() + " items" : "null") + ", correlationId=" + correlationId
                + ", validateFirst=" + validateFirst + '}';
    }
}
