package com.openfinova.banking.loan.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of validating guarantor eligibility.
 */
public class GuarantorValidationResult {
    private boolean eligible;
    private String message;
    private List<String> reasons = new ArrayList<>();

    public GuarantorValidationResult(boolean eligible, String message) {
        this.eligible = eligible;
        this.message = message;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}
