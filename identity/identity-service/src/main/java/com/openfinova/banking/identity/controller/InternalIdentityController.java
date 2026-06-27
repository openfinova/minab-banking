package com.openfinova.banking.identity.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.identity.dto.BankingClaimsResponse;
import com.openfinova.banking.identity.dto.KeycloakAuditEvent;
import com.openfinova.banking.identity.service.InternalClaimsService;
import com.openfinova.banking.identity.service.KeycloakAuditIngestService;

/**
 * Internal, machine-to-machine endpoints for the Keycloak integration.
 *
 * These endpoints are the single integration point the Keycloak SPI uses to (1) fetch the banking
 * authorization claims at token issuance / login and (2) forward authentication events into the
 * banking audit trail.
 *
 * Security: there is no end-user principal on these calls. They are not exposed to clients and are
 * guarded by {@code InternalApiTokenFilter}, which requires the shared {@code X-Internal-Token}
 * secret on every {@code /internal/**} request; the path is otherwise network-restricted to the
 * container network. Method-level {@code @PreAuthorize} is therefore intentionally absent — the
 * authoritative boundary is the shared-secret filter, not user authorities.
 */
@RestController
@RequestMapping("/internal/identity")
public class InternalIdentityController {

    private final InternalClaimsService claimsService;
    private final KeycloakAuditIngestService auditIngestService;

    public InternalIdentityController(InternalClaimsService claimsService,
            KeycloakAuditIngestService auditIngestService) {
        this.claimsService = claimsService;
        this.auditIngestService = auditIngestService;
    }

    /**
     * Returns the banking authorization snapshot for a banking user id.
     *
     * @param userId the persistent {@code identity_users} id (equals the JWT {@code sub})
     * @return the banking claims, including the {@code eligible} login gate
     */
    @GetMapping("/claims/{userId}")
    public BankingClaimsResponse claims(@PathVariable UUID userId) {
        return claimsService.buildClaims(userId);
    }

    /**
     * Ingests a single Keycloak authentication event into the banking audit trail.
     *
     * @param event the forwarded Keycloak event
     * @return {@code 204 No Content}
     */
    @PostMapping("/audit")
    public ResponseEntity<Void> audit(@RequestBody KeycloakAuditEvent event) {
        auditIngestService.ingest(event);
        return ResponseEntity.noContent().build();
    }
}
