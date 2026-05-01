package com.openfinova.banking.exchangerate.controller;

import com.openfinova.banking.exchangerate.api.dto.CurrencyConversionRequest;
import com.openfinova.banking.exchangerate.api.dto.CurrencyConversionResponse;
import com.openfinova.banking.exchangerate.api.dto.ExchangeRateRequest;
import com.openfinova.banking.exchangerate.api.dto.ExchangeRateResponse;
import com.openfinova.banking.exchangerate.api.entity.RateType;
import com.openfinova.banking.exchangerate.api.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for exchange rate and currency conversion operations.
 *
 * Exposes endpoints for:
 * - Currency conversion (customer-facing)
 * - Exchange rate lookup (customer-facing)
 * - Exchange rate management (administrative)
 * - Historical rate queries (administrative/reporting)
 *
 * NOTE: Internal validation methods (validateMultiCurrencyTransaction,
 * validateCurrencyCode)
 * are called internally by TransactionService and should NOT be exposed via
 * REST API.
 */
@RestController
@RequestMapping("/api/v1/exchange")
@Tag(name = "Exchange Rates", description = "APIs for currency exchange rates and conversions")
public class ExchangeRateController {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateController.class);

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @PostMapping("/convert")
    @PreAuthorize("hasAuthority('exchange-rate:read')")
    @Operation(summary = "Convert currency", description = "Converts an amount from one currency to another using current exchange rates")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Currency converted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid conversion request"),
            @ApiResponse(responseCode = "404", description = "Exchange rate not found") })
    public ResponseEntity<CurrencyConversionResponse> convertCurrency(
            @Valid @RequestBody CurrencyConversionRequest request) {

        log.info(
                "Converting currency: {} {} to {}",
                request.getAmount(),
                request.getFromCurrency(),
                request.getToCurrency());

        CurrencyConversionResponse response = exchangeRateService.convertCurrency(request);

        log.info(
                "Successfully converted: {} {} = {} {} (rate: {})",
                response.getOriginalAmount(),
                response.getFromCurrency(),
                response.getConvertedAmount(),
                response.getToCurrency(),
                response.getExchangeRate());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/rate")
    @PreAuthorize("hasAuthority('exchange-rate:read')")
    @Operation(summary = "Get exchange rate", description = "Gets the latest exchange rate between two currencies")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Exchange rate retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Exchange rate not found") })
    public ResponseEntity<Map<String, Object>> getExchangeRate(
            @Parameter(description = "Source currency code (3 letters)", required = true) @RequestParam String sourceCurrency,
            @Parameter(description = "Target currency code (3 letters)", required = true) @RequestParam String targetCurrency,
            @Parameter(description = "Rate type (optional, defaults to SPOT)") @RequestParam(required = false, defaultValue = "SPOT") RateType rateType) {

        log.info("Fetching exchange rate: {} to {}, type: {}", sourceCurrency, targetCurrency, rateType);

        BigDecimal rate = exchangeRateService.getExchangeRate(sourceCurrency, targetCurrency, rateType);

        return ResponseEntity.ok(
                Map.of(
                        "sourceCurrency",
                        sourceCurrency,
                        "targetCurrency",
                        targetCurrency,
                        "rate",
                        rate,
                        "rateType",
                        rateType));
    }

    @GetMapping("/rate/details")
    @PreAuthorize("hasAuthority('exchange-rate:read')")
    @Operation(summary = "Get exchange rate details", description = "Gets detailed exchange rate information including metadata")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange rate details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Exchange rate not found") })
    public ResponseEntity<ExchangeRateResponse> getExchangeRateDetails(
            @Parameter(description = "Source currency code", required = true) @RequestParam String sourceCurrency,
            @Parameter(description = "Target currency code", required = true) @RequestParam String targetCurrency,
            @Parameter(description = "Rate type (optional, defaults to SPOT)") @RequestParam(required = false, defaultValue = "SPOT") RateType rateType) {

        log.info("Fetching exchange rate details: {} to {}, type: {}", sourceCurrency, targetCurrency, rateType);

        ExchangeRateResponse response = exchangeRateService
                .getLatestExchangeRateDetails(sourceCurrency, targetCurrency, rateType);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/rate/historical")
    @PreAuthorize("hasAuthority('exchange-rate:read')")
    @Operation(summary = "Get exchange rate for specific date", description = "Gets the exchange rate between two currencies for a specific date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historical exchange rate retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Exchange rate not found for the specified date") })
    public ResponseEntity<Map<String, Object>> getHistoricalExchangeRate(
            @Parameter(description = "Source currency code", required = true) @RequestParam String sourceCurrency,
            @Parameter(description = "Target currency code", required = true) @RequestParam String targetCurrency,
            @Parameter(description = "Rate date", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Rate type (optional, defaults to SPOT)") @RequestParam(required = false, defaultValue = "SPOT") RateType rateType) {

        log.info(
                "Fetching historical exchange rate: {} to {} for date: {}, type: {}",
                sourceCurrency,
                targetCurrency,
                date,
                rateType);

        BigDecimal rate = exchangeRateService.getExchangeRate(sourceCurrency, targetCurrency, date, rateType);

        return ResponseEntity.ok(
                Map.of(
                        "sourceCurrency",
                        sourceCurrency,
                        "targetCurrency",
                        targetCurrency,
                        "rate",
                        rate,
                        "date",
                        date,
                        "rateType",
                        rateType));
    }

    @GetMapping("/currencies")
    @PreAuthorize("hasAuthority('exchange-rate:read')")
    @Operation(summary = "Get supported currencies", description = "Retrieves the list of all supported currency codes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Supported currencies retrieved successfully") })
    public ResponseEntity<List<String>> getSupportedCurrencies() {

        log.info("Fetching supported currencies");

        List<String> currencies = exchangeRateService.getSupportedCurrencies();

        log.info("Found {} supported currencies", currencies.size());

        return ResponseEntity.ok(currencies);
    }

    @GetMapping("/currencies/{currencyCode}/supported")
    @PreAuthorize("hasAuthority('exchange-rate:read')")
    @Operation(summary = "Check if currency is supported", description = "Checks if a specific currency code is supported")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Currency support status retrieved") })
    public ResponseEntity<Map<String, Object>> isCurrencySupported(
            @Parameter(description = "Currency code to check", required = true) @PathVariable String currencyCode) {

        log.info("Checking if currency is supported: {}", currencyCode);

        boolean supported = exchangeRateService.isCurrencySupported(currencyCode);

        return ResponseEntity.ok(Map.of("currencyCode", currencyCode, "supported", supported));
    }

    @PostMapping("/rates")
    @PreAuthorize("hasAuthority('exchange-rate:write')")
    @Operation(summary = "Create exchange rate", description = "Creates a new exchange rate entry. Administrative operation.")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Exchange rate created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid exchange rate data"),
            @ApiResponse(responseCode = "409", description = "Exchange rate already exists") })
    public ResponseEntity<ExchangeRateResponse> createExchangeRate(@Valid @RequestBody ExchangeRateRequest request) {

        log.info(
                "Creating exchange rate: {} to {} = {} for date: {}, type: {}",
                request.getSourceCurrency(),
                request.getTargetCurrency(),
                request.getRate(),
                request.getRateDate(),
                request.getRateType());

        ExchangeRateResponse response = exchangeRateService.createExchangeRate(request);

        log.info("Successfully created exchange rate with ID: {}", response.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/rates/{id}")
    @PreAuthorize("hasAuthority('exchange-rate:write')")
    @Operation(summary = "Update exchange rate by ID", description = "Corrects a specific exchange rate record identified by its UUID. "
            + "Validates the new natural key (sourceCurrency/targetCurrency/rateDate/rateType) "
            + "for uniqueness when it differs from the existing one. Administrative operation.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Exchange rate updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid exchange rate data"),
            @ApiResponse(responseCode = "404", description = "Exchange rate not found"),
            @ApiResponse(responseCode = "409", description = "New natural key conflicts with an existing record") })
    public ResponseEntity<ExchangeRateResponse> updateExchangeRateById(
            @Parameter(description = "UUID of the exchange rate record to update", required = true) @PathVariable UUID id,
            @Valid @RequestBody ExchangeRateRequest request,
            @Parameter(description = "Username of the operator making the correction (audit trail)", required = true) @RequestParam String updatedBy) {

        log.info(
                "Updating exchange rate {}: {} to {} = {} for {}/{} by {}",
                id,
                request.getSourceCurrency(),
                request.getTargetCurrency(),
                request.getRate(),
                request.getRateDate(),
                request.getRateType(),
                updatedBy);

        request.setUpdatedBy(updatedBy);
        ExchangeRateResponse response = exchangeRateService.updateExchangeRateById(id, request, updatedBy);

        log.info("Successfully updated exchange rate {}", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rates/{id}")
    @PreAuthorize("hasAuthority('exchange-rate:write')")
    @Operation(summary = "Delete exchange rate by ID", description = "Permanently removes an exchange rate record. "
            + "Use only to retract an erroneous publication; prefer PUT to correct the rate value. "
            + "Administrative operation.")
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Exchange rate deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Exchange rate not found") })
    public ResponseEntity<Void> deleteExchangeRate(
            @Parameter(description = "UUID of the exchange rate record to delete", required = true) @PathVariable UUID id,
            @Parameter(description = "Username of the operator requesting deletion (audit trail)", required = true) @RequestParam String deletedBy) {

        log.info("Deleting exchange rate {} by {}", id, deletedBy);

        exchangeRateService.deleteExchangeRate(id, deletedBy);

        log.info("Successfully deleted exchange rate {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rates/history")
    @PreAuthorize("hasAuthority('exchange-rate:write')")
    @Operation(summary = "Get historical rates", description = "Gets historical exchange rates for a currency pair within a date range. Administrative/reporting operation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historical rates retrieved successfully") })
    public ResponseEntity<List<ExchangeRateResponse>> getHistoricalRates(
            @Parameter(description = "Source currency code", required = true) @RequestParam String sourceCurrency,
            @Parameter(description = "Target currency code", required = true) @RequestParam String targetCurrency,
            @Parameter(description = "Start date", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Rate type (optional, defaults to SPOT)") @RequestParam(required = false, defaultValue = "SPOT") RateType rateType) {

        log.info(
                "Fetching historical rates: {} to {} from {} to {}, type: {}",
                sourceCurrency,
                targetCurrency,
                startDate,
                endDate,
                rateType);

        List<ExchangeRateResponse> rates = exchangeRateService
                .getHistoricalRates(sourceCurrency, targetCurrency, startDate, endDate, rateType);

        log.info("Found {} historical rates", rates.size());

        return ResponseEntity.ok(rates);
    }

    @GetMapping("/rates/exists")
    @PreAuthorize("hasAuthority('exchange-rate:write')")
    @Operation(summary = "Check if exchange rate exists", description = "Checks if an exchange rate exists for the given parameters. Administrative operation.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Existence check completed") })
    public ResponseEntity<Map<String, Boolean>> exchangeRateExists(
            @Parameter(description = "Source currency code", required = true) @RequestParam String sourceCurrency,
            @Parameter(description = "Target currency code", required = true) @RequestParam String targetCurrency,
            @Parameter(description = "Rate date", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Rate type (optional, defaults to SPOT)") @RequestParam(required = false, defaultValue = "SPOT") RateType rateType) {

        log.info(
                "Checking if exchange rate exists: {} to {} for date: {}, type: {}",
                sourceCurrency,
                targetCurrency,
                date,
                rateType);

        boolean exists = exchangeRateService.exchangeRateExists(sourceCurrency, targetCurrency, date, rateType);

        return ResponseEntity.ok(Map.of("exists", exists));
    }
}
