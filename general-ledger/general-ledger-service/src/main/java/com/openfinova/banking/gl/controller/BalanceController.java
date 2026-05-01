package com.openfinova.banking.gl.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.gl.api.dto.GLAccountBalance;
import com.openfinova.banking.gl.api.dto.TrialBalance;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import com.openfinova.banking.gl.service.BalanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/gl/balances")
@Tag(name = "GL Balance Inquiries", description = "APIs for querying GL account balances and trial balance")
public class BalanceController {

    private static final Logger log = LoggerFactory.getLogger(BalanceController.class);

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/accounts/{accountId}/current")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get current account balance", description = "Retrieves the current balance of a GL account")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<GLAccountBalance> getCurrentBalance(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId) {

        log.info("Fetching current balance for account: {}", accountId);

        return balanceService.getAccountBalance(accountId).map(balance -> {
            log.info("Current balance for account {}: {}", accountId, balance.getCurrentBalance());
            return ResponseEntity.ok(balance);
        }).orElseGet(() -> {
            log.warn("Account not found: {}", accountId);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping("/accounts/code/{accountCode}/current")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get current balance by account code", description = "Retrieves the current balance using account code")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<GLAccountBalance> getCurrentBalanceByCode(
            @Parameter(description = "Account code", required = true, example = "1000") @PathVariable String accountCode) {

        log.info("Fetching current balance for account code: {}", accountCode);

        return balanceService.getAccountBalanceByCode(accountCode).map(balance -> {
            log.info("Current balance for account {}: {}", accountCode, balance.getCurrentBalance());
            return ResponseEntity.ok(balance);
        }).orElseGet(() -> {
            log.warn("Account not found with code: {}", accountCode);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping("/accounts/{accountId}/historical")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get historical balance", description = "Retrieves the balance of an account as of a specific date")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<Map<String, Object>> getHistoricalBalance(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "As-of date", required = true, example = "2024-01-15") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        log.info("Fetching historical balance for account {} as of {}", accountId, asOfDate);

        BigDecimal balance = balanceService.getBalanceAtDate(accountId, asOfDate);
        log.info("Historical balance for account {} as of {}: {}", accountId, asOfDate, balance);

        return ResponseEntity.ok(Map.of("accountId", accountId, "asOfDate", asOfDate, "balance", balance));
    }

    @GetMapping("/accounts/{accountId}/activity")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get account activity", description = "Retrieves debit/credit activity for an account on a specific date")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Activity retrieved successfully") })
    public ResponseEntity<GLAccountBalance> getAccountActivity(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Activity date", required = true, example = "2024-01-15") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate activityDate) {

        log.info("Fetching activity for account {} on {}", accountId, activityDate);

        GLAccountBalance activity = balanceService.getAccountActivity(accountId, activityDate);
        log.info(
                "Activity for account {}: debits={}, credits={}",
                accountId,
                activity.getTotalDebits(),
                activity.getTotalCredits());

        return ResponseEntity.ok(activity);
    }

    @GetMapping("/accounts/{accountId}/change")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get balance change", description = "Calculates the net change in balance over a period")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance change calculated successfully") })
    public ResponseEntity<GLAccountBalance> getBalanceChange(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Start date", required = true, example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date", required = true, example = "2024-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Calculating balance change for account {} from {} to {}", accountId, startDate, endDate);

        GLAccountBalance change = balanceService.getBalanceChange(accountId, startDate, endDate);
        log.info(
                "Balance change for account {}: opening={}, closing={}, debits={}, credits={}",
                accountId,
                change.getOpeningBalance(),
                change.getClosingBalance(),
                change.getTotalDebits(),
                change.getTotalCredits());

        return ResponseEntity.ok(change);
    }

    @GetMapping("/trial-balance")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get trial balance", description = "Generates a trial balance report as of a specific date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trial balance generated successfully", content = @Content(schema = @Schema(implementation = TrialBalance.class))) })
    public ResponseEntity<TrialBalance> getTrialBalance(
            @Parameter(description = "As-of date", required = true, example = "2024-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        log.info("Generating trial balance as of {}", asOfDate);

        TrialBalance trialBalance = balanceService.getTrialBalance(asOfDate);
        log.info(
                "Trial balance generated: {} accounts, balanced={}",
                trialBalance.getAccountBalances().size(),
                trialBalance.isBalanced());

        return ResponseEntity.ok(trialBalance);
    }

    @GetMapping("/trial-balance/by-type")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get trial balance by account type", description = "Generates a filtered trial balance for specific account types")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Trial balance generated successfully") })
    public ResponseEntity<TrialBalance> getTrialBalanceByType(
            @Parameter(description = "As-of date", required = true, example = "2024-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @Parameter(description = "Account types to include", required = true) @RequestParam List<GLAccountType> accountTypes) {

        log.info("Generating trial balance as of {} for types: {}", asOfDate, accountTypes);

        TrialBalance trialBalance = balanceService.getTrialBalanceByType(asOfDate, accountTypes);
        log.info("Filtered trial balance generated: {} accounts", trialBalance.getAccountBalances().size());

        return ResponseEntity.ok(trialBalance);
    }

    @PostMapping("/accounts/{accountId}/recalculate")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Recalculate account balance", description = "Triggers a recalculation of account balance from transaction history")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Balance recalculated successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<Map<String, String>> recalculateBalance(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId) {

        log.info("Recalculating balance for account: {}", accountId);

        balanceService.recalculateBalance(accountId);
        log.info("Successfully recalculated balance for account: {}", accountId);

        return ResponseEntity
                .ok(Map.of("message", "Balance recalculated successfully", "accountId", accountId.toString()));
    }

    @GetMapping("/accounts/batch")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get balances for multiple accounts", description = "Retrieves balances for multiple accounts in a single request")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Balances retrieved successfully") })
    public ResponseEntity<Map<UUID, GLAccountBalance>> getAccountBalances(
            @Parameter(description = "List of account IDs", required = true) @RequestParam List<UUID> accountIds) {

        log.info("Fetching balances for {} accounts", accountIds.size());

        Map<UUID, GLAccountBalance> balances = balanceService.getAccountBalances(accountIds);
        log.info("Retrieved balances for {} accounts", balances.size());

        return ResponseEntity.ok(balances);
    }
}
