package com.openfinova.banking.loan.api.dto;

/**
 * Request DTO for approving a restructuring request.
 */
public class LoanRestructuringApprovalRequest {

    private String approvedBy;

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
}
