package com.openfinova.banking.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Confirms MFA disable with the current account password")
public class MfaDisableRequest {

    @NotBlank
    private String currentPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String v) {
        this.currentPassword = v;
    }
}
