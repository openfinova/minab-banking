package com.openfinova.banking.customer.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request to batch close accounts")
public class BatchCloseAccountsRequest {

    @NotEmpty(message = "Account IDs list cannot be empty")
    @Schema(description = "List of account IDs to close", required = true)
    private List<UUID> accountIds;

    @NotBlank(message = "Reason is required")
    @Schema(description = "Reason for closure", required = true)
    private String reason;

    @NotBlank(message = "Closed by is required")
    @Schema(description = "User closing the accounts", required = true)
    private String closedBy;

    // Getters and setters
    public List<UUID> getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(List<UUID> accountIds) {
        this.accountIds = accountIds;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }
}
