package com.openfinova.banking.identity.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Administratively suspend sign-in for a user")
public class SuspendUserRequest {

    @NotBlank
    @Size(max = 500)
    private String reason;

    @Schema(description = "When set, suspension ends at this instant (inclusive of automatic re-enable for auth)")
    private LocalDateTime suspensionUntil;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason != null ? reason.strip() : null;
    }

    public LocalDateTime getSuspensionUntil() {
        return suspensionUntil;
    }

    public void setSuspensionUntil(LocalDateTime suspensionUntil) {
        this.suspensionUntil = suspensionUntil;
    }
}
