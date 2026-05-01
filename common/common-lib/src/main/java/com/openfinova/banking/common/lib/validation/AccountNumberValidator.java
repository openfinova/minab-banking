package com.openfinova.banking.common.lib.validation;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for account numbers using a configurable regex pattern.
 *
 * The pattern is loaded from application properties
 * ({@code account.number.regex}, default {@code ^[A-Z0-9]{8,20}$}).
 *
 * Constructor injection is used intentionally: Spring's
 * {@link org.springframework.validation.beanvalidation.LocalValidatorFactoryBean}
 * creates {@code ConstraintValidator} instances through the Spring
 * {@code ConstraintValidatorFactory}, so this bean is fully constructed
 * (including property injection) before {@link #initialize} is called.
 * Field-level {@code @Value} injection cannot provide the same guarantee
 * — the value may still be {@code null} when {@code initialize()} runs
 * under certain context bootstrap orderings, causing an NPE in
 * {@link Pattern#compile}.
 */
@Component
public class AccountNumberValidator implements ConstraintValidator<ValidAccountNumber, String> {

    private final String accountNumberPattern;
    private Pattern pattern;

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
