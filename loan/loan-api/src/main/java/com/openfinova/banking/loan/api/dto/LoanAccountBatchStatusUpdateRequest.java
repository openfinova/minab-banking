package com.openfinova.banking.loan.api.dto;

import java.util.List;
import java.util.UUID;

import com.openfinova.banking.loan.api.entity.LoanStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for batch updating loan account statuses.
 */
public class LoanAccountBatchStatusUpdateRequest {

    @NotEmpty(message = "Loan account IDs are required")
    private List<UUID> loanAccountIds;

    @NotNull(message = "New status is required")
    private LoanStatus newStatus;

    @NotBlank(message = "Reason is required")
    private String reason;

    public List<UUID> getLoanAccountIds() {
        return loanAccountIds;
    }

    public void setLoanAccountIds(List<UUID> loanAccountIds) {
        this.loanAccountIds = loanAccountIds;
    }

    public LoanStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(LoanStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
