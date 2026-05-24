package com.openfinova.banking.identity.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.identity.dto.SecurityAuditEventResponse;
import com.openfinova.banking.identity.dto.UserResponse;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.service.SecurityAuditService;
import com.openfinova.banking.identity.service.UserManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/identity/audit")
@Tag(name = "Security Audit", description = """
        Cross-user security audit trail (login success/failure, MFA, lockout, password changes, etc.). \
        Requires `audit:read`. End-users scoped to their own events should use \
        `GET /api/v1/identity/me/audit-events` instead.""")
@SecurityRequirement(name = "oauth2")
@PreAuthorize("hasAuthority('audit:read')")
public class SecurityAuditController {

    private final SecurityAuditService auditService;
    private final UserManagementService userManagementService;

    public SecurityAuditController(SecurityAuditService auditService, UserManagementService userManagementService) {
        this.auditService = auditService;
        this.userManagementService = userManagementService;
    }

    @GetMapping("/event-types")
    @Operation(summary = "Security audit event type literals for filters")
    public List<String> listSecurityAuditEventTypes() {
        return Arrays.stream(SecurityAuditEventType.values()).map(Enum::name).toList();
    }

    @GetMapping("/suggestions/users")
    @Operation(summary = "User typeahead for audit filters", description = "All user types; same text matching as admin user search (q). Requires audit:read.")
    public List<UserResponse> userSuggestions(@RequestParam String q,
            @RequestParam(required = false, defaultValue = "15") int limit) {
        return userManagementService.suggestUsersForLookup(q, limit);
    }

    @GetMapping("/events")
    @Operation(summary = "Search security audit events", description = "Supports filtering by user id, username, event type, IP, and time range, with pagination.")
    public ResponseEntity<Page<SecurityAuditEventResponse>> searchEvents(
            @Parameter(description = "Filter by user ID") @RequestParam(required = false) UUID userId,
            @Parameter(description = "Filter by event type") @RequestParam(required = false) SecurityAuditEventType eventType,
            @Parameter(description = "Filter by username") @RequestParam(required = false) String username,
            @Parameter(description = "Filter by IP address") @RequestParam(required = false) String ipAddress,
            @Parameter(description = "Events from (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Events to (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Spring Data pagination (page, size, sort)") Pageable pageable) {

        Page<SecurityAuditEventResponse> page = auditService
                .search(userId, eventType, username, ipAddress, from, to, pageable)
                .map(SecurityAuditEventResponse::from);
        return ResponseEntity.ok(page);
    }
}
