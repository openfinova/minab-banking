package com.openfinova.banking.tp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.tp.api.dto.CreateFeeRuleRequest;
import com.openfinova.banking.tp.api.dto.CreateFeeWaiverRequest;
import com.openfinova.banking.tp.api.dto.FeeRuleResponse;
import com.openfinova.banking.tp.api.dto.FeeWaiverResponse;
import com.openfinova.banking.tp.api.dto.UpdateFeeRuleRequest;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.entity.FeeRule;
import com.openfinova.banking.tp.entity.FeeWaiver;
import com.openfinova.banking.tp.mapper.FeeRuleMapper;
import com.openfinova.banking.tp.mapper.FeeWaiverMapper;
import com.openfinova.banking.tp.service.FeeManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for fee rule configuration and waiver management.
 *
 * NOTE: This controller exposes only administrative and configuration endpoints.
 * Internal fee calculation methods (calculateFees, applyWaivers, evaluateTierEligibility, etc.)
 * are called internally by TransactionService and should NOT be exposed via REST API.
 *
 * Administrative endpoints:
 * - Fee Rule CRUD operations
 * - Fee Waiver management
 * - Viewing active rules and waivers
 */
@RestController
@RequestMapping("/api/v1/fees")
@Tag(name = "Fee Management", description = "Administrative APIs for managing transaction fees and waivers")
public class FeeManagementController {

    private static final Logger log = LoggerFactory.getLogger(FeeManagementController.class);

    private final FeeManagementService feeManagementService;
    private final FeeRuleMapper feeRuleMapper;
    private final FeeWaiverMapper feeWaiverMapper;

    public FeeManagementController(FeeManagementService feeManagementService, FeeRuleMapper feeRuleMapper,
            FeeWaiverMapper feeWaiverMapper) {
        this.feeManagementService = feeManagementService;
        this.feeRuleMapper = feeRuleMapper;
        this.feeWaiverMapper = feeWaiverMapper;
    }

    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('fee:read')")
    @Operation(summary = "Get active fee rules", description = "Retrieves all currently active fee rules for administrative review")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Fee rules retrieved successfully") })
    public ResponseEntity<List<FeeRuleResponse>> getActiveFeeRules() {

        log.info("Fetching active fee rules");

        List<FeeRule> rules = feeManagementService.getActiveFeeRules();
        List<FeeRuleResponse> response = rules.stream().map(feeRuleMapper::toResponse).toList();

        log.info("Found {} active fee rules", response.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/rules/type/{type}")
    @PreAuthorize("hasAuthority('fee:read')")
    @Operation(summary = "Get fee rules by transaction type", description = "Retrieves fee rules for a specific transaction type")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Fee rules retrieved successfully") })
    public ResponseEntity<List<FeeRuleResponse>> getFeeRulesByType(
            @Parameter(description = "Transaction type", required = true) @PathVariable TransactionType type) {

        log.info("Fetching fee rules for type: {}", type);

        List<FeeRule> rules = feeManagementService.getFeeRulesByType(type);
        List<FeeRuleResponse> response = rules.stream().map(feeRuleMapper::toResponse).toList();

        log.info("Found {} fee rules for type: {}", response.size(), type);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAuthority('admin:config:write')")
    @Operation(summary = "Create fee rule", description = "Creates a new fee rule with validation")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Fee rule created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid fee rule data") })
    public ResponseEntity<FeeRuleResponse> createFeeRule(@Valid @RequestBody CreateFeeRuleRequest request) {

        log.info("Creating fee rule for type: {}, tier: {}", request.getTransactionType(), request.getCustomerTier());

        FeeRule rule = feeRuleMapper.toEntity(request);
        FeeRule created = feeManagementService.createFeeRule(rule);

        log.info("Successfully created fee rule with ID: {}", created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(feeRuleMapper.toResponse(created));
    }

    @PutMapping("/rules/{ruleId}")
    @PreAuthorize("hasAuthority('admin:config:write')")
    @Operation(summary = "Update fee rule", description = "Updates an existing fee rule")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Fee rule updated successfully"),
            @ApiResponse(responseCode = "404", description = "Fee rule not found"),
            @ApiResponse(responseCode = "400", description = "Invalid fee rule data") })
    public ResponseEntity<FeeRuleResponse> updateFeeRule(
            @Parameter(description = "Rule ID", required = true) @PathVariable UUID ruleId,
            @Valid @RequestBody UpdateFeeRuleRequest request) {

        log.info("Updating fee rule: {}", ruleId);

        FeeRule updatedRule = feeRuleMapper.toEntity(request);
        FeeRule updated = feeManagementService.updateFeeRule(ruleId, updatedRule);

        log.info("Successfully updated fee rule: {}", ruleId);

        return ResponseEntity.ok(feeRuleMapper.toResponse(updated));
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("hasAuthority('admin:config:write')")
    @Operation(summary = "Delete fee rule", description = "Deletes a fee rule by ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Fee rule deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Fee rule not found") })
    public ResponseEntity<Void> deleteFeeRule(
            @Parameter(description = "Rule ID", required = true) @PathVariable UUID ruleId) {

        log.info("Deleting fee rule: {}", ruleId);

        feeManagementService.deleteFeeRule(ruleId);

        log.info("Successfully deleted fee rule: {}", ruleId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/waivers")
    @PreAuthorize("hasAuthority('admin:config:write')")
    @Operation(summary = "Create fee waiver", description = "Creates a new fee waiver for a customer")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Fee waiver created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid waiver data") })
    public ResponseEntity<FeeWaiverResponse> createFeeWaiver(@Valid @RequestBody CreateFeeWaiverRequest request) {

        log.info("Creating fee waiver for customer: {}", request.getCustomerId());

        FeeWaiver created = feeManagementService.createFeeWaiver(feeWaiverMapper.toEntity(request));

        log.info("Successfully created fee waiver with ID: {}", created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(feeWaiverMapper.toResponse(created));
    }

    @GetMapping("/waivers/customer/{customerId}")
    @PreAuthorize("hasAuthority('fee:read')")
    @Operation(summary = "Get active fee waivers", description = "Retrieves active fee waivers for a specific customer")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Fee waivers retrieved successfully") })
    public ResponseEntity<List<FeeWaiverResponse>> getActiveFeeWaivers(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId) {

        log.info("Fetching active fee waivers for customer: {}", customerId);

        List<FeeWaiver> waivers = feeManagementService.getActiveFeeWaivers(customerId);

        log.info("Found {} active fee waivers for customer: {}", waivers.size(), customerId);

        List<FeeWaiverResponse> responses = waivers.stream().map(feeWaiverMapper::toResponse).toList();

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/waiver-campaigns")
    @PreAuthorize("hasAuthority('fee:campaign:write')")
    @Operation(summary = "Register bulk waiver campaign (stub)", description = "Accepts campaign metadata — expansion job is backlog work for S3")
    public ResponseEntity<Map<String, Object>> registerWaiverCampaign(@RequestBody Map<String, Object> body) {

        Map<String, Object> out = new HashMap<>();
        out.put("campaignId", UUID.randomUUID().toString());
        out.put("payloadEcho", body);
        out.put("status", "ACCEPTED_FOR_PROCESSING");

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(out);
    }
}
