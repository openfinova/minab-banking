package com.openfinova.banking.setup.controller;

import com.openfinova.banking.setup.api.dto.BankProperties;
import com.openfinova.banking.setup.api.dto.BankInfoResponse;
import com.openfinova.banking.setup.api.dto.CurrencyResponse;
import com.openfinova.banking.setup.service.BankConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for bank information operations.
 *
 * Exposes endpoints for:
 * - Bank information retrieval (customer-facing)
 */
@RestController
@RequestMapping("/api/v1/bank")
@Tag(name = "Bank", description = "APIs for bank information")
public class BankController {

    private static final Logger log = LoggerFactory.getLogger(BankController.class);

    private final BankConfigService bankConfigService;

    public BankController(BankConfigService bankConfigService) {
        this.bankConfigService = bankConfigService;
    }

    @GetMapping("/details")
    @PreAuthorize("hasAuthority('admin:config:read')")
    @Operation(summary = "Get bank details", description = "Retrieves the bank's details including name, currency, SWIFT code, and country code")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Bank details retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Error retrieving bank details") })
    public ResponseEntity<BankProperties> getBankDetails() {
        log.info("Fetching bank details");

        BankProperties bankProperties = bankConfigService.getBankDetails();

        log.info("Bank details retrieved successfully");

        return ResponseEntity.ok(bankProperties);
    }

    @GetMapping("/name")
    @PreAuthorize("hasAuthority('admin:config:read')")
    @Operation(summary = "Get bank name", description = "Retrieves the bank's name")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Bank name retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Error retrieving bank name") })
    public ResponseEntity<BankInfoResponse> getBankName() {
        log.info("Fetching bank name");

        String bankName = bankConfigService.getBankName();

        return ResponseEntity.ok(new BankInfoResponse(bankName));
    }

    @GetMapping("/currency")
    @PreAuthorize("hasAuthority('admin:config:read')")
    @Operation(summary = "Get bank's operating currency", description = "Retrieves the bank's primary operating currency code")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Bank currency retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Error retrieving bank currency") })
    public ResponseEntity<CurrencyResponse> getBankCurrency() {
        log.info("Fetching bank currency");

        String currency = bankConfigService.getCurrency();

        return ResponseEntity.ok(new CurrencyResponse(currency));
    }
}
