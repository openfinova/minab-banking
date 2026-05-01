package com.openfinova.banking.tp.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to create fee waiver")
public class CreateFeeWaiverRequest {

    @NotNull(message = "Customer ID is required")
    @Schema(description = "Customer ID", required = true)
    private UUID customerId;

    @Schema(description = "Waiver reason")
    private String reason;

    // Getters and setters
    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
