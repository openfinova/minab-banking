package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for bulk transaction posting.
 * Contains successfully posted transactions and any validation errors.
 */
@Schema(description = "Response for bulk transaction posting operation")
public class BulkPostTransactionResponse {

    @Schema(description = "List of successfully posted transactions")
    private List<GLTransactionDTO> successfulTransactions = new ArrayList<>();

    @Schema(description = "List of validation errors for failed transactions")
    private List<ValidationError> validationErrors = new ArrayList<>();

    @Schema(description = "Total number of transactions submitted", example = "100")
    private int totalSubmitted;

    @Schema(description = "Number of transactions successfully posted", example = "95")
    private int successCount;

    @Schema(description = "Number of transactions that failed validation", example = "5")
    private int failureCount;

    // Constructors
    public BulkPostTransactionResponse() {
    }

    public BulkPostTransactionResponse(int totalSubmitted) {
        this.totalSubmitted = totalSubmitted;
    }

    // Getters and Setters
    public List<GLTransactionDTO> getSuccessfulTransactions() {
        return successfulTransactions;
    }

    public void setSuccessfulTransactions(List<GLTransactionDTO> successfulTransactions) {
        this.successfulTransactions = successfulTransactions;
        this.successCount = successfulTransactions != null ? successfulTransactions.size() : 0;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
        this.failureCount = validationErrors != null ? validationErrors.size() : 0;
    }

    public int getTotalSubmitted() {
        return totalSubmitted;
    }

    public void setTotalSubmitted(int totalSubmitted) {
        this.totalSubmitted = totalSubmitted;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    /**
     * Add a successful transaction to the response.
     */
    public void addSuccessfulTransaction(GLTransactionDTO transaction) {
        this.successfulTransactions.add(transaction);
        this.successCount = this.successfulTransactions.size();
    }

    /**
     * Add a validation error to the response.
     */
    public void addValidationError(ValidationError error) {
        this.validationErrors.add(error);
        this.failureCount = this.validationErrors.size();
    }

    /**
     * Check if all transactions were successful.
     */
    public boolean isFullySuccessful() {
        return failureCount == 0 && successCount == totalSubmitted;
    }

    /**
     * Check if there were any failures.
     */
    public boolean hasErrors() {
        return failureCount > 0;
    }

    @Override
    public String toString() {
        return "BulkPostTransactionResponse{" + "totalSubmitted=" + totalSubmitted + ", successCount=" + successCount
                + ", failureCount=" + failureCount + '}';
    }
}
