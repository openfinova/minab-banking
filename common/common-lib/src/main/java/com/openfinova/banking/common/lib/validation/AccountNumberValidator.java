package com.openfinova.banking.common.lib.validation;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for account numbers using a configurable regex pattern.
 *
 * <p>When built by Spring, the pattern comes from {@code account.number.regex}
 * (default {@code ^[A-Z0-9]{8,20}$}).
 *
 * <p>Hibernate Validator may also instantiate this class with a no-arg
 * constructor during JPA pre-persist / flush validation. That path must not
 * rely on Spring; it uses the same default regex as the property default.
 */
@Component
public class AccountNumberValidator implements ConstraintValidator<ValidAccountNumber, String> {

    private static final String DEFAULT_ACCOUNT_NUMBER_REGEX = "^[A-Z0-9]{8,20}$";

    private final String accountNumberPattern;
    private Pattern pattern;

    /** Used by Hibernate Validator when it does not use Spring's {@code ConstraintValidatorFactory}. */
    public AccountNumberValidator() {
        this.accountNumberPattern = DEFAULT_ACCOUNT_NUMBER_REGEX;
    }

    @Autowired
    public AccountNumberValidator(@Value("${account.number.regex:^[A-Z0-9]{8,20}$}") String accountNumberPattern) {
        this.accountNumberPattern = accountNumberPattern;
    }

    @Override
    public void initialize(ValidAccountNumber annotation) {
        this.pattern = Pattern.compile(accountNumberPattern);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull/@NotBlank
        if (value == null || value.isEmpty()) {
            return true;
        }

        return pattern.matcher(value).matches();
    }
}
