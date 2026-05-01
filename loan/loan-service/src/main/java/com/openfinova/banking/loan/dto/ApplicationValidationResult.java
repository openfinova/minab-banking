package com.openfinova.banking.loan.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of validating a loan application for a workflow step.
 */
public class ApplicationValidationResult {
    private boolean valid;
    private String message;
    private List<String> errors = new ArrayList<>();

    public ApplicationValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
