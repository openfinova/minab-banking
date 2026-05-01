package com.openfinova.banking.loan.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for rejecting an early settlement quote.
 */
public class EarlySettlementRejectionRequest {

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;

    private String rejectedBy;

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }
}
