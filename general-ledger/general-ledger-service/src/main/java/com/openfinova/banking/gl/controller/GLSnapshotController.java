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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.gl.api.dto.AccountReconciliationResult;
import com.openfinova.banking.gl.api.dto.BalanceReconciliationReport;
import com.openfinova.banking.gl.api.dto.SnapshotsComplianceReport;
import com.openfinova.banking.gl.api.dto.ValidationResult;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import com.openfinova.banking.gl.dto.RepairResult;
import com.openfinova.banking.gl.dto.SnapshotRecoveryResult;
import com.openfinova.banking.gl.dto.SnapshotStatistics;
import com.openfinova.banking.gl.service.GLSnapshotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/gl/snapshots")
@Tag(name = "GL Snapshot & Maintenance", description = "APIs for snapshot generation, reconciliation, and system maintenance")
public class GLSnapshotController {

    private static final Logger log = LoggerFactory.getLogger(GLSnapshotController.class);

    private final GLSnapshotService snapshotService;

    public GLSnapshotController(GLSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @PostMapping("/daily")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Generate daily snapshots", description = "Generates daily balance snapshots for all accounts")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Snapshots generated successfully") })
    public ResponseEntity<Map<String, String>> generateDailySnapshots() {
        log.info("Generating daily snapshots for all accounts");

        snapshotService.generateDailySnapshots();
        log.info("Successfully generated daily snapshots");

        return ResponseEntity.ok(Map.of("message", "Daily snapshots generated successfully"));
    }

    @PostMapping("/daily/{date}")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Generate snapshots for date", description = "Generates daily balance snapshots for a specific date")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Snapshots generated successfully") })
    public ResponseEntity<Map<String, Object>> generateDailySnapshotsForDate(
            @Parameter(description = "Date for snapshot generation", required = true, example = "2024-01-15") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Generating daily snapshots for date: {}", date);

        snapshotService.generateDailySnapshots(date);
        log.info("Successfully generated daily snapshots for date: {}", date);

        return ResponseEntity.ok(Map.of("message", "Daily snapshots generated successfully", "date", date));
    }

    @PostMapping("/weekly")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Generate weekly snapshots", description = "Generates weekly balance aggregations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Weekly snapshots generated successfully") })
    public ResponseEntity<Map<String, String>> generateWeeklySnapshots() {
        log.info("Generating weekly snapshots");

        snapshotService.generateWeeklySnapshots();
        log.info("Successfully generated weekly snapshots");

        return ResponseEntity.ok(Map.of("message", "Weekly snapshots generated successfully"));
    }

    @PostMapping("/monthly")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Generate monthly snapshots", description = "Generates monthly balance aggregations for fiscal reporting")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Monthly snapshots generated successfully") })
    public ResponseEntity<Map<String, String>> generateMonthlySnapshots() {
        log.info("Generating monthly snapshots");

        snapshotService.generateMonthlySnapshots();
        log.info("Successfully generated monthly snapshots");

        return ResponseEntity.ok(Map.of("message", "Monthly snapshots generated successfully"));
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Generate snapshots for specific accounts", description = "Generates snapshots for a list of accounts on a specific date")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Snapshots generated successfully") })
    public ResponseEntity<Map<String, Object>> generateSnapshotsForAccounts(
            @Parameter(description = "List of account IDs", required = true) @RequestParam List<UUID> accountIds,
            @Parameter(description = "Date for snapshot generation", required = true, example = "2024-01-15") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Generating snapshots for {} accounts on date: {}", accountIds.size(), date);

        int count = snapshotService.generateSnapshotsForAccounts(accountIds, date);
        log.info("Generated {} snapshots for specified accounts", count);

        return ResponseEntity.ok(Map.of("message", "Snapshots generated successfully", "count", count, "date", date));
    }

    @PostMapping("/account-type/{accountType}")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Generate snapshots by account type", description = "Generates snapshots for all accounts of a specific type")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Snapshots generated successfully") })
    public ResponseEntity<Map<String, Object>> generateSnapshotsForAccountType(
            @Parameter(description = "Account type", required = true) @PathVariable GLAccountType accountType,
            @Parameter(description = "Date for snapshot generation", required = true, example = "2024-01-15") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Generating snapshots for account type: {} on date: {}", accountType, date);

        int count = snapshotService.generateSnapshotsForAccountType(accountType, date);
        log.info("Generated {} snapshots for account type: {}", count, accountType);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Snapshots generated successfully",
                        "accountType",
                        accountType,
                        "count",
                        count,
                        "date",
                        date));
    }

    @PostMapping("/missing/{date}")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Generate missing snapshots", description = "Identifies and generates missing snapshots for a specific date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Missing snapshots generated successfully") })
    public ResponseEntity<Map<String, Object>> generateMissingSnapshots(
            @Parameter(description = "Date to check for missing snapshots", required = true, example = "2024-01-15") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Generating missing snapshots for date: {}", date);

        int count = snapshotService.generateMissingSnapshots(date);
        log.info("Generated {} missing snapshots for date: {}", count, date);

        return ResponseEntity
                .ok(Map.of("message", "Missing snapshots generated successfully", "count", count, "date", date));
    }

    @DeleteMapping("/cleanup")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Cleanup old snapshots", description = "Removes old snapshots beyond the retention period")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Cleanup completed successfully") })
    public ResponseEntity<Map<String, Object>> cleanupOldSnapshots(
            @Parameter(description = "Number of days to retain snapshots", required = true, example = "365") @RequestParam int retentionDays) {

        log.info("Cleaning up snapshots older than {} days", retentionDays);

        int count = snapshotService.cleanupOldSnapshots(retentionDays);
        log.info("Deleted {} old snapshots", count);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Old snapshots cleaned up successfully",
                        "deletedCount",
                        count,
                        "retentionDays",
                        retentionDays));
    }

    @PostMapping("/rebuild")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Rebuild snapshots for period", description = "Rebuilds snapshots for a specific period (recovery operation)")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Snapshots rebuilt successfully") })
    public ResponseEntity<Map<String, Object>> rebuildSnapshotsForPeriod(
            @Parameter(description = "Start date", required = true, example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date", required = true, example = "2024-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Rebuilding snapshots for period: {} to {}", startDate, endDate);

        int count = snapshotService.rebuildSnapshotsForPeriod(startDate, endDate);
        log.info("Rebuilt {} snapshots for period", count);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Snapshots rebuilt successfully",
                        "count",
                        count,
                        "startDate",
                        startDate,
                        "endDate",
                        endDate));
    }

    @GetMapping("/statistics/{date}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get snapshot statistics", description = "Retrieves snapshot coverage statistics for a specific date")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully") })
    public ResponseEntity<SnapshotStatistics> getSnapshotStatistics(
            @Parameter(description = "Date to analyze", required = true, example = "2024-01-15") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Fetching snapshot statistics for date: {}", date);

        SnapshotStatistics stats = snapshotService.getSnapshotStatistics(date);
        log.info("Retrieved snapshot statistics for date: {}", date);

        return ResponseEntity.ok(stats);
    }

    @PostMapping("/reconciliation")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Perform balance reconciliation", description = "Performs comprehensive balance reconciliation between snapshots and transactions")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Reconciliation completed successfully") })
    public ResponseEntity<BalanceReconciliationReport> performBalanceReconciliation() {
        log.info("Performing balance reconciliation");

        BalanceReconciliationReport report = snapshotService.performBalanceReconciliation();
        log.info(
                "Balance reconciliation completed: {} accounts checked, {} inconsistencies found",
                report.getTotalAccountsChecked(),
                report.getInconsistentAccounts());

        return ResponseEntity.ok(report);
    }

    @PostMapping("/reconciliation/account/{accountId}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Reconcile specific account", description = "Performs balance reconciliation for a specific account and date")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Reconciliation completed successfully") })
    public ResponseEntity<AccountReconciliationResult> performBalanceReconciliationForAccount(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Date for reconciliation", required = true, example = "2024-01-15") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Performing balance reconciliation for account: {} on date: {}", accountId, date);

        AccountReconciliationResult result = snapshotService.performBalanceReconciliationForAccount(accountId, date);
        log.info("Reconciliation for account {}: {}", accountId, result.isConsistent() ? "CONSISTENT" : "INCONSISTENT");

        return ResponseEntity.ok(result);
    }

    @PostMapping("/reconciliation/period")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Reconcile period", description = "Performs balance reconciliation for a specific period")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Reconciliation completed successfully") })
    public ResponseEntity<BalanceReconciliationReport> performBalanceReconciliationForPeriod(
            @Parameter(description = "Start date", required = true, example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date", required = true, example = "2024-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Performing balance reconciliation for period: {} to {}", startDate, endDate);

        BalanceReconciliationReport report = snapshotService.performBalanceReconciliationForPeriod(startDate, endDate);
        log.info(
                "Period reconciliation completed: {} accounts checked, {} inconsistencies found",
                report.getTotalAccountsChecked(),
                report.getInconsistentAccounts());

        return ResponseEntity.ok(report);
    }

    @PostMapping("/validation/data-integrity")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Validate data integrity", description = "Performs comprehensive data integrity validation")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Validation completed successfully") })
    public ResponseEntity<Map<String, String>> performDataIntegrityValidation() {
        log.info("Performing data integrity validation");

        snapshotService.performDataIntegrityValidation();
        log.info("Data integrity validation completed");

        return ResponseEntity.ok(Map.of("message", "Data integrity validation completed successfully"));
    }

    @PostMapping("/validation/audit-trail")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Validate audit trail", description = "Validates audit trail for gaps in sequential transaction numbers")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Validation completed successfully") })
    public ResponseEntity<Map<String, String>> performAuditTrailValidation() {
        log.info("Performing audit trail validation");

        snapshotService.performAuditTrailValidation();
        log.info("Audit trail validation completed");

        return ResponseEntity.ok(Map.of("message", "Audit trail validation completed successfully"));
    }

    @PostMapping("/validation/sequential-numbers")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Validate sequential numbers", description = "Validates sequential transaction number integrity")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Validation completed successfully") })
    public ResponseEntity<ValidationResult> performSequentialNumberValidation() {
        log.info("Performing sequential number validation");

        ValidationResult result = snapshotService.performSequentialNumberValidation();
        log.info("Sequential number validation completed: {}", result.isValid() ? "VALID" : "INVALID");

        return ResponseEntity.ok(result);
    }

    @GetMapping("/validation/snapshot-integrity/{date}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Validate snapshot integrity", description = "Validates snapshot data integrity for a specific date")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Validation completed successfully") })
    public ResponseEntity<ValidationResult> validateSnapshotIntegrity(
            @Parameter(description = "Date to validate", required = true, example = "2024-01-15") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Validating snapshot integrity for date: {}", date);

        ValidationResult result = snapshotService.validateSnapshotIntegrity(date);
        log.info("Snapshot integrity validation for {}: {}", date, result.isValid() ? "VALID" : "INVALID");

        return ResponseEntity.ok(result);
    }

    @PostMapping("/repair")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Validate and repair snapshots", description = "Validates and repairs snapshot data for a period")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Repair completed successfully") })
    public ResponseEntity<RepairResult> validateAndRepairSnapshots(
            @Parameter(description = "Start date", required = true, example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date", required = true, example = "2024-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Validating and repairing snapshots for period: {} to {}", startDate, endDate);

        RepairResult result = snapshotService.validateAndRepairSnapshots(startDate, endDate);
        log.info(
                "Snapshot repair completed: {} repaired, {} recreated",
                result.getSnapshotsRepaired(),
                result.getSnapshotsRecreated());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/compliance-report")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Generate compliance report", description = "Generates compliance report for regulatory requirements")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Report generated successfully") })
    public ResponseEntity<SnapshotsComplianceReport> generateComplianceReport(
            @Parameter(description = "Start date", required = true, example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date", required = true, example = "2024-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Generating compliance report for period: {} to {}", startDate, endDate);

        SnapshotsComplianceReport report = snapshotService.generateComplianceReport(startDate, endDate);
        log.info("Compliance report generated: {}", report.isCompliant() ? "COMPLIANT" : "NON-COMPLIANT");

        return ResponseEntity.ok(report);
    }

    @PostMapping("/recover/{date}")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Recover from failed snapshot", description = "Recovers from failed snapshot operations")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Recovery completed") })
    public ResponseEntity<SnapshotRecoveryResult> recoverFromFailedSnapshot(
            @Parameter(description = "Date of failed snapshot", required = true, example = "2024-01-15") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Recovering from failed snapshot for date: {}", date);

        SnapshotRecoveryResult result = snapshotService.recoverFromFailedSnapshot(date);
        log.info("Snapshot recovery for {}: {}", date, result.isSuccessful() ? "SUCCESSFUL" : "FAILED");

        return ResponseEntity.ok(result);
    }

    @PostMapping("/archive")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Archive old snapshots", description = "Archives old snapshots to separate storage before deletion")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Archival completed successfully") })
    public ResponseEntity<Map<String, Object>> archiveOldSnapshots(
            @Parameter(description = "Archive snapshots before this date", required = true, example = "2023-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beforeDate) {

        log.info("Archiving snapshots before date: {}", beforeDate);

        int count = snapshotService.archiveOldSnapshots(beforeDate);
        log.info("Archived {} snapshots", count);

        return ResponseEntity
                .ok(Map.of("message", "Snapshots archived successfully", "count", count, "beforeDate", beforeDate));
    }
}
