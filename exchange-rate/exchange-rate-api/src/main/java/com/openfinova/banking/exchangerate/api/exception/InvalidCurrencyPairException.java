package com.openfinova.banking.exchangerate.api.exception;

/**
 * Exception thrown when an invalid currency pair is provided for exchange rate operations.
 */
public class InvalidCurrencyPairException extends RuntimeException {

    private final String sourceCurrency;
    private final String targetCurrency;

    public InvalidCurrencyPairException(String sourceCurrency, String targetCurrency, String reason) {
        super(String.format("Invalid currency pair %s to %s: %s", sourceCurrency, targetCurrency, reason));
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
    }

    public InvalidCurrencyPairException(String message) {
        super(message);
        this.sourceCurrency = null;
        this.targetCurrency = null;
    }

    public InvalidCurrencyPairException(String message, Throwable cause) {
        super(message, cause);
        this.sourceCurrency = null;
        this.targetCurrency = null;
    }

    public String getSourceCurrency() {
        return sourceCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }
}