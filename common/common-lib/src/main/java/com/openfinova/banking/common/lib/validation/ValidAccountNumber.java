package com.openfinova.banking.common.lib.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates account number format based on configurable regex pattern.
 * The pattern is loaded from application properties.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AccountNumberValidator.class)
@Documented
public @interface ValidAccountNumber {

    String message() default "Invalid account number format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
