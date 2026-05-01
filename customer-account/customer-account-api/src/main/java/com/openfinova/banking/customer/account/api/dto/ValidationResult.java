package com.openfinova.banking.customer.account.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A unified validation container for the account module.
 * Can be used for transactional pre-checks, beneficiary validation,
 * and account status verification.
 */
public class ValidationResult {
    private boolean valid;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private BigDecimal availableBalance;
    private String accountStatus;
    private List<String> applicableLimits = new ArrayList<>();
    private String message;

    public ValidationResult() {
    }

    public ValidationResult(boolean valid) {
        this.valid = valid;
    }

    public ValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
        if (!valid && message != null) {
            this.errors.add(message);
        }
    }

    public static ValidationResult success() {
        return new ValidationResult(true);
    }

    public static ValidationResult valid() {
        return new ValidationResult(true);
    }

    public static ValidationResult invalid(String error) {
        ValidationResult result = new ValidationResult(false);
        result.addError(error);
        return result;
    }

    public static ValidationResult failure(String error) {
        ValidationResult result = new ValidationResult(false);
        result.addError(error);
        return result;
    }

    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public String getErrorMessage() {
        if (errors.isEmpty()) {
            return message;
        }
        return String.join(", ", errors);
    }

    public String getWarningMessage() {
        if (warnings.isEmpty()) {
            return message;
        }
        return String.join(", ", warnings);
    }

    // Getters and Setters
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public List<String> getApplicableLimits() {
        return applicableLimits;
    }

    public void setApplicableLimits(List<String> applicableLimits) {
        this.applicableLimits = applicableLimits;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
