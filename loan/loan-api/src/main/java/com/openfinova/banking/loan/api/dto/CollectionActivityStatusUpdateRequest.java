package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.CollectionStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating collection activity status.
 */
public class CollectionActivityStatusUpdateRequest {

    @NotNull(message = "New status is required")
    private CollectionStatus newStatus;

    private String updatedBy;

    public CollectionStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(CollectionStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
