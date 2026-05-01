package com.openfinova.banking.tp.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.tp.api.dto.CreateVelocityLimitRequest;
import com.openfinova.banking.tp.api.dto.UpdateVelocityLimitRequest;
import com.openfinova.banking.tp.api.dto.VelocityLimitBreachDTO;
import com.openfinova.banking.tp.api.dto.VelocityLimitResponse;
import com.openfinova.banking.tp.api.dto.VelocityLimitStatus;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.api.entity.VelocityLimitPeriod;
import com.openfinova.banking.tp.entity.VelocityLimit;
import com.openfinova.banking.tp.mapper.VelocityLimitMapper;
import com.openfinova.banking.tp.service.VelocityLimitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for velocity limit configuration and monitoring.
 *
 * NOTE: This controller exposes configuration and customer-facing monitoring endpoints.
 * Internal limit checking methods (checkLimits, incrementUsage, resetExpiredLimits, etc.)
 * are called internally by TransactionService and should NOT be exposed via REST API.
 *
 * Configuration endpoints:
 * - Velocity Limit CRUD operations
 *
 * Customer-facing monitoring endpoints:
 * - View remaining limits
 * - View limit status
 * - View breach history
 */
@RestController
@RequestMapping("/api/v1/velocity-limits")
@Tag(name = "Velocity Limits", description = "APIs for managing transaction velocity and volume limits")
public class VelocityLimitController {

    private static final Logger log = LoggerFactory.getLogger(VelocityLimitController.class);

    private final VelocityLimitService velocityLimitService;
    private final VelocityLimitMapper velocityLimitMapper;

    public VelocityLimitController(VelocityLimitService velocityLimitService, VelocityLimitMapper velocityLimitMapper) {
        this.velocityLimitService = velocityLimitService;
        this.velocityLimitMapper = velocityLimitMapper;
    }

    // Configuration Endpoints

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAuthority('velocity-limit:read')")
    @Operation(summary = "Get velocity limits by account", description = "Retrieves all velocity limits configured for a specific account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Velocity limits retrieved successfully") })
    public ResponseEntity<List<VelocityLimitResponse>> getVelocityLimitsByAccount(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId) {

        log.info("Fetching velocity limits for account: {}", accountId);

        List<VelocityLimit> limits = velocityLimitService.getVelocityLimitsByAccount(accountId);
        List<VelocityLimitResponse> response = limits.stream().map(velocityLimitMapper::toResponse).toList();

        log.info("Found {} velocity limits for account: {}", response.size(), accountId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAuthority('velocity-limit:read')")
    @Operation(summary = "Get velocity limits by transaction type", description = "Retrieves all velocity limits for a specific transaction type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Velocity limits retrieved successfully") })
    public ResponseEntity<List<VelocityLimitResponse>> getVelocityLimitsByType(
            @Parameter(description = "Transaction type", required = true) @PathVariable TransactionType type) {

        log.info("Fetching velocity limits for type: {}", type);

        List<VelocityLimit> limits = velocityLimitService.getVelocityLimitsByType(type);
        List<VelocityLimitResponse> response = limits.stream().map(velocityLimitMapper::toResponse).toList();

        log.info("Found {} velocity limits for type: {}", response.size(), type);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin:config:write')")
    @Operation(summary = "Create velocity limit", description = "Creates a new velocity limit with validation")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Velocity limit created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid velocity limit data") })
    public ResponseEntity<VelocityLimitResponse> createVelocityLimit(
            @Valid @RequestBody CreateVelocityLimitRequest request) {

        log.info(
                "Creating velocity limit for account {}: type={}, period={}",
                request.getAccountId(),
                request.getTransactionType(),
                request.getPeriod());

        VelocityLimit limit = velocityLimitMapper.toEntity(request);
        VelocityLimit created = velocityLimitService.createVelocityLimit(limit);

        log.info("Successfully created velocity limit with ID: {}", created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(velocityLimitMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:config:write')")
    @Operation(summary = "Update velocity limit", description = "Updates an existing velocity limit")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Velocity limit updated successfully"),
            @ApiResponse(responseCode = "404", description = "Velocity limit not found") })
    public ResponseEntity<VelocityLimitResponse> updateVelocityLimit(
            @Parameter(description = "Limit ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody UpdateVelocityLimitRequest request) {

        log.info("Updating velocity limit: {}", id);

        VelocityLimit updatedLimit = velocityLimitMapper.toEntity(request);
        VelocityLimit updated = velocityLimitService.updateVelocityLimit(id, updatedLimit);

        log.info("Successfully updated velocity limit: {}", id);

        return ResponseEntity.ok(velocityLimitMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:config:write')")
    @Operation(summary = "Delete velocity limit", description = "Deletes a velocity limit by ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Velocity limit deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Velocity limit not found") })
    public ResponseEntity<Void> deleteVelocityLimit(
            @Parameter(description = "Limit ID", required = true) @PathVariable UUID id) {

        log.info("Deleting velocity limit: {}", id);

        velocityLimitService.deleteVelocityLimit(id);

        log.info("Successfully deleted velocity limit: {}", id);

        return ResponseEntity.ok().build();
    }

    // Customer-Facing Monitoring Endpoints

    @GetMapping("/account/{accountId}/remaining")
    @PreAuthorize("hasAuthority('velocity-limit:read')")
    @Operation(summary = "Get remaining limits", description = "Retrieves remaining headroom for all active limits for an account. Useful for showing customers their remaining daily/monthly limits.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Remaining limits retrieved successfully") })
    public ResponseEntity<List<VelocityLimitResponse>> getRemainingLimits(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Transaction type") @RequestParam TransactionType type) {

        log.info("Fetching remaining limits for account {}, type: {}", accountId, type);

        List<VelocityLimit> limits = velocityLimitService.getRemainingLimits(accountId, type);
        List<VelocityLimitResponse> response = limits.stream().map(velocityLimitMapper::toResponse).toList();

        log.info("Found {} remaining limits for account: {}", response.size(), accountId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId}/status")
    @PreAuthorize("hasAuthority('velocity-limit:read')")
    @Operation(summary = "Get current limit status", description = "Gets the current limit status for an account and transaction type")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Limit status retrieved successfully") })
    public ResponseEntity<VelocityLimitStatus> getCurrentLimitStatus(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Transaction type") @RequestParam TransactionType type) {

        log.info("Fetching limit status for account {}, type: {}", accountId, type);

        VelocityLimitStatus status = velocityLimitService.getCurrentLimitStatus(accountId, type);

        return ResponseEntity.ok(status);
    }

    @GetMapping("/account/{accountId}/remaining/{period}")
    @PreAuthorize("hasAuthority('velocity-limit:read')")
    @Operation(summary = "Get remaining limit amount", description = "Gets the remaining limit amount for a specific period")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Remaining limit retrieved successfully") })
    public ResponseEntity<Map<String, Object>> getRemainingLimit(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Transaction type") @RequestParam TransactionType type,
            @Parameter(description = "Limit period", required = true) @PathVariable VelocityLimitPeriod period) {

        log.info("Fetching remaining limit for account {}, type: {}, period: {}", accountId, type, period);

        BigDecimal remaining = velocityLimitService.getRemainingLimit(accountId, type, period);

        return ResponseEntity
                .ok(Map.of("remainingAmount", remaining != null ? remaining : "unlimited", "period", period));
    }

    @GetMapping("/account/{accountId}/remaining-count/{period}")
    @PreAuthorize("hasAuthority('velocity-limit:read')")
    @Operation(summary = "Get remaining transaction count", description = "Gets the remaining transaction count for a specific period")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Remaining count retrieved successfully") })
    public ResponseEntity<Map<String, Object>> getRemainingTransactionCount(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Transaction type") @RequestParam TransactionType type,
            @Parameter(description = "Limit period", required = true) @PathVariable VelocityLimitPeriod period) {

        log.info("Fetching remaining count for account {}, type: {}, period: {}", accountId, type, period);

        Integer remaining = velocityLimitService.getRemainingTransactionCount(accountId, type, period);

        return ResponseEntity
                .ok(Map.of("remainingCount", remaining != null ? remaining : "unlimited", "period", period));
    }

    @GetMapping("/account/{accountId}/next-reset/{period}")
    @PreAuthorize("hasAuthority('velocity-limit:read')")
    @Operation(summary = "Get next limit reset time", description = "Gets the next limit reset time for a specific period")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Next reset time retrieved successfully") })
    public ResponseEntity<Map<String, Object>> getNextLimitReset(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Transaction type") @RequestParam TransactionType type,
            @Parameter(description = "Limit period", required = true) @PathVariable VelocityLimitPeriod period) {

        log.info("Fetching next reset time for account {}, type: {}, period: {}", accountId, type, period);

        LocalDateTime nextReset = velocityLimitService.getNextLimitReset(accountId, type, period);

        return ResponseEntity
                .ok(Map.of("nextResetAt", nextReset != null ? nextReset : "no limit configured", "period", period));
    }

    @GetMapping("/account/{accountId}/breaches")
    @PreAuthorize("hasAuthority('velocity-limit:read')")
    @Operation(summary = "Get limit breaches", description = "Gets limit breaches for an account within a date range. Useful for administrative review and customer support.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Limit breaches retrieved successfully") })
    public ResponseEntity<List<VelocityLimitBreachDTO>> getLimitBreaches(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Fetching limit breaches for account {} from {} to {}", accountId, startDate, endDate);

        List<VelocityLimitBreachDTO> breaches = velocityLimitService.getLimitBreaches(accountId, startDate, endDate);

        log.info("Found {} limit breaches for account: {}", breaches.size(), accountId);

        return ResponseEntity.ok(breaches);
    }
}
