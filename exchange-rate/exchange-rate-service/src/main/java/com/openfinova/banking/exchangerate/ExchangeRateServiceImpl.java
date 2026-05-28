package com.openfinova.banking.exchangerate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.exchangerate.api.ExchangeRateService;
import com.openfinova.banking.exchangerate.api.dto.CurrencyConversionRequest;
import com.openfinova.banking.exchangerate.api.dto.CurrencyConversionResponse;
import com.openfinova.banking.exchangerate.api.dto.ExchangeRateRequest;
import com.openfinova.banking.exchangerate.api.dto.ExchangeRateResponse;
import com.openfinova.banking.exchangerate.api.entity.RateType;
import com.openfinova.banking.exchangerate.service.ExchangeRateManagementService;

/**
 * Facade implementation for exchange rate operations consumed by other modules.
 * Delegates to {@link ExchangeRateManagementService} where authoritative security checks apply.
 */
@Service
@Transactional
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateManagementService exchangeRateManagementService;

    public ExchangeRateServiceImpl(ExchangeRateManagementService exchangeRateManagementService) {
        this.exchangeRateManagementService = exchangeRateManagementService;
    }

    @Override
    public BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency) {
        return exchangeRateManagementService.getExchangeRate(sourceCurrency, targetCurrency);
    }

    @Override
    public BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency, RateType rateType) {
        return exchangeRateManagementService.getExchangeRate(sourceCurrency, targetCurrency, rateType);
    }

    @Override
    public BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency, LocalDate date) {
        return exchangeRateManagementService.getExchangeRate(sourceCurrency, targetCurrency, date);
    }

    @Override
    public BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency, LocalDate date, RateType rateType) {
        return exchangeRateManagementService.getExchangeRate(sourceCurrency, targetCurrency, date, rateType);
    }

    @Override
    public ExchangeRateResponse getExchangeRateDetails(String sourceCurrency, String targetCurrency, LocalDate date,
            RateType rateType) {
        return exchangeRateManagementService.getExchangeRateDetails(sourceCurrency, targetCurrency, date, rateType);
    }

    @Override
    public ExchangeRateResponse getLatestExchangeRateDetails(String sourceCurrency, String targetCurrency,
            RateType rateType) {
        return exchangeRateManagementService.getLatestExchangeRateDetails(sourceCurrency, targetCurrency, rateType);
    }

    @Override
    public BigDecimal convertCurrency(BigDecimal amount, String sourceCurrency, String targetCurrency) {
        return exchangeRateManagementService.convertCurrency(amount, sourceCurrency, targetCurrency);
    }

    @Override
    public BigDecimal convertCurrency(BigDecimal amount, String sourceCurrency, String targetCurrency,
            LocalDate rateDate, RateType rateType) {
        return exchangeRateManagementService
                .convertCurrency(amount, sourceCurrency, targetCurrency, rateDate, rateType);
    }

    @Override
    public CurrencyConversionResponse convertCurrency(CurrencyConversionRequest request) {
        return exchangeRateManagementService.convertCurrency(request);
    }

    @Override
    public List<BigDecimal> convertToCommonCurrency(List<CurrencyAmount> amounts, String targetCurrency,
            LocalDate rateDate, RateType rateType) {
        return exchangeRateManagementService.convertToCommonCurrency(amounts, targetCurrency, rateDate, rateType);
    }

    @Override
    public ExchangeRateResponse createExchangeRate(ExchangeRateRequest request) {
        return exchangeRateManagementService.createExchangeRate(request);
    }

    @Override
    public ExchangeRateResponse updateExchangeRateById(UUID id, ExchangeRateRequest request, String updatedBy) {
        return exchangeRateManagementService.updateExchangeRateById(id, request);
    }

    @Override
    public void deleteExchangeRate(UUID id, String deletedBy) {
        exchangeRateManagementService.deleteExchangeRate(id);
    }

    @Override
    public List<ExchangeRateResponse> getHistoricalRates(String sourceCurrency, String targetCurrency,
            LocalDate startDate, LocalDate endDate, RateType rateType) {
        return exchangeRateManagementService
                .getHistoricalRates(sourceCurrency, targetCurrency, startDate, endDate, rateType);
    }

    @Override
    public BigDecimal getAverageRate(String sourceCurrency, String targetCurrency, LocalDate startDate,
            LocalDate endDate) {
        return exchangeRateManagementService.getAverageRate(sourceCurrency, targetCurrency, startDate, endDate);
    }

    @Override
    public BigDecimal getAverageRate(String sourceCurrency, String targetCurrency, LocalDate startDate,
            LocalDate endDate, RateType rateType) {
        return exchangeRateManagementService
                .getAverageRate(sourceCurrency, targetCurrency, startDate, endDate, rateType);
    }

    @Override
    public boolean exchangeRateExists(String sourceCurrency, String targetCurrency, LocalDate date, RateType rateType) {
        return exchangeRateManagementService.exchangeRateExists(sourceCurrency, targetCurrency, date, rateType);
    }

    @Override
    public BigDecimal calculateMultiCurrencyTotal(List<CurrencyAmount> amounts, String targetCurrency,
            LocalDate rateDate, RateType rateType) {
        return exchangeRateManagementService.calculateMultiCurrencyTotal(amounts, targetCurrency, rateDate, rateType);
    }

    @Override
    public void validateMultiCurrencyTransaction(List<CurrencyAmount> amounts) {
        exchangeRateManagementService.validateMultiCurrencyTransaction(amounts);
    }

    @Override
    public List<String> getSupportedCurrencies() {
        return exchangeRateManagementService.getSupportedCurrencies();
    }

    @Override
    public void validateCurrencyCode(String currencyCode) {
        exchangeRateManagementService.validateCurrencyCode(currencyCode);
    }

    @Override
    public boolean isCurrencySupported(String currencyCode) {
        return exchangeRateManagementService.isCurrencySupported(currencyCode);
    }

    @Override
    public BigDecimal calculateFXFee(BigDecimal amount, String fromCurrency, String toCurrency, double tierMultiplier) {
        return exchangeRateManagementService.calculateFXFee(amount, fromCurrency, toCurrency, tierMultiplier);
    }
}
