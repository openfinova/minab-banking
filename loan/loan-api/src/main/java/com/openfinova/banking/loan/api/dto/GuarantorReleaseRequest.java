package com.openfinova.banking.loan.api.dto;

/**
 * Request DTO for releasing a guarantor.
 */
public class GuarantorReleaseRequest {

    private String releasedBy;

    public String getReleasedBy() {
        return releasedBy;
    }

    public void setReleasedBy(String releasedBy) {
        this.releasedBy = releasedBy;
    }
}
