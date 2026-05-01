package com.openfinova.banking.common.lib.validation;

import com.openfinova.banking.common.lib.model.SupportedCurrency;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for ISO 4217 currency codes.
 * Validates that a string is a valid three-letter currency code.
 */
public class CurrencyValidator implements ConstraintValidator<ValidCurrency, String> {

    @Override
    public void initialize(ValidCurrency constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null values are considered valid (use @NotNull for null checks)
        if (value == null) {
            return true;
        }

        // Check if the currency code is valid using the Currency enum
        return SupportedCurrency.isValid(value);
    }
}
