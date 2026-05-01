package com.openfinova.banking.tp.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Fee waiver response")
public class FeeWaiverResponse {

    @Schema(description = "Waiver ID")
    private UUID id;

    @Schema(description = "Customer ID")
    private UUID customerId;

    @Schema(description = "Waiver reason")
    private String reason;

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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
