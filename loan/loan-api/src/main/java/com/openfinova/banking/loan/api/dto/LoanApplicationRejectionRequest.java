package com.openfinova.banking.loan.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for rejecting a loan application.
 */
public class LoanApplicationRejectionRequest {

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
