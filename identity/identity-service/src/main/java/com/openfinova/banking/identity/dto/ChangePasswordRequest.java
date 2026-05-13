package com.openfinova.banking.identity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Self-service password change")
public class ChangePasswordRequest {

    @NotBlank
    @JsonAlias({ "current_password", "password", "oldPassword", "old_password" })
    private String currentPassword;

    @NotBlank
    @Size(min = 12, max = 128)
    @JsonAlias("new_password")
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String v) {
        this.currentPassword = v;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String v) {
        this.newPassword = v;
    }
}
