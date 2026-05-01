package com.openfinova.banking.customer.api.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing customer validation results.
 */
public class CustomerValidationResult {

    private UUID customerId;
    private boolean isValid;
    private List<String> validationErrors;
    private String customerStatus;
    private String kycStatus;

    public CustomerValidationResult() {
        this.validationErrors = new ArrayList<>();
    }

    public CustomerValidationResult(UUID customerId, boolean isValid) {
        this.customerId = customerId;
        this.isValid = isValid;
        this.validationErrors = new ArrayList<>();
    }

    /**
     * Creates a successful validation result.
     */
    public static CustomerValidationResult success(UUID customerId, String customerStatus, String kycStatus) {
        CustomerValidationResult result = new CustomerValidationResult(customerId, true);
        result.setCustomerStatus(customerStatus);
        result.setKycStatus(kycStatus);
        return result;
    }

    /**
     * Creates a failed validation result with errors.
     */
    public static CustomerValidationResult failure(UUID customerId, List<String> errors) {
        CustomerValidationResult result = new CustomerValidationResult(customerId, false);
        result.setValidationErrors(errors);
        return result;
    }

    /**
     * Adds a validation error.
     */
    public void addError(String error) {
        this.validationErrors.add(error);
        this.isValid = false;
    }

    // Getters and Setters
    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public String getCustomerStatus() {
        return customerStatus;
    }

    public void setCustomerStatus(String customerStatus) {
        this.customerStatus = customerStatus;
    }

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }
}