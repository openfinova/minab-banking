package com.openfinova.banking.loan.api.dto;

/**
 * Request DTO for verifying a guarantor.
 */
public class GuarantorVerificationRequest {

    private String verifiedBy;

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }
}
