package com.openfinova.banking.gl.controller;

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

import com.openfinova.banking.gl.dto.RevaluationDetailResponse;
import com.openfinova.banking.gl.dto.RevaluationRunResponse;
import com.openfinova.banking.gl.mapper.RevaluationMapper;
import com.openfinova.banking.gl.service.RevaluationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for FX revaluation operations.
 *
 * POST /api/gl/revaluation — trigger an ad-hoc revaluation run
 * GET /api/gl/revaluation/runs — list all runs (optionally filtered by date range)
 * GET /api/gl/revaluation/runs/{id} — retrieve a single run summary
 * GET /api/gl/revaluation/runs/{id}/details — per-account detail lines for a run
 */
@RestController
@RequestMapping("/api/v1/gl/revaluation")
@Tag(name = "FX Revaluation", description = "Trigger and inspect foreign-currency revaluation runs")
public class RevaluationController {

    private static final Logger log = LoggerFactory.getLogger(RevaluationController.class);

    private final RevaluationService revaluationService;
    private final RevaluationMapper revaluationMapper;

    public RevaluationController(RevaluationService revaluationService, RevaluationMapper revaluationMapper) {
        this.revaluationService = revaluationService;
        this.revaluationMapper = revaluationMapper;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Trigger ad-hoc revaluation", description = "Revalues all active foreign-currency GL accounts as of the supplied date. "
            + "Fails atomically — if any account cannot be revalued no entries are posted.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Revaluation completed successfully"),
            @ApiResponse(responseCode = "400", description = "Revaluation failed — see message for details"),
            @ApiResponse(responseCode = "422", description = "Invalid request parameters") })
    public ResponseEntity<Map<String, Object>> triggerRevaluation(
            @Parameter(description = "As-of date for revaluation (ISO 8601)", required = true, example = "2026-02-28") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @Parameter(description = "Username initiating the run", example = "treasury-ops") @RequestParam(required = false, defaultValue = "system") String executedBy) {

        log.info("Manual revaluation requested: asOfDate={} executedBy={}", asOfDate, executedBy);

        var run = revaluationService.performRevaluation(asOfDate, "MANUAL", executedBy);

        log.info(
                "Revaluation completed: runId={} processed={} revalued={} adjustment={}",
                run.getId(),
                run.getAccountsProcessed(),
                run.getAccountsRevalued(),
                run.getTotalAdjustment());

        return ResponseEntity.ok(
                Map.of(
                        "revaluationRunId",
                        run.getId(),
                        "asOfDate",
                        run.getRevaluationDate().toString(),
                        "baseCurrency",
                        run.getBaseCurrency(),
                        "accountsProcessed",
                        run.getAccountsProcessed(),
                        "accountsRevalued",
                        run.getAccountsRevalued(),
                        "accountsFailed",
                        run.getAccountsFailed(),
                        "totalAdjustment",
                        run.getTotalAdjustment(),
                        "executedBy",
                        run.getExecutedBy(),
                        "executedAt",
                        run.getExecutedAt().toString()));
    }

    @GetMapping("/runs")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "List revaluation runs", description = "Returns all revaluation runs, optionally filtered to a date range. "
            + "Results are ordered most-recent first.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Run list returned") })
    public ResponseEntity<List<RevaluationRunResponse>> listRuns(
            @Parameter(description = "Filter: runs on or after this date (ISO 8601)", example = "2026-01-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Filter: runs on or before this date (ISO 8601)", example = "2026-12-31") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(revaluationMapper.toRunResponseList(revaluationService.listRevaluationRuns(from, to)));
    }

    @GetMapping("/runs/{id}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get a revaluation run by ID", description = "Returns the summary (processed/revalued/failed counts, total adjustment) for a single run.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Run found"),
            @ApiResponse(responseCode = "404", description = "Run not found") })
    public ResponseEntity<RevaluationRunResponse> getRun(
            @Parameter(description = "Revaluation run UUID", required = true) @PathVariable UUID id) {

        return revaluationService.getRevaluationRun(id).map(revaluationMapper::toRunResponse).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/runs/{id}/details")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get per-account detail lines for a revaluation run", description = "Returns the individual account adjustments (old rate, new rate, gain/loss) "
            + "for every account processed in the specified run.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Details returned"),
            @ApiResponse(responseCode = "404", description = "Run not found") })
    public ResponseEntity<List<RevaluationDetailResponse>> getRunDetails(
            @Parameter(description = "Revaluation run UUID", required = true) @PathVariable UUID id) {

        try {
            return ResponseEntity
                    .ok(revaluationMapper.toDetailResponseList(revaluationService.getRevaluationRunDetails(id)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
