package com.openfinova.banking.exchangerate.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.lib.model.SupportedCurrency;
import com.openfinova.banking.exchangerate.api.ExchangeRateService.CurrencyAmount;
import com.openfinova.banking.exchangerate.api.dto.CurrencyConversionRequest;
import com.openfinova.banking.exchangerate.api.dto.CurrencyConversionResponse;
import com.openfinova.banking.exchangerate.api.dto.ExchangeRateRequest;
import com.openfinova.banking.exchangerate.api.dto.ExchangeRateResponse;
import com.openfinova.banking.exchangerate.api.entity.RateType;
import com.openfinova.banking.exchangerate.api.exception.ExchangeRateNotFoundException;
import com.openfinova.banking.exchangerate.api.exception.ExchangeRateValidationException;
import com.openfinova.banking.exchangerate.api.exception.InvalidCurrencyPairException;
import com.openfinova.banking.exchangerate.config.ExchangeRateProperties;
import com.openfinova.banking.exchangerate.entity.ExchangeRate;
import com.openfinova.banking.exchangerate.entity.FXSpread;
import com.openfinova.banking.exchangerate.repository.ExchangeRateRepository;
import com.openfinova.banking.exchangerate.repository.FXSpreadRepository;
import com.openfinova.banking.setup.api.DateTimeService;

@Service
@Transactional
public class ExchangeRateManagementService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final DateTimeService dateTimeService;
    private final FXSpreadRepository fxSpreadRepository;
    private final ExchangeRateProperties properties;

    @Value("${app.base-currency:EUR}")
    private String baseCurrency;

    /** Default spread when no configuration exists (0.25% = 25 bps). */
    public final BigDecimal DEFAULT_SPREAD = new BigDecimal("0.0025");

    public ExchangeRateManagementService(ExchangeRateRepository exchangeRateRepository, DateTimeService dateTimeService,
            FXSpreadRepository fxSpreadRepository, ExchangeRateProperties properties) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.dateTimeService = dateTimeService;
        this.fxSpreadRepository = fxSpreadRepository;
        this.properties = properties;
    }

    @Cacheable(value = "exchangeRates", key = "#sourceCurrency + '_' + #targetCurrency + '_latest'")
    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency) {
        return getExchangeRate(sourceCurrency, targetCurrency, RateType.SPOT);
    }

    @Cacheable(value = "exchangeRates", key = "#sourceCurrency + '_' + #targetCurrency + '_latest_' + #rateType")
    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency, RateType rateType) {
        validateCurrencyPair(sourceCurrency, targetCurrency);
        return getLatestExchangeRateInternal(sourceCurrency, targetCurrency, rateType);
    }

    @Cacheable(value = "exchangeRates", key = "#sourceCurrency + '_' + #targetCurrency + '_' + #date")
    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency, LocalDate date) {
        return getExchangeRate(sourceCurrency, targetCurrency, date, RateType.SPOT);
    }

    @Cacheable(value = "exchangeRates", key = "#sourceCurrency + '_' + #targetCurrency + '_' + #date + '_' + #rateType")
    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency, LocalDate date, RateType rateType) {
        validateCurrencyPair(sourceCurrency, targetCurrency);
        validateDate(date);
        return getExchangeRateInternal(sourceCurrency, targetCurrency, date, rateType);
    }

    @Cacheable(value = "exchangeRates", key = "#sourceCurrency + '_' + #targetCurrency + '_' + #date + '_' + #rateType + '_details'")
    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public ExchangeRateResponse getExchangeRateDetails(String sourceCurrency, String targetCurrency, LocalDate date,
            RateType rateType) {
        validateCurrencyPair(sourceCurrency, targetCurrency);
        validateDate(date);

        Optional<ExchangeRate> exchangeRate = exchangeRateRepository
                .findBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(
                        sourceCurrency,
                        targetCurrency,
                        date,
                        rateType);

        if (exchangeRate.isPresent()) {
            return mapToResponse(exchangeRate.get());
        }

        throw new ExchangeRateNotFoundException(sourceCurrency, targetCurrency, date.toString());
    }

    @Cacheable(value = "exchangeRates", key = "#sourceCurrency + '_' + #targetCurrency + '_latest_' + #rateType + '_details'")
    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public ExchangeRateResponse getLatestExchangeRateDetails(String sourceCurrency, String targetCurrency,
            RateType rateType) {
        validateCurrencyPair(sourceCurrency, targetCurrency);

        Optional<ExchangeRate> latestRate = exchangeRateRepository
                .findFirstBySourceCurrencyAndTargetCurrencyAndRateTypeOrderByRateDateDesc(
                        sourceCurrency,
                        targetCurrency,
                        rateType);

        if (latestRate.isPresent()) {
            return mapToResponse(latestRate.get());
        }

        throw new ExchangeRateNotFoundException(sourceCurrency, targetCurrency);
    }

    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public BigDecimal convertCurrency(BigDecimal amount, String sourceCurrency, String targetCurrency) {
        return convertCurrency(amount, sourceCurrency, targetCurrency, dateTimeService.today(), RateType.SPOT);
    }

    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public BigDecimal convertCurrency(BigDecimal amount, String sourceCurrency, String targetCurrency,
            LocalDate rateDate, RateType rateType) {
        return convertCurrencyInternal(amount, sourceCurrency, targetCurrency, rateDate, rateType);
    }

    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public CurrencyConversionResponse convertCurrency(CurrencyConversionRequest request) {
        validateCurrencyPair(request.getFromCurrency(), request.getToCurrency());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ExchangeRateValidationException("amount", request.getAmount(), "Amount must be non-negative");
        }

        LocalDate conversionDate = request.getConversionDate() != null ? request.getConversionDate()
                : dateTimeService.today();
        validateDate(conversionDate);

        // Get exchange rate
        BigDecimal exchangeRate = getExchangeRate(request.getFromCurrency(), request.getToCurrency(), conversionDate);

        // Convert amount
        BigDecimal convertedAmount = convertCurrency(
                request.getAmount(),
                request.getFromCurrency(),
                request.getToCurrency(),
                conversionDate,
                RateType.SPOT);

        return new CurrencyConversionResponse(
                request.getAmount(),
                request.getFromCurrency(),
                convertedAmount,
                request.getToCurrency(),
                exchangeRate,
                conversionDate);
    }

    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public List<BigDecimal> convertToCommonCurrency(List<CurrencyAmount> amounts, String targetCurrency,
            LocalDate rateDate, RateType rateType) {
        validateCurrencyCode(targetCurrency);

        return amounts.stream()
                .map(
                        amount -> convertCurrencyInternal(
                                amount.getAmount(),
                                amount.getCurrency(),
                                targetCurrency,
                                rateDate,
                                rateType))
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('exchange-rate:write', 'service:exchange-rate:write')")
    public ExchangeRateResponse createExchangeRate(ExchangeRateRequest request) {
        validateCurrencyPair(request.getSourceCurrency(), request.getTargetCurrency());
        validateRate(request.getRate());
        validateDate(request.getRateDate());

        // Create exchange rate entity
        ExchangeRate exchangeRate = new ExchangeRate(
                request.getSourceCurrency(),
                request.getTargetCurrency(),
                request.getRate(),
                request.getRateDate(),
                request.getRateType());

        // Validate the exchange rate entity
        validateExchangeRate(exchangeRate);

        // Check if rate already exists
        boolean exists = exchangeRateRepository.existsBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(
                request.getSourceCurrency(),
                request.getTargetCurrency(),
                request.getRateDate(),
                request.getRateType());

        if (exists) {
            throw new ExchangeRateValidationException(
                    "exchangeRate",
                    request,
                    "Exchange rate already exists for this currency pair, date, and type");
        }

        // Set bid/ask spread if provided
        exchangeRate.setBidRate(request.getBidRate());
        exchangeRate.setAskRate(request.getAskRate());

        // Save the exchange rate
        ExchangeRate saved = exchangeRateRepository.save(exchangeRate);

        // Clear cache
        evictRateCache(request.getSourceCurrency(), request.getTargetCurrency());

        return mapToResponse(saved);
    }

    @CacheEvict(value = "exchangeRates", allEntries = true)
    @PreAuthorize("hasAnyAuthority('exchange-rate:write', 'service:exchange-rate:write')")
    public ExchangeRateResponse updateExchangeRateById(UUID id, ExchangeRateRequest request) {
        validateCurrencyPair(request.getSourceCurrency(), request.getTargetCurrency());
        validateRate(request.getRate());
        validateDate(request.getRateDate());

        ExchangeRate existing = exchangeRateRepository.findById(id)
                .orElseThrow(() -> new ExchangeRateNotFoundException("Exchange rate not found with ID: " + id));

        // If the natural key is changing, make sure the new key isn't already taken by a different record
        boolean naturalKeyChanged = !existing.getSourceCurrency().equals(request.getSourceCurrency())
                || !existing.getTargetCurrency().equals(request.getTargetCurrency())
                || !existing.getRateDate().equals(request.getRateDate())
                || existing.getRateType() != request.getRateType();

        if (naturalKeyChanged) {
            boolean conflict = exchangeRateRepository.existsBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(
                    request.getSourceCurrency(),
                    request.getTargetCurrency(),
                    request.getRateDate(),
                    request.getRateType());
            if (conflict) {
                throw new ExchangeRateValidationException(
                        "exchangeRate",
                        request,
                        "An exchange rate already exists for " + request.getSourceCurrency() + "/"
                                + request.getTargetCurrency() + " on " + request.getRateDate() + " ("
                                + request.getRateType() + ")");
            }
        }

        existing.setSourceCurrency(request.getSourceCurrency());
        existing.setTargetCurrency(request.getTargetCurrency());
        existing.setRate(request.getRate());
        existing.setBidRate(request.getBidRate());
        existing.setAskRate(request.getAskRate());
        existing.setRateDate(request.getRateDate());
        existing.setRateType(request.getRateType());

        ExchangeRate saved = exchangeRateRepository.save(existing);
        return mapToResponse(saved);
    }

    @CacheEvict(value = "exchangeRates", allEntries = true)
    @PreAuthorize("hasAnyAuthority('exchange-rate:write', 'service:exchange-rate:write')")
    public void deleteExchangeRate(UUID id) {
        ExchangeRate existing = exchangeRateRepository.findById(id)
                .orElseThrow(() -> new ExchangeRateNotFoundException(id.toString()));
        exchangeRateRepository.delete(existing);
    }

    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public List<ExchangeRateResponse> getHistoricalRates(String sourceCurrency, String targetCurrency,
            LocalDate startDate, LocalDate endDate, RateType rateType) {
        validateCurrencyPair(sourceCurrency, targetCurrency);
        validateDate(startDate);
        validateDate(endDate);

        if (startDate.isAfter(endDate)) {
            throw new ExchangeRateValidationException(
                    "dateRange",
                    startDate + " to " + endDate,
                    "Start date cannot be after end date");
        }

        List<ExchangeRate> historicalRates = exchangeRateRepository
                .findBySourceCurrencyAndTargetCurrencyAndRateDateBetweenAndRateType(
                        sourceCurrency,
                        targetCurrency,
                        startDate,
                        endDate,
                        rateType);

        return historicalRates.stream().map(this::mapToResponse).toList();
    }

    @Cacheable(value = "exchangeRates", key = "#sourceCurrency + '_' + #targetCurrency + '_avg_' + #startDate + '_' + #endDate")
    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public BigDecimal getAverageRate(String sourceCurrency, String targetCurrency, LocalDate startDate,
            LocalDate endDate) {
        return getAverageRate(sourceCurrency, targetCurrency, startDate, endDate, RateType.SPOT);
    }

    @Cacheable(value = "exchangeRates", key = "#sourceCurrency + '_' + #targetCurrency + '_avg_' + #startDate + '_' + #endDate + '_' + #rateType")
    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public BigDecimal getAverageRate(String sourceCurrency, String targetCurrency, LocalDate startDate,
            LocalDate endDate, RateType rateType) {
        validateCurrencyPair(sourceCurrency, targetCurrency);
        validateDate(startDate);
        validateDate(endDate);

        if (startDate.isAfter(endDate)) {
            throw new ExchangeRateValidationException(
                    "dateRange",
                    startDate + " to " + endDate,
                    "Start date cannot be after end date");
        }

        // Get all historical rates within the date range
        List<ExchangeRate> historicalRates = exchangeRateRepository
                .findBySourceCurrencyAndTargetCurrencyAndRateDateBetweenAndRateType(
                        sourceCurrency,
                        targetCurrency,
                        startDate,
                        endDate,
                        rateType);

        if (historicalRates.isEmpty()) {
            throw new ExchangeRateNotFoundException(
                    "No exchange rates found for " + sourceCurrency + "/" + targetCurrency + " between " + startDate
                            + " and " + endDate + " for rate type " + rateType);
        }

        // Calculate the average rate
        BigDecimal sum = historicalRates.stream().map(ExchangeRate::getRate).reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(historicalRates.size()), 10, RoundingMode.HALF_UP);
    }

    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public boolean exchangeRateExists(String sourceCurrency, String targetCurrency, LocalDate date, RateType rateType) {
        validateCurrencyPair(sourceCurrency, targetCurrency);
        validateDate(date);
        return exchangeRateRepository.existsBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(
                sourceCurrency,
                targetCurrency,
                date,
                rateType);
    }

    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public BigDecimal calculateMultiCurrencyTotal(List<CurrencyAmount> amounts, String targetCurrency,
            LocalDate rateDate, RateType rateType) {
        List<BigDecimal> convertedAmounts = convertToCommonCurrency(amounts, targetCurrency, rateDate, rateType);

        return convertedAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
    }

    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public void validateMultiCurrencyTransaction(List<CurrencyAmount> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            throw new IllegalArgumentException("Currency amounts list cannot be null or empty");
        }

        for (CurrencyAmount amount : amounts) {
            if (amount == null) {
                throw new IllegalArgumentException("Currency amount cannot be null");
            }

            validateCurrencyCode(amount.getCurrency());

            if (amount.getAmount() == null || amount.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Amount must be non-negative");
            }
        }
    }

    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public List<String> getSupportedCurrencies() {
        return SupportedCurrency.getAllCodes().stream().sorted().toList();
    }

    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public void validateCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }

        String upperCurrency = currencyCode.toUpperCase().trim();

        if (upperCurrency.length() != 3) {
            throw new IllegalArgumentException("Currency code must be exactly 3 characters");
        }

        if (!SupportedCurrency.isValid(upperCurrency)) {
            throw new IllegalArgumentException("Unsupported currency code: " + upperCurrency);
        }
    }

    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public boolean isCurrencySupported(String currencyCode) {
        try {
            validateCurrencyCode(currencyCode);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates currency pair for exchange rate operations.
     */
    private void validateCurrencyPair(String source, String target) {
        if (source == null || source.trim().isEmpty()) {
            throw new InvalidCurrencyPairException(source, target, "Source currency cannot be null or empty");
        }
        if (target == null || target.trim().isEmpty()) {
            throw new InvalidCurrencyPairException(source, target, "Target currency cannot be null or empty");
        }

        try {
            validateCurrencyCode(source);
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyPairException(source, target, "Invalid source currency: " + e.getMessage());
        }

        try {
            validateCurrencyCode(target);
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyPairException(source, target, "Invalid target currency: " + e.getMessage());
        }

        if (source.equals(target)) {
            throw new InvalidCurrencyPairException(source, target, "Source and target currencies cannot be the same");
        }
    }

    /**
     * Validates exchange rate value.
     */
    private void validateRate(BigDecimal rate) {
        if (rate == null) {
            throw new ExchangeRateValidationException("rate", null, "Rate cannot be null");
        }
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExchangeRateValidationException("rate", rate, "Rate must be positive");
        }
        if (rate.scale() > 8) {
            throw new ExchangeRateValidationException("rate", rate, "Rate precision cannot exceed 8 decimal places");
        }
    }

    /**
     * Validates date for exchange rate operations.
     */
    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new ExchangeRateValidationException("date", null, "Date cannot be null");
        }
        if (date.isAfter(dateTimeService.today())) {
            throw new ExchangeRateValidationException("date", date, "Date cannot be in the future");
        }
    }

    /**
     * Maps ExchangeRate entity to ExchangeRateResponse DTO.
     */
    private ExchangeRateResponse mapToResponse(ExchangeRate exchangeRate) {
        ExchangeRateResponse response = new ExchangeRateResponse(
                exchangeRate.getId(),
                exchangeRate.getSourceCurrency(),
                exchangeRate.getTargetCurrency(),
                exchangeRate.getRate(),
                exchangeRate.getRateDate(),
                exchangeRate.getRateType(),
                exchangeRate.getCreatedAt());
        response.setBidRate(exchangeRate.getBidRate());
        response.setAskRate(exchangeRate.getAskRate());
        response.setCreatedBy(exchangeRate.getCreatedBy());
        response.setUpdatedAt(exchangeRate.getUpdatedAt());
        response.setUpdatedBy(exchangeRate.getUpdatedBy());
        response.setVersion(exchangeRate.getVersion());
        return response;
    }

    /**
     * Internal method to convert currency amounts.
     */
    private BigDecimal convertCurrencyInternal(BigDecimal amount, String sourceCurrency, String targetCurrency,
            LocalDate rateDate, RateType rateType) {
        validateCurrencyCode(sourceCurrency);
        validateCurrencyCode(targetCurrency);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }

        // Same currency, no conversion needed
        if (sourceCurrency.equals(targetCurrency)) {
            return amount;
        }

        BigDecimal exchangeRate = getExchangeRateInternal(sourceCurrency, targetCurrency, rateDate, rateType);
        return amount.multiply(exchangeRate).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Internal method to get exchange rate between two currencies for a specific date and type.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Direct row for the exact date.</li>
     *   <li>Inverse row for the exact date (1 / stored).</li>
     *   <li>Staleness fallback: most recent direct/inverse row within
     *       {@code app.exchange-rate.max-staleness-days} before the requested date. Lets requests for a
     *       date the scheduler hasn't published yet (e.g. weekends, late-day calls) succeed using the
     *       last published business-day rate.</li>
     *   <li>Cross via the bank's base currency (configured by {@code app.base-currency}).</li>
     * </ol>
     */
    private BigDecimal getExchangeRateInternal(String sourceCurrency, String targetCurrency, LocalDate rateDate,
            RateType rateType) {
        Optional<ExchangeRate> directRate = exchangeRateRepository
                .findBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(
                        sourceCurrency,
                        targetCurrency,
                        rateDate,
                        rateType);

        if (directRate.isPresent()) {
            return directRate.get().getRate();
        }

        Optional<ExchangeRate> inverseRate = exchangeRateRepository
                .findBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(
                        targetCurrency,
                        sourceCurrency,
                        rateDate,
                        rateType);

        if (inverseRate.isPresent()) {
            return BigDecimal.ONE.divide(inverseRate.get().getRate(), 8, RoundingMode.HALF_UP);
        }

        // Staleness fallback within the configured window — most recent row on or before requested date.
        int staleness = properties.getMaxStalenessDays();
        if (staleness > 0) {
            LocalDate earliest = rateDate.minusDays(staleness);

            Optional<ExchangeRate> staleDirect = exchangeRateRepository
                    .findFirstBySourceCurrencyAndTargetCurrencyAndRateTypeAndRateDateBetweenOrderByRateDateDesc(
                            sourceCurrency,
                            targetCurrency,
                            rateType,
                            earliest,
                            rateDate);
            if (staleDirect.isPresent()) {
                return staleDirect.get().getRate();
            }

            Optional<ExchangeRate> staleInverse = exchangeRateRepository
                    .findFirstBySourceCurrencyAndTargetCurrencyAndRateTypeAndRateDateBetweenOrderByRateDateDesc(
                            targetCurrency,
                            sourceCurrency,
                            rateType,
                            earliest,
                            rateDate);
            if (staleInverse.isPresent()) {
                return BigDecimal.ONE.divide(staleInverse.get().getRate(), 8, RoundingMode.HALF_UP);
            }
        }

        // Cross-currency via the bank's base currency (e.g. EUR for an ECB-driven setup).
        if (!sourceCurrency.equals(baseCurrency) && !targetCurrency.equals(baseCurrency)) {
            try {
                BigDecimal sourceToBase = getExchangeRateInternal(sourceCurrency, baseCurrency, rateDate, rateType);
                BigDecimal baseToTarget = getExchangeRateInternal(baseCurrency, targetCurrency, rateDate, rateType);
                return sourceToBase.multiply(baseToTarget).setScale(8, RoundingMode.HALF_UP);
            } catch (IllegalArgumentException e) {
                // Cross-currency conversion failed, continue to error
            }
        }

        throw new IllegalArgumentException(
                String.format(
                        "Exchange rate not found for %s to %s on %s (type: %s)",
                        sourceCurrency,
                        targetCurrency,
                        rateDate,
                        rateType));
    }

    /**
     * Internal method to get the latest exchange rate between two currencies.
     */
    private BigDecimal getLatestExchangeRateInternal(String sourceCurrency, String targetCurrency, RateType rateType) {
        Optional<ExchangeRate> latestRate = exchangeRateRepository
                .findFirstBySourceCurrencyAndTargetCurrencyAndRateTypeOrderByRateDateDesc(
                        sourceCurrency,
                        targetCurrency,
                        rateType);

        if (latestRate.isPresent()) {
            return latestRate.get().getRate();
        }

        // Try inverse rate
        Optional<ExchangeRate> inverseRate = exchangeRateRepository
                .findFirstBySourceCurrencyAndTargetCurrencyAndRateTypeOrderByRateDateDesc(
                        targetCurrency,
                        sourceCurrency,
                        rateType);

        if (inverseRate.isPresent()) {
            return BigDecimal.ONE.divide(inverseRate.get().getRate(), 8, RoundingMode.HALF_UP);
        }

        throw new IllegalArgumentException(
                String.format(
                        "Latest exchange rate not found for %s to %s (type: %s)",
                        sourceCurrency,
                        targetCurrency,
                        rateType));
    }

    /**
     * Evicts cache for both direct and inverse currency pairs when rates are updated.
     */
    private void evictRateCache(String source, String target) {
        evictPairCache(source, target);
        evictPairCache(target, source);
    }

    /**
     * Evicts cache for a single currency pair. Parameters are used in {@code @CacheEvict} key.
     */
    // Parameters are referenced only in the @CacheEvict SpEL key, not in method body.
    @SuppressWarnings("unused")
    @CacheEvict(value = "exchangeRates", key = "#source + '_' + #target + '_latest'")
    private void evictPairCache(String source, String target) {
        // Eviction performed by @CacheEvict
    }

    /**
     * Validates exchange rate data consistency.
     *
     * @param exchangeRate the exchange rate to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateExchangeRate(ExchangeRate exchangeRate) {
        if (exchangeRate == null) {
            return;
        }

        // Validate source and target currencies
        validateCurrencyCode(exchangeRate.getSourceCurrency());
        validateCurrencyCode(exchangeRate.getTargetCurrency());

        // Validate currencies are different
        if (exchangeRate.getSourceCurrency().equals(exchangeRate.getTargetCurrency())) {
            throw new IllegalArgumentException("Source and target currencies cannot be the same");
        }

        // Validate rate is positive
        if (exchangeRate.getRate() == null || exchangeRate.getRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }

        // Validate rate date is not in the future
        if (exchangeRate.getRateDate().isAfter(dateTimeService.today())) {
            throw new IllegalArgumentException("Exchange rate date cannot be in the future");
        }

        // Validate rate precision (should not exceed 8 decimal places)
        if (exchangeRate.getRate().scale() > 8) {
            throw new IllegalArgumentException("Exchange rate precision cannot exceed 8 decimal places");
        }

        // Validate bid/ask spread consistency
        BigDecimal bid = exchangeRate.getBidRate();
        BigDecimal ask = exchangeRate.getAskRate();
        BigDecimal mid = exchangeRate.getRate();
        if (bid != null && bid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bid rate must be positive");
        }
        if (ask != null && ask.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ask rate must be positive");
        }
        if (bid != null && bid.compareTo(mid) > 0) {
            throw new IllegalArgumentException("Bid rate cannot exceed mid rate");
        }
        if (ask != null && ask.compareTo(mid) < 0) {
            throw new IllegalArgumentException("Ask rate cannot be below mid rate");
        }
        if (bid != null && ask != null && bid.compareTo(ask) >= 0) {
            throw new IllegalArgumentException("Bid rate must be strictly less than ask rate");
        }
    }

    @PreAuthorize("hasAnyAuthority('exchange-rate:read', 'service:exchange-rate:read')")
    public BigDecimal calculateFXFee(BigDecimal amount, String fromCurrency, String toCurrency, double tierMultiplier) {
        if (fromCurrency == null || toCurrency == null || fromCurrency.equals(toCurrency)) {
            return BigDecimal.ZERO;
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal baseSpread = getBaseSpread(fromCurrency, toCurrency);
        BigDecimal effectiveSpread = baseSpread.multiply(BigDecimal.valueOf(tierMultiplier));
        BigDecimal fee = amount.multiply(effectiveSpread).setScale(4, RoundingMode.HALF_UP);

        return fee;
    }

    private BigDecimal getBaseSpread(String fromCurrency, String toCurrency) {
        return fxSpreadRepository.findBySourceCurrencyAndTargetCurrency(fromCurrency, toCurrency)
                .map(FXSpread::getSpreadRate)
                .or(
                        () -> fxSpreadRepository.findBySourceCurrencyAndTargetCurrency(toCurrency, fromCurrency)
                                .map(FXSpread::getSpreadRate))
                .orElse(DEFAULT_SPREAD);
    }
}