package com.openfinova.banking.loan.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for assigning a loan application to an underwriter.
 */
public class LoanApplicationAssignUnderwriterRequest {

    @NotBlank(message = "Underwriter ID is required")
    private String underwriterId;

    public String getUnderwriterId() {
        return underwriterId;
    }

    public void setUnderwriterId(String underwriterId) {
        this.underwriterId = underwriterId;
    }
}
