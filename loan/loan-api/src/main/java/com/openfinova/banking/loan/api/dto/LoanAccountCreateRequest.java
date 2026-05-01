package com.openfinova.banking.loan.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a loan account from an approved application.
 */
public class LoanAccountCreateRequest {

    @NotNull(message = "Application ID is required")
    private UUID applicationId;

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }
}
