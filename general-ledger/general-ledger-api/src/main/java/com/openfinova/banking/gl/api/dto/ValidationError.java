package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing a validation error in batch transaction processing.
 * Used to report which transactions failed validation and why.
 */
@Schema(description = "Validation error for a specific transaction in a batch")
public class ValidationError {

    @Schema(description = "Index/position of the transaction in the batch", example = "42")
    private int index;

    @Schema(description = "Reference ID of the transaction that failed validation", example = "TXN-2026-001")
    private String referenceId;

    @Schema(description = "Error message describing the validation failure", example = "Transaction is not balanced: debits=1000.00, credits=900.00")
    private String errorMessage;

    @Schema(description = "Error code for programmatic handling", example = "UNBALANCED_TRANSACTION")
    private String errorCode;

    // Constructors
    public ValidationError() {
    }

    public ValidationError(int index, String referenceId, String errorMessage) {
        this.index = index;
        this.referenceId = referenceId;
        this.errorMessage = errorMessage;
    }

    public ValidationError(int index, String referenceId, String errorMessage, String errorCode) {
        this.index = index;
        this.referenceId = referenceId;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    // Getters and Setters
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return "ValidationError{" + "index=" + index + ", referenceId='" + referenceId + '\'' + ", errorMessage='"
                + errorMessage + '\'' + ", errorCode='" + errorCode + '\'' + '}';
    }
}
