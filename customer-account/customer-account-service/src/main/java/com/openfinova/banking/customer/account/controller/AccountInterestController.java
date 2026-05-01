package com.openfinova.banking.customer.account.controller;

import com.openfinova.banking.customer.account.mapper.InterestRateMapper;
import com.openfinova.banking.customer.account.api.dto.InterestRateResponse;
import com.openfinova.banking.customer.account.api.dto.SetInterestRateRequest;
import com.openfinova.banking.customer.account.entity.InterestRate;
import com.openfinova.banking.customer.account.service.AccountService;
import com.openfinova.banking.customer.account.service.InterestRateService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Account Interest", description = "APIs for managing account interest rates and accrual")
/**
 * REST controller for account interest management.
 * Handles setting interest rates, calculating accrued interest, and processing interest accrual.
 */
public class AccountInterestController {

    private static final Logger log = LoggerFactory.getLogger(AccountInterestController.class);

    private final InterestRateService interestRateService;
    private final AccountService accountService;
    private final InterestRateMapper interestRateMapper;

    public AccountInterestController(InterestRateService interestRateService, AccountService accountService,
            InterestRateMapper interestRateMapper) {
        this.interestRateService = interestRateService;
        this.accountService = accountService;
        this.interestRateMapper = interestRateMapper;
    }

    @PostMapping("/{id}/interest/rates")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Set interest rate", description = "Configures a new interest rate for an account")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Interest rate set successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data") })
    public ResponseEntity<InterestRateResponse> setInterestRate(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody SetInterestRateRequest request) {

        log.info(
                "Setting interest rate for account {}: type={}, rate={}",
                id,
                request.getRateType(),
                request.getAnnualPercentageRate());

        InterestRate.RateType rateType = InterestRate.RateType.valueOf(request.getRateType().name());
        InterestRate rate = interestRateService
                .addInterestRate(id, rateType, request.getAnnualPercentageRate(), request.getEffectiveFrom());

        log.info("Successfully set interest rate with ID: {}", rate.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(interestRateMapper.toResponse(rate));
    }

    @GetMapping("/{id}/interest/rates/current")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get current rate", description = "Retrieves the currently effective interest rate")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Interest rate retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No active rate found") })
    public ResponseEntity<InterestRateResponse> getCurrentRate(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Rate type") @RequestParam InterestRate.RateType rateType) {

        log.info("Fetching current interest rate for account {}: type={}", id, rateType);

        InterestRate rate = interestRateService.getEffectiveRateByType(id, rateType);

        if (rate == null) {
            log.warn("No active interest rate found for account {} and type {}", id, rateType);
            return ResponseEntity.notFound().build();
        }

        log.info("Found current interest rate for account: {}", id);

        return ResponseEntity.ok(interestRateMapper.toResponse(rate));
    }

    @GetMapping("/{id}/interest/calculate")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Calculate accrued interest", description = "Calculates accrued interest for a period")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Interest calculated successfully") })
    public ResponseEntity<Map<String, BigDecimal>> calculateAccruedInterest(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "From date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "To date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        log.info("Calculating accrued interest for account {} from {} to {}", id, fromDate, toDate);

        BigDecimal accruedInterest = accountService.calculateAccruedInterest(id, fromDate, toDate);

        log.info("Calculated accrued interest for account {}: {}", id, accruedInterest);

        return ResponseEntity.ok(Map.of("accruedInterest", accruedInterest));
    }

    @PostMapping("/{id}/interest/post")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Post accrued interest", description = "Posts accrued interest to an account")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Interest posted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data") })
    public ResponseEntity<Void> postAccruedInterest(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Interest amount") @RequestParam BigDecimal interestAmount,
            @Parameter(description = "Posting date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate postingDate,
            @Parameter(description = "Posted by") @RequestParam String postedBy) {

        log.info("Posting accrued interest for account {}: amount={}, date={}", id, interestAmount, postingDate);

        accountService.postAccruedInterest(id, interestAmount, postingDate, postedBy);

        log.info("Successfully posted accrued interest for account: {}", id);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/interest/process-accrual")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Process interest accrual", description = "Runs interest accrual for all eligible accounts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Interest accrual processed successfully") })
    public ResponseEntity<Map<String, Integer>> processInterestAccrual(
            @Parameter(description = "Accrual date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate accrualDate) {

        log.info("Processing interest accrual for date: {}", accrualDate);

        int count = accountService.processInterestAccrual(accrualDate);

        log.info("Processed interest accrual for {} accounts", count);

        return ResponseEntity.ok(Map.of("accountsProcessed", count));
    }
}
