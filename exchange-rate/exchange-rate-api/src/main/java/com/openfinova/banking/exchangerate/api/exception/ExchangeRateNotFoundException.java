package com.openfinova.banking.exchangerate.api.exception;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;

/**
 * Exception thrown when an exchange rate is not found for a given currency pair and date.
 */
public class ExchangeRateNotFoundException extends ResourceNotFoundException {

    private final String sourceCurrency;
    private final String targetCurrency;
    private final String rateDate;

    public ExchangeRateNotFoundException(String sourceCurrency, String targetCurrency, String rateDate) {
        super("ExchangeRate", sourceCurrency + "-" + targetCurrency + " on " + rateDate,
                String.format("Exchange rate not found for %s to %s on %s", sourceCurrency, targetCurrency, rateDate));
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
        this.rateDate = rateDate;
    }

    public ExchangeRateNotFoundException(String sourceCurrency, String targetCurrency) {
        super("ExchangeRate", sourceCurrency + "-" + targetCurrency,
                String.format("Latest exchange rate not found for %s to %s", sourceCurrency, targetCurrency));
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
        this.rateDate = null;
    }

    public ExchangeRateNotFoundException(String message) {
        super(message);
        this.sourceCurrency = null;
        this.targetCurrency = null;
        this.rateDate = null;
    }

    public ExchangeRateNotFoundException(String message, Throwable cause) {
        super(message);
        initCause(cause);
        this.sourceCurrency = null;
        this.targetCurrency = null;
        this.rateDate = null;
    }

    public String getSourceCurrency() {
        return sourceCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public String getRateDate() {
        return rateDate;
    }
}