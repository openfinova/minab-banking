package com.openfinova.banking.loan.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for completing a collection activity.
 */
public class CollectionActivityCompleteRequest {

    @NotBlank(message = "Outcome is required")
    private String outcome;

    private String completedBy;

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(String completedBy) {
        this.completedBy = completedBy;
    }
}
