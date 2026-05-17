package com.openfinova.banking.exchangerate.provider;

/**
 * Raised when an {@link ExchangeRateProvider} cannot deliver rates (network failure, malformed
 * response, unsupported base currency, ...). Callers are expected to log + skip the sync run;
 * existing rates remain queryable via the read-side staleness fallback.
 */
public class ExchangeRateProviderException extends RuntimeException {

    public ExchangeRateProviderException(String message) {
        super(message);
    }

    public ExchangeRateProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
