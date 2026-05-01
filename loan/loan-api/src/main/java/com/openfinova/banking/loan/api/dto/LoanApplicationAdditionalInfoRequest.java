package com.openfinova.banking.loan.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for requesting additional information.
 */
public class LoanApplicationAdditionalInfoRequest {

    @NotBlank(message = "Information required is required")
    private String informationRequired;

    public String getInformationRequired() {
        return informationRequired;
    }

    public void setInformationRequired(String informationRequired) {
        this.informationRequired = informationRequired;
    }
}
