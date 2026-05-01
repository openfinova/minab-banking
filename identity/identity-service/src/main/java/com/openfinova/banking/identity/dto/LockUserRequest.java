package com.openfinova.banking.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Reason recorded when administratively locking an account")
public class LockUserRequest {

    @NotBlank
    @Size(max = 255)
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String v) {
        this.reason = v;
    }
}
