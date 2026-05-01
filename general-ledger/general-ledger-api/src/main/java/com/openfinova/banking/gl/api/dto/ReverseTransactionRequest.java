package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to reverse a GL transaction")
public class ReverseTransactionRequest {

    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    @Schema(description = "Reason for reversal", example = "Posting error")
    private String reason;

    @NotBlank(message = "Reversed by is required")
    @Size(max = 100, message = "Reversed by must not exceed 100 characters")
    @Schema(description = "User reversing the transaction", example = "admin")
    private String reversedBy;

    // Constructors
    public ReverseTransactionRequest() {
    }

    public ReverseTransactionRequest(String reason, String reversedBy) {
        this.reason = reason;
        this.reversedBy = reversedBy;
    }

    // Getters and Setters
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReversedBy() {
        return reversedBy;
    }

    public void setReversedBy(String reversedBy) {
        this.reversedBy = reversedBy;
    }
}
