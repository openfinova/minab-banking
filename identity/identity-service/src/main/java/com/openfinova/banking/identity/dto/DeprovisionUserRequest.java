package com.openfinova.banking.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Optional reason recorded on deprovisioning audit trail")
public class DeprovisionUserRequest {

    @Size(max = 500)
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason != null && !reason.isBlank() ? reason.strip() : null;
    }
}
