package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.LoanStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating loan account status.
 */
public class LoanAccountStatusUpdateRequest {

    @NotNull(message = "New status is required")
    private LoanStatus newStatus;

    @NotBlank(message = "Reason is required")
    private String reason;

    public LoanStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(LoanStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
