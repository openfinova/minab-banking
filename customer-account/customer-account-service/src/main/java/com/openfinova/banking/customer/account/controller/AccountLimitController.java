package com.openfinova.banking.customer.account.controller;

import com.openfinova.banking.customer.account.mapper.AccountLimitMapper;
import com.openfinova.banking.customer.account.api.dto.AccountLimitResponse;
import com.openfinova.banking.customer.account.api.dto.AddLimitRequest;
import com.openfinova.banking.customer.account.api.dto.ValidationResult;
import com.openfinova.banking.customer.account.api.entity.LimitType;
import com.openfinova.banking.customer.account.entity.AccountLimit;
import com.openfinova.banking.customer.account.service.AccountLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Account Limits", description = "APIs for managing account limits and constraints")
/**
 * REST controller for account limit management.
 * Handles adding, retrieving, updating, and removing limits on customer accounts.
 */
public class AccountLimitController {

    private static final Logger log = LoggerFactory.getLogger(AccountLimitController.class);

    private final AccountLimitService limitService;
    private final AccountLimitMapper limitMapper;

    public AccountLimitController(AccountLimitService limitService, AccountLimitMapper limitMapper) {
        this.limitService = limitService;
        this.limitMapper = limitMapper;
    }

    @PostMapping("/{id}/limits")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Add limit", description = "Adds a new limit to an account")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Limit added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data") })
    public ResponseEntity<AccountLimitResponse> addLimit(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody AddLimitRequest request) {

        log.info(
                "Adding limit to account {}: type={}, period={}",
                id,
                request.getLimitType(),
                request.getLimitPeriod());

        AccountLimit limit = limitService.addLimit(
                id,
                request.getLimitType(),
                request.getLimitPeriod(),
                request.getMaxAmount(),
                request.getMaxCount(),
                request.getCreatedBy());

        log.info("Successfully added limit with ID: {}", limit.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(limitMapper.toResponse(limit));
    }

    @GetMapping("/{id}/limits")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get effective limits", description = "Retrieves all effective limits for an account")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Limits retrieved successfully") })
    public ResponseEntity<List<AccountLimitResponse>> getEffectiveLimits(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id) {

        log.info("Fetching effective limits for account: {}", id);

        List<AccountLimit> limits = limitService.getEffectiveLimitsByAccount(id);
        List<AccountLimitResponse> response = limits.stream().map(limitMapper::toResponse).toList();

        log.info("Found {} effective limits for account: {}", response.size(), id);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/limits/check")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Check limit", description = "Checks if a transaction amount violates any active limits")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Limit check completed") })
    public ResponseEntity<ValidationResult> checkLimit(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Limit type to check") @RequestParam LimitType limitType,
            @Parameter(description = "Transaction amount") @RequestParam BigDecimal amount) {

        log.info("Checking limit for account {}: type={}, amount={}", id, limitType, amount);

        ValidationResult result = limitService.checkLimit(id, limitType, amount);

        log.info("Limit check result for account {}: {}", id, result.isValid() ? "VALID" : "INVALID");

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/limits/{limitId}")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Remove limit", description = "Permanently removes or expires a limit")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Limit removed successfully"),
            @ApiResponse(responseCode = "404", description = "Limit not found") })
    public ResponseEntity<Void> removeLimit(
            @Parameter(description = "Limit ID", required = true) @PathVariable UUID limitId,
            @Parameter(description = "User removing the limit") @RequestParam String removedBy) {

        log.info("Removing limit with ID: {}", limitId);

        limitService.removeLimit(limitId, removedBy);

        log.info("Successfully removed limit: {}", limitId);

        return ResponseEntity.ok().build();
    }
}
