package com.openfinova.banking.customer.account.api.dto;

import com.openfinova.banking.customer.account.api.entity.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request to batch update account status")
public class BatchStatusUpdateRequest {

    @NotEmpty(message = "Account IDs list cannot be empty")
    @Schema(description = "List of account IDs to update", required = true)
    private List<UUID> accountIds;

    @NotNull(message = "New status is required")
    @Schema(description = "New account status", required = true)
    private AccountStatus newStatus;

    @NotBlank(message = "Reason is required")
    @Schema(description = "Reason for status change", required = true)
    private String reason;

    @NotBlank(message = "Changed by is required")
    @Schema(description = "User making the change", required = true)
    private String changedBy;

    // Getters and setters
    public List<UUID> getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(List<UUID> accountIds) {
        this.accountIds = accountIds;
    }

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

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
}
