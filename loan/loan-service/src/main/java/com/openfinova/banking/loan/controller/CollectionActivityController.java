package com.openfinova.banking.loan.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.loan.api.dto.CollectionActivityCompleteRequest;
import com.openfinova.banking.loan.api.dto.CollectionActivityFollowUpRequest;
import com.openfinova.banking.loan.api.dto.CollectionActivityReportResponse;
import com.openfinova.banking.loan.api.dto.CollectionActivityRequest;
import com.openfinova.banking.loan.api.dto.CollectionActivityResponse;
import com.openfinova.banking.loan.api.dto.CollectionActivityStatusUpdateRequest;
import com.openfinova.banking.loan.api.dto.CollectionActivityUpdateRequest;
import com.openfinova.banking.loan.api.entity.CollectionStatus;
import com.openfinova.banking.loan.entity.CollectionActivity;
import com.openfinova.banking.loan.mapper.CollectionActivityMapper;
import com.openfinova.banking.loan.service.CollectionActivityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for managing loan collection activities.
 * Provides operations for delinquency management and collections workflow.
 */
@RestController
@RequestMapping("/api/v1/loan-accounts/{loanAccountId}/collections")
@Tag(name = "Loan Collections", description = "Loan collection activity management APIs")
public class CollectionActivityController {

    private final CollectionActivityService collectionActivityService;

    public CollectionActivityController(CollectionActivityService collectionActivityService) {
        this.collectionActivityService = collectionActivityService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('loan:collect')")
    @Operation(summary = "Create a collection activity")
    public ResponseEntity<CollectionActivityResponse> createCollectionActivity(@PathVariable UUID loanAccountId,
            @Valid @RequestBody CollectionActivityRequest request) {

        CollectionActivity created = collectionActivityService.createActivity(
                loanAccountId,
                request.getActivityType(),
                request.getActivityDate(),
                request.getNotes(),
                request.getFollowUpDate(),
                "TODO_CURRENT_USER");

        return ResponseEntity.status(HttpStatus.CREATED).body(CollectionActivityMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get collection activity by ID")
    public ResponseEntity<CollectionActivityResponse> getActivityById(@PathVariable UUID loanAccountId,
            @PathVariable UUID id) {

        return collectionActivityService.getActivityForLoanAccount(loanAccountId, id)
                .map(CollectionActivityMapper::toResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all collection activities for a loan account")
    public ResponseEntity<List<CollectionActivityResponse>> getActivitiesByLoanAccount(
            @PathVariable UUID loanAccountId) {

        List<CollectionActivity> activities = collectionActivityService.getActivitiesByLoanAccount(loanAccountId);
        return ResponseEntity.ok(activities.stream().map(CollectionActivityMapper::toResponse).toList());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get collection activities by status")
    public ResponseEntity<Page<CollectionActivityResponse>> getActivitiesByStatus(@PathVariable UUID loanAccountId,
            @PathVariable CollectionStatus status, Pageable pageable) {

        Page<CollectionActivity> activities = collectionActivityService
                .getActivitiesByLoanAccountAndStatus(loanAccountId, status, pageable);
        return ResponseEntity.ok(activities.map(CollectionActivityMapper::toResponse));
    }

    @GetMapping("/by-date-range")
    @Operation(summary = "Get collection activities between dates")
    public ResponseEntity<Page<CollectionActivityResponse>> getActivitiesByDateRange(@PathVariable UUID loanAccountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, Pageable pageable) {

        Page<CollectionActivity> activities = collectionActivityService
                .getActivitiesByLoanAccountAndDateRange(loanAccountId, startDate, endDate, pageable);
        return ResponseEntity.ok(activities.map(CollectionActivityMapper::toResponse));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('loan:collect')")
    @Operation(summary = "Update collection activity")
    public ResponseEntity<CollectionActivityResponse> updateActivity(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody CollectionActivityUpdateRequest request) {

        CollectionActivity activity = collectionActivityService
                .updateActivity(id, request.getNotes(), request.getFollowUpDate(), request.getUpdatedBy());

        return ResponseEntity.ok(CollectionActivityMapper.toResponse(activity));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('loan:collect')")
    @Operation(summary = "Update activity status")
    public ResponseEntity<CollectionActivityResponse> updateActivityStatus(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody CollectionActivityStatusUpdateRequest request) {

        CollectionActivity activity = collectionActivityService
                .updateActivityStatusForLoanAccount(loanAccountId, id, request.getNewStatus(), request.getUpdatedBy());
        return ResponseEntity.ok(CollectionActivityMapper.toResponse(activity));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('loan:collect:approve')")
    @Operation(summary = "Mark collection activity as complete")
    public ResponseEntity<CollectionActivityResponse> completeActivity(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody CollectionActivityCompleteRequest request) {

        CollectionActivity activity = collectionActivityService
                .completeActivity(id, request.getOutcome(), request.getCompletedBy());
        return ResponseEntity.ok(CollectionActivityMapper.toResponse(activity));
    }

    @PostMapping("/{id}/schedule-followup")
    @PreAuthorize("hasAuthority('loan:collect')")
    @Operation(summary = "Schedule follow-up for collection activity")
    public ResponseEntity<CollectionActivityResponse> scheduleFollowUp(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody CollectionActivityFollowUpRequest request) {

        CollectionActivity activity = collectionActivityService.scheduleFollowUpForLoanAccount(
                loanAccountId,
                id,
                request.getFollowUpDate(),
                request.getFollowUpType(),
                request.getScheduledBy());

        return ResponseEntity.ok(CollectionActivityMapper.toResponse(activity));
    }

    @GetMapping("/report")
    @Operation(summary = "Generate collection activity report for a period")
    public ResponseEntity<CollectionActivityReportResponse> generateReport(@PathVariable UUID loanAccountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        CollectionActivityReportResponse report = collectionActivityService
                .generateActivityReport(loanAccountId, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/pending/count")
    @Operation(summary = "Count pending collection activities for this loan account")
    public ResponseEntity<Long> countPendingActivities(@PathVariable UUID loanAccountId) {
        return ResponseEntity.ok(collectionActivityService.countPendingActivitiesForLoanAccount(loanAccountId));
    }
}
