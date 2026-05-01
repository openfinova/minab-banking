package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.CollateralStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating collateral status.
 */
public class CollateralStatusUpdateRequest {

    @NotNull(message = "New status is required")
    private CollateralStatus newStatus;

    public CollateralStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(CollateralStatus newStatus) {
        this.newStatus = newStatus;
    }
}
