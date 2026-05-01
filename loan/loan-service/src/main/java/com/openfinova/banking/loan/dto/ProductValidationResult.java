package com.openfinova.banking.loan.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of validating loan parameters against a product definition.
 */
public class ProductValidationResult {
    private boolean valid;
    private String message;
    private List<String> errors = new ArrayList<>();

    public static ProductValidationResult success() {
        ProductValidationResult r = new ProductValidationResult();
        r.valid = true;
        r.message = "Valid";
        return r;
    }

    public static ProductValidationResult failure(String message) {
        ProductValidationResult r = new ProductValidationResult();
        r.valid = false;
        r.message = message;
        r.errors.add(message);
        return r;
    }

    public static ProductValidationResult failure(List<String> errors) {
        ProductValidationResult r = new ProductValidationResult();
        r.valid = false;
        r.errors = new ArrayList<>(errors);
        r.message = errors.isEmpty() ? "Invalid" : String.join("; ", errors);
        return r;
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
