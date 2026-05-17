package com.openfinova.banking.customer.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.customer.api.dto.CustomerAuditEventResponse;
import com.openfinova.banking.customer.service.CustomerAuditReadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer audit", description = "Regulatory audit trail for customer profile changes")
public class CustomerAuditController {

    private static final Logger log = LoggerFactory.getLogger(CustomerAuditController.class);

    private final CustomerAuditReadService auditReadService;

    public CustomerAuditController(CustomerAuditReadService auditReadService) {
        this.auditReadService = auditReadService;
    }

    @GetMapping("/{customerId}/audit-events")
    @PreAuthorize("hasAuthority('customer:read')")
    @Operation(summary = "List customer audit events", description = "Paginated immutable audit entries. Sensitive old/new values are masked unless the caller has customer:pii:read.")
    public ResponseEntity<Page<CustomerAuditEventResponse>> listAuditEvents(@PathVariable UUID customerId,
            Authentication authentication, @PageableDefault(size = 25, sort = "changedAt") Pageable pageable) {
        log.info("Listing audit events for customer {}", customerId);
        Page<CustomerAuditEventResponse> page = auditReadService.listAuditEvents(customerId, pageable, authentication);
        return ResponseEntity.ok(page);
    }
}
