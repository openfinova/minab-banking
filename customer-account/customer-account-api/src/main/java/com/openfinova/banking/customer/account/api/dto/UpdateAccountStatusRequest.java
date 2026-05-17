package com.openfinova.banking.customer.account.api.dto;

import com.openfinova.banking.customer.account.api.entity.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update account status")
public class UpdateAccountStatusRequest {

    @NotNull(message = "New status is required")
    @Schema(description = "New account status", required = true)
    private AccountStatus newStatus;

    @NotBlank(message = "Reason is required")
    @Schema(description = "Reason for status change", required = true)
    private String reason;


    // Getters and setters
    public AccountStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(AccountStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
