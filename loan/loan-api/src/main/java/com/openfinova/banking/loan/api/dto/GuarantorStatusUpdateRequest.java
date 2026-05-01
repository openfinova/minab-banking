package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.GuarantorStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating guarantor status.
 */
public class GuarantorStatusUpdateRequest {

    @NotNull(message = "New status is required")
    private GuarantorStatus newStatus;

    private String updatedBy;

    public GuarantorStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(GuarantorStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
