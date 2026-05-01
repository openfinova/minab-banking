package com.openfinova.banking.gl.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.gl.api.dto.GLAuditTrailDTO;
import com.openfinova.banking.gl.api.entity.GLEntityType;
import com.openfinova.banking.gl.entity.GLAuditTrail;
import com.openfinova.banking.gl.mapper.GLAuditTrailMapper;
import com.openfinova.banking.gl.service.AuditQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST API controller for querying audit trail records.
 * Provides endpoints for regulatory reporting, compliance monitoring, and forensic investigations.
 *
 * IMPORTANT: This controller only supports READ operations.
 * Audit records are immutable and cannot be modified or deleted.
 */
@RestController
@RequestMapping("/api/v1/gl/audit")
@Tag(name = "GL Audit Trail", description = "Regulatory audit trail queries for compliance and reporting")
public class AuditController {

    private static final Logger log = LoggerFactory.getLogger(AuditController.class);

    private final AuditQueryService auditQueryService;
    private final GLAuditTrailMapper auditTrailMapper;

    public AuditController(AuditQueryService auditQueryService, GLAuditTrailMapper auditTrailMapper) {
        this.auditQueryService = auditQueryService;
        this.auditTrailMapper = auditTrailMapper;
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get entity change history", description = "Retrieves complete audit trail for a specific GL entity (account, transaction, etc.). "
            + "Returns all changes in chronological order (newest first) for regulatory review.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit trail retrieved successfully", content = @Content(schema = @Schema(implementation = GLAuditTrailDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid entity type or entity ID") })
    public ResponseEntity<List<GLAuditTrailDTO>> getEntityHistory(
            @Parameter(description = "Type of entity (GL_ACCOUNT, GL_TRANSACTION, etc.)", required = true) @PathVariable GLEntityType entityType,
            @Parameter(description = "UUID of the entity", required = true) @PathVariable UUID entityId) {

        log.info("Retrieving audit history for {} {}", entityType, entityId);

        List<GLAuditTrail> auditTrail = auditQueryService.getEntityHistory(entityType, entityId);
        List<GLAuditTrailDTO> response = auditTrailMapper.toDTOList(auditTrail);

        log.debug("Found {} audit records for {} {}", response.size(), entityType, entityId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reversals")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get all transaction reversals in period", description = "Retrieves all transaction reversals within a date range. "
            + "Critical for audit committees and external auditors reviewing exceptions.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Reversals retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date range") })
    public ResponseEntity<List<GLAuditTrailDTO>> getReversals(
            @Parameter(description = "Start date (inclusive)", required = true, example = "2026-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive)", required = true, example = "2026-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Retrieving reversals from {} to {}", startDate, endDate);

        List<GLAuditTrail> reversals = auditQueryService.getReversalsInPeriod(startDate, endDate);
        List<GLAuditTrailDTO> response = auditTrailMapper.toDTOList(reversals);

        log.info("Found {} reversals in period", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{username}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get user activity report", description = "Retrieves all audit trail entries for a specific user within a date range. "
            + "Use for access review, compliance audits, and user activity analysis.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "User activity retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid username or date range") })
    public ResponseEntity<List<GLAuditTrailDTO>> getUserActivity(
            @Parameter(description = "Username to query", required = true, example = "john.doe") @PathVariable String username,
            @Parameter(description = "Start date (inclusive)", required = true, example = "2026-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive)", required = true, example = "2026-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Retrieving activity for user {} from {} to {}", username, startDate, endDate);

        List<GLAuditTrail> activity = auditQueryService.getUserActivityReport(username, startDate, endDate);
        List<GLAuditTrailDTO> response = auditTrailMapper.toDTOList(activity);

        log.debug("Found {} activity records for user {}", response.size(), username);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/high-risk")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get high-risk actions report", description = "Retrieves all high-risk actions (REVERSE, DELETE, PERIOD_REOPEN, BALANCE_ADJUSTMENT) "
            + "within a date range for compliance monitoring and management review.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "High-risk actions retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date range") })
    public ResponseEntity<List<GLAuditTrailDTO>> getHighRiskActions(
            @Parameter(description = "Start date (inclusive)", required = true, example = "2026-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive)", required = true, example = "2026-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Retrieving high-risk actions from {} to {}", startDate, endDate);

        List<GLAuditTrail> highRiskActions = auditQueryService.getHighRiskActions(startDate, endDate);
        List<GLAuditTrailDTO> response = auditTrailMapper.toDTOList(highRiskActions);

        log.info("Found {} high-risk actions in period", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/compliance-violations")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get compliance violations (missing required reason)", description = "Retrieves all audit records where a mandatory reason is missing or insufficient. "
            + "Use for compliance violation detection and remediation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compliance violations retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date range") })
    public ResponseEntity<List<GLAuditTrailDTO>> getComplianceViolations(
            @Parameter(description = "Start date (inclusive)", required = true, example = "2026-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive)", required = true, example = "2026-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.warn("Querying for compliance violations from {} to {}", startDate, endDate);

        List<GLAuditTrail> violations = auditQueryService.getChangesWithoutReason(startDate, endDate);
        List<GLAuditTrailDTO> response = auditTrailMapper.toDTOList(violations);

        if (!response.isEmpty()) {
            log.warn("Found {} compliance violations (missing required reason)", response.size());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/large-amounts")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get large amount changes for materiality analysis", description = "Retrieves all audit records involving transaction amounts greater than threshold. "
            + "Use for materiality analysis (e.g., 'all reversals > $1M in last 30 days').")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Large amount changes retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid threshold or date range") })
    public ResponseEntity<List<GLAuditTrailDTO>> getLargeAmountChanges(
            @Parameter(description = "Minimum transaction amount threshold", required = true, example = "1000000.00") @RequestParam BigDecimal threshold,
            @Parameter(description = "Start date (inclusive)", required = true, example = "2026-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive)", required = true, example = "2026-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Retrieving changes with amount > {} from {} to {}", threshold, startDate, endDate);

        List<GLAuditTrail> largeChanges = auditQueryService.getLargeAmountChanges(threshold, startDate, endDate);
        List<GLAuditTrailDTO> response = auditTrailMapper.toDTOList(largeChanges);

        log.info("Found {} changes with amount > {}", response.size(), threshold);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/correlation/{correlationId}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get correlated audit trail for multi-step operations", description = "Retrieves all audit records linked by correlation ID. "
            + "Use to trace complete business operations spanning multiple entity changes "
            + "(e.g., all changes during a period close).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Correlated audit trail retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid correlation ID") })
    public ResponseEntity<List<GLAuditTrailDTO>> getCorrelatedAuditTrail(
            @Parameter(description = "Correlation ID linking related audit entries", required = true) @PathVariable UUID correlationId) {

        log.info("Retrieving correlated audit trail for correlation ID {}", correlationId);

        List<GLAuditTrail> correlatedTrail = auditQueryService.getCorrelatedAuditTrail(correlationId);
        List<GLAuditTrailDTO> response = auditTrailMapper.toDTOList(correlatedTrail);

        log.debug("Found {} correlated audit records", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get recent changes for a specific date", description = "Retrieves all audit trail entries for a specific date. "
            + "Use for daily reconciliation and operational monitoring.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Recent changes retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date") })
    public ResponseEntity<List<GLAuditTrailDTO>> getRecentChanges(
            @Parameter(description = "Date to query", required = true, example = "2026-02-17") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Retrieving changes for date {}", date);

        List<GLAuditTrail> changes = auditQueryService.getRecentChanges(date);
        List<GLAuditTrailDTO> response = auditTrailMapper.toDTOList(changes);

        log.debug("Found {} changes for date {}", response.size(), date);
        return ResponseEntity.ok(response);
    }
}
