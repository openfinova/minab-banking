package com.openfinova.banking.loan.api.dto;

/**
 * Request DTO for completing a disbursement.
 */
public class LoanDisbursementCompletionRequest {

    private String completedBy;

    public String getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(String completedBy) {
        this.completedBy = completedBy;
    }
}
