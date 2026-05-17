package com.openfinova.banking.exchangerate.provider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * Port for external exchange-rate data sources (ECB, exchangerate-api.com, internal trading desk, ...).
 *
 * <p>Implementations fetch the latest published mid rates from the given base currency to each requested
 * target currency. Targets the implementation does not publish should simply be omitted from the result;
 * the caller will log and skip them rather than treating it as a failure.
 */
public interface ExchangeRateProvider {

    /** Identifier matched against {@code app.exchange-rate.provider}. */
    String getId();

    /**
     * Fetch the latest published rates.
     *
     * @param baseCurrency ISO 4217 base currency the rates should be quoted against.
     * @param targetCurrencies set of ISO 4217 target currencies to fetch.
     * @return the rates response.
     * @throws ExchangeRateProviderException if the upstream call fails or the response is malformed.
     */
    ProviderRates fetchLatestRates(String baseCurrency, Set<String> targetCurrencies);

    /**
     * Provider response: the publication date and a map of target currency code to rate
     * (units of target per 1 unit of base).
     */
    record ProviderRates(String baseCurrency, LocalDate publicationDate, Map<String, BigDecimal> rates) {
    }
}
