package com.openfinova.banking.gl.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.gl.dto.ClearSuspenseRequest;
import com.openfinova.banking.gl.dto.CreateSuspenseItemRequest;
import com.openfinova.banking.gl.dto.SuspenseAgingReportDTO;
import com.openfinova.banking.gl.dto.SuspenseItemFilterDTO;
import com.openfinova.banking.gl.dto.SuspenseItemResponse;
import com.openfinova.banking.gl.entity.SuspenseEscalation;
import com.openfinova.banking.gl.service.SuspenseAccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for suspense account management.
 *
 * Provides endpoints for:
 * - Viewing and searching suspense items
 * - Clearing suspense items to target accounts
 * - Aging analysis and reporting
 * - Escalation management
 * - Investigation workflow
 *
 * All DTOs are in the service module per architectural convention.
 */
@RestController
@RequestMapping("/api/v1/gl/suspense")
@Tag(name = "Suspense Account Management", description = "Manage suspense account items and workflow")
public class SuspenseAccountController {

    private final SuspenseAccountService suspenseAccountService;

    public SuspenseAccountController(SuspenseAccountService suspenseAccountService) {
        this.suspenseAccountService = suspenseAccountService;
    }

    /**
     * Get a specific suspense item by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get suspense item", description = "Retrieve a specific suspense item by ID")
    public ResponseEntity<SuspenseItemResponse> getSuspenseItem(
            @Parameter(description = "Suspense item ID") @PathVariable UUID id) {
        SuspenseItemResponse response = suspenseAccountService.getSuspenseItem(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Search and filter suspense items.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Search suspense items", description = "Search and filter suspense items with pagination")
    public ResponseEntity<Page<SuspenseItemResponse>> searchSuspenseItems(
            @Parameter(description = "Filter criteria") @ModelAttribute SuspenseItemFilterDTO filter,
            Pageable pageable) {
        Page<SuspenseItemResponse> items = suspenseAccountService.findSuspenseItems(filter, pageable);
        return ResponseEntity.ok(items);
    }

    /**
     * Create a new suspense item.
     * Typically called internally from Transaction Processing module.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('gl:post')")
    @Operation(summary = "Create suspense item", description = "Create a new suspense item for a GL transaction")
    public ResponseEntity<SuspenseItemResponse> createSuspenseItem(
            @Valid @RequestBody CreateSuspenseItemRequest request) {
        // TODO: Add authorization check - only allow internal service calls
        SuspenseItemResponse response = suspenseAccountService.createSuspenseItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Clear a suspense item to the target account.
     */
    @PostMapping("/{id}/clear")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Clear suspense item", description = "Clear a suspense item to the identified target account")
    public ResponseEntity<SuspenseItemResponse> clearSuspenseItem(
            @Parameter(description = "Suspense item ID") @PathVariable UUID id,
            @Valid @RequestBody ClearSuspenseRequest request) {
        // TODO: Add authorization check - requires appropriate approval level
        SuspenseItemResponse response = suspenseAccountService.clearSuspenseItem(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Start investigation on a suspense item.
     */
    @PostMapping("/{id}/investigate")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Start investigation", description = "Assign a suspense item for investigation")
    public ResponseEntity<SuspenseItemResponse> startInvestigation(
            @Parameter(description = "Suspense item ID") @PathVariable UUID id,
            @Parameter(description = "Investigator username") @RequestParam String investigator) {
        // TODO: Add authorization check
        SuspenseItemResponse response = suspenseAccountService.startInvestigation(id, investigator);
        return ResponseEntity.ok(response);
    }

    // ========== Aging & Analysis ==========

    /**
     * Get aging report for suspense items.
     */
    @GetMapping("/aging-report")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get aging report", description = "Generate aging analysis report for suspense items")
    public ResponseEntity<SuspenseAgingReportDTO> getAgingReport(
            @Parameter(description = "Currency code (optional, null for all currencies)") @RequestParam(required = false) String currency) {
        SuspenseAgingReportDTO report = suspenseAccountService.generateAgingReport(currency);
        return ResponseEntity.ok(report);
    }

    /**
     * Get items older than specified days.
     */
    @GetMapping("/aged")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get aged items", description = "Get suspense items older than specified number of days")
    public ResponseEntity<List<SuspenseItemResponse>> getAgedItems(
            @Parameter(description = "Minimum age in days") @RequestParam int days) {
        List<SuspenseItemResponse> items = suspenseAccountService.getItemsOlderThan(days);
        return ResponseEntity.ok(items);
    }

    /**
     * Get items requiring AML review.
     */
    @GetMapping("/aml-review")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get AML review items", description = "Get suspense items requiring AML/CFT review")
    public ResponseEntity<List<SuspenseItemResponse>> getAMLReviewItems() {
        List<SuspenseItemResponse> items = suspenseAccountService.getItemsRequiringAMLReview();
        return ResponseEntity.ok(items);
    }

    /**
     * Get suspense statistics summary.
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get statistics", description = "Get summary statistics for active suspense items")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = suspenseAccountService.getSuspenseStatistics();
        return ResponseEntity.ok(stats);
    }

    // ========== Escalation Management ==========

    /**
     * Get all unresolved escalations.
     */
    @GetMapping("/escalations")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get escalations", description = "Get all unresolved suspense escalations")
    public ResponseEntity<List<SuspenseEscalation>> getEscalations() {
        List<SuspenseEscalation> escalations = suspenseAccountService.getUnresolvedEscalations();
        return ResponseEntity.ok(escalations);
    }

    /**
     * Get overdue escalations.
     */
    @GetMapping("/escalations/overdue")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get overdue escalations", description = "Get escalations that are past their due date")
    public ResponseEntity<List<SuspenseEscalation>> getOverdueEscalations() {
        List<SuspenseEscalation> escalations = suspenseAccountService.getOverdueEscalations();
        return ResponseEntity.ok(escalations);
    }

    /**
     * Resolve an escalation.
     */
    @PostMapping("/escalations/{escalationId}/resolve")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Resolve escalation", description = "Mark an escalation as resolved")
    public ResponseEntity<Void> resolveEscalation(
            @Parameter(description = "Escalation ID") @PathVariable UUID escalationId,
            @Parameter(description = "User resolving the escalation") @RequestParam String resolvedBy,
            @Parameter(description = "Resolution notes") @RequestParam String resolutionNotes) {
        // TODO: Add authorization check
        suspenseAccountService.resolveEscalation(escalationId, resolvedBy, resolutionNotes);
        return ResponseEntity.ok().build();
    }

    // ========== Manual Trigger Endpoints (Admin Only) ==========

    /**
     * Manually trigger automatic clearing rules.
     * For testing or ad-hoc execution.
     */
    @PostMapping("/admin/trigger-clearing")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Trigger clearing rules", description = "Manually trigger automatic clearing rule application")
    public ResponseEntity<Map<String, Object>> triggerClearingRules() {
        // TODO: Add admin authorization check
        int clearedCount = suspenseAccountService.applyAutomaticClearingRules();
        return ResponseEntity.ok(Map.of("clearedCount", clearedCount));
    }

    /**
     * Manually trigger escalation check.
     * For testing or ad-hoc execution.
     */
    @PostMapping("/admin/trigger-escalation")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Trigger escalation check", description = "Manually trigger escalation threshold processing")
    public ResponseEntity<Map<String, Object>> triggerEscalationCheck() {
        // TODO: Add admin authorization check
        int escalationCount = suspenseAccountService.checkEscalationThresholds();
        return ResponseEntity.ok(Map.of("escalationCount", escalationCount));
    }
}
