package com.openfinova.banking.exchangerate.api.exception;

/**
 * Exception thrown when exchange rate validation fails.
 */
public class ExchangeRateValidationException extends RuntimeException {

    private final String field;
    private final Object value;

    public ExchangeRateValidationException(String field, Object value, String reason) {
        super(String.format("Validation failed for field '%s' with value '%s': %s", field, value, reason));
        this.field = field;
        this.value = value;
    }

    public ExchangeRateValidationException(String message) {
        super(message);
        this.field = null;
        this.value = null;
    }

    public ExchangeRateValidationException(String message, Throwable cause) {
        super(message, cause);
        this.field = null;
        this.value = null;
    }

    public String getField() {
        return field;
    }

    public Object getValue() {
        return value;
    }
}