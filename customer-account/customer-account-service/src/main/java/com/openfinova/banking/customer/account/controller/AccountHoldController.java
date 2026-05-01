package com.openfinova.banking.customer.account.controller;

import com.openfinova.banking.customer.account.api.dto.AccountHoldResponse;
import com.openfinova.banking.customer.account.api.dto.PlaceHoldRequest;
import com.openfinova.banking.customer.account.entity.AccountHold;
import com.openfinova.banking.customer.account.mapper.AccountHoldMapper;
import com.openfinova.banking.customer.account.service.AccountHoldService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Account Holds", description = "APIs for managing account holds")
/**
 * REST controller for account hold management.
 * Handles placing, releasing, and querying holds on customer accounts.
 */
public class AccountHoldController {

    private static final Logger log = LoggerFactory.getLogger(AccountHoldController.class);

    private final AccountHoldService holdService;
    private final AccountHoldMapper holdMapper;

    public AccountHoldController(AccountHoldService holdService, AccountHoldMapper holdMapper) {
        this.holdService = holdService;
        this.holdMapper = holdMapper;
    }

    @PostMapping("/{id}/holds")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Place hold", description = "Places an administrative hold on account funds")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Hold placed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data") })
    public ResponseEntity<AccountHoldResponse> placeHold(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody PlaceHoldRequest request) {

        log.info("Placing hold on account {} for amount: {}", id, request.getAmount());

        AccountHold hold = holdService
                .placeHold(id, request.getAmount(), request.getCurrency(), request.getReason(), request.getExpiresAt());

        log.info("Successfully placed hold with ID: {}", hold.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(holdMapper.toResponse(hold));
    }

    @GetMapping("/{id}/holds")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get active holds", description = "Retrieves all active holds for an account")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Holds retrieved successfully") })
    public ResponseEntity<List<AccountHoldResponse>> getActiveHolds(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id) {

        log.info("Fetching active holds for account: {}", id);

        List<AccountHold> holds = holdService.getActiveHoldsByAccount(id);
        List<AccountHoldResponse> response = holds.stream().map(holdMapper::toResponse).toList();

        log.info("Found {} active holds for account: {}", response.size(), id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/holds/total")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get total hold amount", description = "Gets the total amount currently held for an account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total hold amount retrieved successfully") })
    public ResponseEntity<Map<String, BigDecimal>> getTotalHoldAmount(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id) {

        log.info("Fetching total hold amount for account: {}", id);

        BigDecimal totalHoldAmount = holdService.getTotalHoldAmount(id);

        log.info("Total hold amount for account {}: {}", id, totalHoldAmount);

        return ResponseEntity.ok(Map.of("totalHoldAmount", totalHoldAmount));
    }

    @PostMapping("/holds/{holdId}/release")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Release hold", description = "Releases an active hold, making funds available")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Hold released successfully"),
            @ApiResponse(responseCode = "404", description = "Hold not found") })
    public ResponseEntity<Void> releaseHold(
            @Parameter(description = "Hold ID", required = true) @PathVariable UUID holdId) {

        log.info("Releasing hold with ID: {}", holdId);

        holdService.releaseHold(holdId);

        log.info("Successfully released hold: {}", holdId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/holds/{holdId}/settle")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Settle hold", description = "Marks a hold as settled (consumed by transaction)")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Hold settled successfully"),
            @ApiResponse(responseCode = "404", description = "Hold not found") })
    public ResponseEntity<Void> settleHold(
            @Parameter(description = "Hold ID", required = true) @PathVariable UUID holdId) {

        log.info("Settling hold with ID: {}", holdId);

        holdService.settleHold(holdId);

        log.info("Successfully settled hold: {}", holdId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/holds/process-expired")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Process expired holds", description = "Automatically expires holds that have passed expiration date")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Expired holds processed successfully") })
    public ResponseEntity<Map<String, Integer>> processExpiredHolds() {

        log.info("Processing expired holds");

        int count = holdService.processExpiredHolds();

        log.info("Processed {} expired holds", count);

        return ResponseEntity.ok(Map.of("holdsExpired", count));
    }
}
