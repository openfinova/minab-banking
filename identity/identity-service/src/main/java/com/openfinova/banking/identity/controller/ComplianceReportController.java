package com.openfinova.banking.identity.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.identity.dto.LoginActivityReportRow;
import com.openfinova.banking.identity.dto.PermissionChangeReportRow;
import com.openfinova.banking.identity.dto.SodViolationResponse;
import com.openfinova.banking.identity.dto.UserAccessReportRow;
import com.openfinova.banking.identity.service.ComplianceReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/identity/compliance/reports")
@Tag(name = "Compliance Reports", description = "Regulatory reporting endpoints for user access, permission changes, login activity, "
        + "and Segregation of Duties violations. Requires 'report:generate' authority.")
@SecurityRequirement(name = "oauth2")
@PreAuthorize("hasAuthority('report:generate')")
public class ComplianceReportController {

    private final ComplianceReportService reportService;

    public ComplianceReportController(ComplianceReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/user-access")
    @Operation(summary = "User access report", description = "Returns all users with their currently assigned roles and account status. "
            + "Used for periodic access reviews and regulatory user-access attestation.")
    public ResponseEntity<Page<UserAccessReportRow>> getUserAccessReport(
            @Parameter(description = "Spring Data pagination (page, size, sort)") Pageable pageable) {
        return ResponseEntity.ok(reportService.getUserAccessReport(pageable));
    }

    @GetMapping("/permission-changes")
    @Operation(summary = "Permission change report", description = "Returns role creation, deletion, and permission mutation events within "
            + "the given time window. Covers ROLE_CREATED, ROLE_DELETED, ROLE_PERMISSIONS_CHANGED, "
            + "PERMISSION_ADDED, PERMISSION_REMOVED, ROLE_ASSIGNED, and ROLE_REVOKED events.")
    public ResponseEntity<Page<PermissionChangeReportRow>> getPermissionChangeReport(
            @Parameter(description = "Events from (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Events to (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Spring Data pagination (page, size, sort)") Pageable pageable) {
        return ResponseEntity.ok(reportService.getPermissionChangeReport(from, to, pageable));
    }

    @GetMapping("/login-activity")
    @Operation(summary = "Login activity report", description = "Returns login success and failure events within the given time window. "
            + "Optionally filtered by username.")
    public ResponseEntity<Page<LoginActivityReportRow>> getLoginActivityReport(
            @Parameter(description = "Filter by username (exact match)") @RequestParam(required = false) String username,
            @Parameter(description = "Events from (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Events to (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Spring Data pagination (page, size, sort)") Pageable pageable) {
        return ResponseEntity.ok(reportService.getLoginActivityReport(username, from, to, pageable));
    }

    @GetMapping("/sod-violations")
    @Operation(summary = "Segregation of Duties violation report", description = "Scans all active users and returns any who currently hold two or more roles "
            + "that violate a configured SoD rule. Intended for periodic compliance reviews.")
    public ResponseEntity<List<SodViolationResponse>> getSodViolations() {
        return ResponseEntity.ok(reportService.getSodViolations());
    }
}
