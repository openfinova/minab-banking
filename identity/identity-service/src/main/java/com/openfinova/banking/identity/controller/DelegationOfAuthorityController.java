package com.openfinova.banking.identity.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.identity.api.audit.AuditActor;
import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.identity.dto.CreateDelegationRequest;
import com.openfinova.banking.identity.dto.DelegationResponse;
import com.openfinova.banking.identity.service.DelegationOfAuthorityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/identity/delegations")
@Tag(name = "Delegation of authority", description = "Register and query delegations between staff users (DoA).")
@SecurityRequirement(name = "oauth2")
public class DelegationOfAuthorityController {

    private final DelegationOfAuthorityService delegationService;

    public DelegationOfAuthorityController(DelegationOfAuthorityService delegationService) {
        this.delegationService = delegationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin:doa:write')")
    @Operation(summary = "Create a delegation of authority")
    public ResponseEntity<DelegationResponse> create(Authentication auth,
            @Valid @RequestBody CreateDelegationRequest request) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DelegationResponse.from(delegationService.create(request, actor)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:doa:read')")
    @Operation(summary = "Get delegation by id")
    public DelegationResponse get(@PathVariable UUID id) {
        return DelegationResponse.from(delegationService.get(id));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('admin:doa:write')")
    @Operation(summary = "Revoke an active delegation")
    public DelegationResponse revoke(Authentication auth, @PathVariable UUID id) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return DelegationResponse.from(delegationService.revoke(id, actor));
    }

    @GetMapping("/outgoing/{userId}")
    @PreAuthorize("hasAuthority('admin:doa:read')")
    @Operation(summary = "List delegations granted by a user")
    public List<DelegationResponse> listOutgoing(@PathVariable UUID userId) {
        return delegationService.listOutgoing(userId);
    }

    @GetMapping("/incoming/{userId}")
    @PreAuthorize("hasAuthority('admin:doa:read')")
    @Operation(summary = "List delegations received by a user")
    public List<DelegationResponse> listIncoming(@PathVariable UUID userId) {
        return delegationService.listIncoming(userId);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('admin:doa:read')")
    @Operation(summary = "Active delegations for a delegatee and transaction type (for supervisory checks)")
    public List<DelegationResponse> activeForDelegatee(@RequestParam UUID delegateeUserId,
            @RequestParam String transactionType) {
        return delegationService.findActiveForDelegatee(delegateeUserId, transactionType).stream()
                .map(DelegationResponse::from).toList();
    }
}
