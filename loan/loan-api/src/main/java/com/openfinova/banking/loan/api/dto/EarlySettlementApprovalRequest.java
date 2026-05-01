package com.openfinova.banking.loan.api.dto;

/**
 * Request DTO for approving an early settlement quote.
 */
public class EarlySettlementApprovalRequest {

    private String approvedBy;

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
}
