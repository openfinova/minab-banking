package com.openfinova.banking.loan.api.dto;

/**
 * Request DTO for processing a restructuring request.
 */
public class LoanRestructuringProcessRequest {

    private String processedBy;

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }
}
