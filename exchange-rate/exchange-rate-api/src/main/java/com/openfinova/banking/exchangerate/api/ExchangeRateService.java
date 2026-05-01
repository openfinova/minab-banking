package com.openfinova.banking.exchangerate.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.openfinova.banking.exchangerate.api.dto.CurrencyConversionRequest;
import com.openfinova.banking.exchangerate.api.dto.CurrencyConversionResponse;
import com.openfinova.banking.exchangerate.api.dto.ExchangeRateRequest;
import com.openfinova.banking.exchangerate.api.dto.ExchangeRateResponse;
import com.openfinova.banking.exchangerate.api.entity.RateType;

/**
 * Unified Service interface for currency management and conversions.
 * This interface defines the contract for exchange rate operations used across the system.
 */
public interface ExchangeRateService {

    /**
     * Gets the latest exchange rate between two currencies using SPOT rate type.
     *
     * @param sourceCurrency The source currency code
     * @param targetCurrency The target currency code
     * @return The latest exchange rate
     * @throws ExchangeRateNotFoundException if the exchange rate is not found
     */
    BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency);

    /**
     * Gets the latest exchange rate between two currencies for a specific rate type.
     *
     * @param sourceCurrency The source currency code
     * @param targetCurrency The target currency code
     * @param rateType The rate type to use
     * @return The latest exchange rate
     * @throws ExchangeRateNotFoundException if the exchange rate is not found
     */
    BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency, RateType rateType);

    /**
     * Gets the exchange rate between two currencies for a specific date using SPOT rate type.
     *
     * @param sourceCurrency The source currency code
     * @param targetCurrency The target currency code
     * @param date The date for the exchange rate
     * @return The exchange rate for the specific date
     * @throws ExchangeRateNotFoundException if the exchange rate is not found
     */
    BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency, LocalDate date);

    /**
     * Gets the exchange rate between two currencies for a specific date and rate type.
     *
     * @param sourceCurrency The source currency code
     * @param targetCurrency The target currency code
     * @param date The date for the exchange rate
     * @param rateType The rate type to use
     * @return The exchange rate for the specific date and rate type
     * @throws ExchangeRateNotFoundException if the exchange rate is not found
     */
    BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency, LocalDate date, RateType rateType);

    /**
     * Gets detailed exchange rate information for a specific date and rate type.
     *
     * @param sourceCurrency The source currency code
     * @param targetCurrency The target currency code
     * @param date The date for the exchange rate
     * @param rateType The rate type to use
     * @return The exchange rate details
     * @throws ExchangeRateNotFoundException if the exchange rate is not found
     */
    ExchangeRateResponse getExchangeRateDetails(String sourceCurrency, String targetCurrency, LocalDate date,
            RateType rateType);

    /**
     * Gets detailed latest exchange rate information for a specific rate type.
     *
     * @param sourceCurrency The source currency code
     * @param targetCurrency The target currency code
     * @param rateType The rate type to use
     * @return The latest exchange rate details
     * @throws ExchangeRateNotFoundException if the exchange rate is not found
     */
    ExchangeRateResponse getLatestExchangeRateDetails(String sourceCurrency, String targetCurrency, RateType rateType);

    /**
     * Converts an amount from one currency to another using current SPOT rates.
     *
     * @param amount The amount to convert
     * @param sourceCurrency The source currency code
     * @param targetCurrency The target currency code
     * @return The converted amount
     * @throws ExchangeRateNotFoundException if the exchange rate is not found
     */
    BigDecimal convertCurrency(BigDecimal amount, String sourceCurrency, String targetCurrency);

    /**
     * Converts an amount from one currency to another using rates for a specific date and type.
     *
     * @param amount The amount to convert
     * @param sourceCurrency The source currency code
     * @param targetCurrency The target currency code
     * @param rateDate The date for the exchange rate
     * @param rateType The rate type to use
     * @return The converted amount
     * @throws ExchangeRateNotFoundException if the exchange rate is not found
     */
    BigDecimal convertCurrency(BigDecimal amount, String sourceCurrency, String targetCurrency, LocalDate rateDate,
            RateType rateType);

    /**
     * Converts currency using a detailed request and returns a comprehensive response.
     *
     * @param request The currency conversion request containing all details
     * @return The comprehensive currency conversion response
     * @throws ExchangeRateNotFoundException if the exchange rate is not found
     */
    CurrencyConversionResponse convertCurrency(CurrencyConversionRequest request);

    /**
     * Converts multiple amounts to a common currency for aggregation.
     *
     * @param amounts The list of currency amounts to convert
     * @param targetCurrency The target currency code
     * @param rateDate The date for the exchange rate
     * @param rateType The rate type to use
     * @return The list of converted amounts in the target currency
     * @throws ExchangeRateNotFoundException if the exchange rate is not found
     */
    List<BigDecimal> convertToCommonCurrency(List<CurrencyAmount> amounts, String targetCurrency, LocalDate rateDate,
            RateType rateType);

    /**
     * Creates a new exchange rate entry.
     *
     * @param request The exchange rate request containing the rate details
     * @return The created exchange rate response
     */
    ExchangeRateResponse createExchangeRate(ExchangeRateRequest request);

    /**
     * Updates a specific exchange rate record by its UUID.
     * Allows correcting any field (rate, date, type) of an already-published entry.
     *
     * @param id        UUID of the record to update
     * @param request   the corrected rate data
     * @param updatedBy username of the operator making the correction (required for audit trail)
     * @return the updated exchange rate response
     * @throws com.openfinova.banking.exchangerate.api.exception.ExchangeRateNotFoundException if no record with that ID exists
     */
    ExchangeRateResponse updateExchangeRateById(UUID id, ExchangeRateRequest request, String updatedBy);

    /**
     * Deletes an exchange rate record by its UUID.
     * Use only to retract a mistakenly published rate; prefer correction via
     * {@link #updateExchangeRateById} when the rate itself is wrong but the record is valid.
     *
     * @param id        UUID of the record to delete
     * @param deletedBy username of the operator requesting deletion (required for audit trail)
     * @throws com.openfinova.banking.exchangerate.api.exception.ExchangeRateNotFoundException if no record with that ID exists
     */
    void deleteExchangeRate(UUID id, String deletedBy);

    /**
     * Gets historical exchange rates for a currency pair within a date range.
     *
     * @param sourceCurrency The source currency code
     * @param targetCurrency The target currency code
     * @param startDate The start date of the period (inclusive)
     * @param endDate The end date of the period (inclusive)
     * @param rateType The rate type to use
     * @return The list of historical exchange rates
     */
    List<ExchangeRateResponse> getHistoricalRates(String sourceCurrency, String targetCurrency, LocalDate startDate,
            LocalDate endDate, RateType rateType);

    /**
     * Calculates the average exchange rate for a currency pair over a date range using SPOT rate type.
     * This is typically used for P&L account translation per IAS 21.
     *
     * @param sourceCurrency The source currency code
     * @param targetCurrency The target currency code
     * @param startDate The start date of the period (inclusive)
     * @param endDate The end date of the period (inclusive)
     * @return The average exchange rate over the period
     * @throws ExchangeRateNotFoundException if no rates found in the date range
     */
    BigDecimal getAverageRate(String sourceCurrency, String targetCurrency, LocalDate startDate, LocalDate endDate);

    /**
     * Calculates the average exchange rate for a currency pair over a date range for a specific rate type.
     *
     * @param sourceCurrency The source currency code
     * @param targetCurrency The target currency code
     * @param startDate The start date of the period (inclusive)
     * @param endDate The end date of the period (inclusive)
     * @param rateType The type of exchange rate to use
     * @return The average exchange rate over the period
     * @throws ExchangeRateNotFoundException if no rates found in the date range
     */
    BigDecimal getAverageRate(String sourceCurrency, String targetCurrency, LocalDate startDate, LocalDate endDate,
            RateType rateType);

    /**
     * Checks if an exchange rate exists for the given parameters.
     *
     * @param sourceCurrency The source currency code
     * @param targetCurrency The target currency code
     * @param date The date for the exchange rate
     * @param rateType The rate type to use
     * @return true if the exchange rate exists, false otherwise
     */
    boolean exchangeRateExists(String sourceCurrency, String targetCurrency, LocalDate date, RateType rateType);

    /**
     * Calculates the total of multiple currency amounts in a target currency.
     *
     * @param amounts The list of currency amounts
     * @param targetCurrency The target currency code
     * @param rateDate The date for the exchange rates
     * @param rateType The rate type to use
     * @return The calculated total in the target currency
     * @throws ExchangeRateNotFoundException if any required exchange rate is not found
     */
    BigDecimal calculateMultiCurrencyTotal(List<CurrencyAmount> amounts, String targetCurrency, LocalDate rateDate,
            RateType rateType);

    /**
     * Validates that all monetary fields in a transaction support multi-currency operations.
     *
     * @param amounts The list of currency amounts to validate
     * @throws com.openfinova.banking.exchangerate.api.exception.ExchangeRateValidationException if validation fails
     */
    void validateMultiCurrencyTransaction(List<CurrencyAmount> amounts);

    /**
     * Gets all supported currencies.
     *
     * @return The list of supported currency codes
     */
    List<String> getSupportedCurrencies();

    /**
     * Validates if a currency code is supported.
     *
     * @param currencyCode The currency code to validate
     * @throws com.openfinova.banking.exchangerate.api.exception.ExchangeRateValidationException if the currency is not supported
     */
    void validateCurrencyCode(String currencyCode);

    /**
     * Checks if a currency is supported.
     *
     * @param currencyCode The currency code to check
     * @return true if the currency is supported, false otherwise
     */
    boolean isCurrencySupported(String currencyCode);

    /**
     * Calculates the FX fee for a cross-currency conversion using configurable spreads.
     * Returns zero if both currencies are the same or amount is invalid.
     *
     * @param amount         transaction amount (in any of the two currencies)
     * @param fromCurrency   source currency of the conversion leg
     * @param toCurrency     target currency of the conversion leg
     * @param tierMultiplier multiplier for tier-based discount (1.0 = full fee, 0.5 = 50% discount)
     * @return fee amount (zero or positive), rounded to 4 decimal places
     */
    BigDecimal calculateFXFee(BigDecimal amount, String fromCurrency, String toCurrency, double tierMultiplier);

    /**
     * Data class for currency amount pairs.
     */
    public static class CurrencyAmount {
        private final BigDecimal amount;
        private final String currency;

        public CurrencyAmount(BigDecimal amount, String currency) {
            this.amount = amount;
            this.currency = currency;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getCurrency() {
            return currency;
        }
    }
}
