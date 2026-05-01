package com.openfinova.banking.customer.account.controller;

import com.openfinova.banking.customer.account.api.dto.AccountStatement;
import com.openfinova.banking.customer.account.api.dto.StatementPeriod;
import com.openfinova.banking.customer.account.service.AccountService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Account Statements", description = "APIs for generating account statements")
/**
 * REST controller for account statement generation.
 * Handles generating statements for date ranges, monthly statements, and retrieving available statement periods.
 */
public class AccountStatementController {

    private static final Logger log = LoggerFactory.getLogger(AccountStatementController.class);

    private final AccountService accountService;

    public AccountStatementController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}/statements")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Generate statement", description = "Generates an account statement for a specific period")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Statement generated successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<AccountStatement> generateStatement(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "From date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "To date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        log.info("Generating statement for account {} from {} to {}", id, fromDate, toDate);

        AccountStatement statement = accountService.generateAccountStatement(id, fromDate, toDate);

        log.info("Successfully generated statement for account: {}", id);

        return ResponseEntity.ok(statement);
    }

    @GetMapping("/{id}/statements/monthly")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Generate monthly statement", description = "Generates a monthly account statement")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Monthly statement generated successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<AccountStatement> generateMonthlyStatement(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Year") @RequestParam int year,
            @Parameter(description = "Month (1-12)") @RequestParam int month) {

        log.info("Generating monthly statement for account {}: {}/{}", id, year, month);

        AccountStatement statement = accountService.generateMonthlyStatement(id, year, month);

        log.info("Successfully generated monthly statement for account: {}", id);

        return ResponseEntity.ok(statement);
    }

    @GetMapping("/{id}/statements/periods")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get available periods", description = "Gets all available statement periods for an account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statement periods retrieved successfully") })
    public ResponseEntity<List<StatementPeriod>> getAvailableStatementPeriods(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id) {

        log.info("Fetching available statement periods for account: {}", id);

        List<StatementPeriod> periods = accountService.getAvailableStatementPeriods(id);

        log.info("Found {} statement periods for account: {}", periods.size(), id);

        return ResponseEntity.ok(periods);
    }
}
