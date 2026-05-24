package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Rejection reason for a pending GL transaction (required)")
public class GlRejectTransactionRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(max = 500)
    @Schema(description = "Why this transaction is rejected", example = "Incorrect account coding")
    private String reason;

    public GlRejectTransactionRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
