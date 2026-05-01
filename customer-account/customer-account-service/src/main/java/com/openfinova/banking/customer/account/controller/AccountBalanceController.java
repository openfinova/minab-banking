package com.openfinova.banking.customer.account.controller;

import com.openfinova.banking.customer.account.api.dto.AccountBalanceView;
import com.openfinova.banking.customer.account.api.dto.BalanceHistoryResponse;
import com.openfinova.banking.customer.account.service.AccountBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Account Balance", description = "APIs for account balance inquiries and management")
/**
 * REST controller for account balance operations.
 * Handles balance inquiries, balance history, and available balance calculations.
 */
public class AccountBalanceController {

    private static final Logger log = LoggerFactory.getLogger(AccountBalanceController.class);

    private final AccountBalanceService balanceService;

    public AccountBalanceController(AccountBalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get current balance", description = "Retrieves current balance for an account")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<AccountBalanceView> getCurrentBalance(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id) {

        log.info("Fetching current balance for account: {}", id);

        AccountBalanceView balance = balanceService.getDetailedBalance(id);

        log.info("Retrieved balance for account: {}", id);

        return ResponseEntity.ok(balance);
    }

    @GetMapping("/{id}/balance/detailed")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get detailed balance", description = "Retrieves detailed balance with GL account breakdown")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detailed balance retrieved successfully") })
    public ResponseEntity<AccountBalanceView> getDetailedBalance(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id) {

        log.info("Fetching detailed balance for account: {}", id);

        AccountBalanceView balance = balanceService.getDetailedBalance(id);

        log.info("Retrieved detailed balance for account: {}", id);

        return ResponseEntity.ok(balance);
    }

    @GetMapping("/{id}/balance/available")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get available balance", description = "Retrieves available balance considering holds")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Available balance retrieved successfully") })
    public ResponseEntity<Map<String, BigDecimal>> getAvailableBalance(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id) {

        log.info("Fetching available balance for account: {}", id);

        BigDecimal availableBalance = balanceService.getAvailableBalance(id);

        log.info("Retrieved available balance for account: {}", id);

        return ResponseEntity.ok(Map.of("availableBalance", availableBalance));
    }

    @GetMapping("/{id}/balance/history")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get balance history", description = "Retrieves balance history over a date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance history retrieved successfully") })
    public ResponseEntity<BalanceHistoryResponse> getBalanceHistory(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Fetching balance history for account {} from {} to {}", id, startDate, endDate);

        BalanceHistoryResponse history = balanceService.getBalanceHistory(id, startDate, endDate);

        log.info("Retrieved balance history for account: {}", id);

        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}/balance/trends")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get balance trends", description = "Calculates balance trends and analytics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance trends calculated successfully") })
    public ResponseEntity<BalanceHistoryResponse> getBalanceTrends(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Number of days to analyze") @RequestParam(defaultValue = "30") int days) {

        log.info("Calculating balance trends for account {} over {} days", id, days);

        BalanceHistoryResponse trends = balanceService.calculateBalanceTrends(id, days);

        log.info("Calculated balance trends for account: {}", id);

        return ResponseEntity.ok(trends);
    }

    @GetMapping("/{id}/balance/as-of")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get balance as of date", description = "Retrieves balance as of a specific date")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Balance retrieved successfully") })
    public ResponseEntity<AccountBalanceView> getBalanceAsOfDate(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "As of date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        log.info("Fetching balance for account {} as of {}", id, asOfDate);

        AccountBalanceView balance = balanceService.getBalanceAsOfDate(id, asOfDate);

        log.info("Retrieved balance as of date for account: {}", id);

        return ResponseEntity.ok(balance);
    }

    @PostMapping("/{id}/balance/refresh")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Refresh balance view", description = "Forces recalculation of balance from GL accounts")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Balance refreshed successfully") })
    public ResponseEntity<Void> refreshBalanceView(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id) {

        log.info("Refreshing balance view for account: {}", id);

        balanceService.refreshBalanceView(id);

        log.info("Successfully refreshed balance view for account: {}", id);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/balance/validate")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Validate balance consistency", description = "Validates balance consistency between account and GL")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Validation completed") })
    public ResponseEntity<Map<String, Boolean>> validateBalanceConsistency(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id) {

        log.info("Validating balance consistency for account: {}", id);

        boolean isConsistent = balanceService.validateBalanceConsistency(id);

        log.info("Balance consistency validation for account {}: {}", id, isConsistent ? "CONSISTENT" : "INCONSISTENT");

        return ResponseEntity.ok(Map.of("isConsistent", isConsistent));
    }
}
