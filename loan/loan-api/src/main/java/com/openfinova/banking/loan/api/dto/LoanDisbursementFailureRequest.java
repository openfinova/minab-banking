package com.openfinova.banking.loan.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for failing a disbursement.
 */
public class LoanDisbursementFailureRequest {

    @NotBlank(message = "Failure reason is required")
    private String failureReason;

    private String failedBy;

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getFailedBy() {
        return failedBy;
    }

    public void setFailedBy(String failedBy) {
        this.failedBy = failedBy;
    }
}
