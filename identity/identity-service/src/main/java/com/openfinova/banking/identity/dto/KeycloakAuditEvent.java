package com.openfinova.banking.identity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Authentication event forwarded by the Keycloak {@code banking-audit-listener} SPI.
 *
 * Carries only non-sensitive correlation data (event type, client, ip, username); never tokens or
 * secrets. Mapped onto the banking {@code SecurityAuditEventType} by {@code KeycloakAuditIngestService}.
 *
 * @param type      Keycloak event type name (e.g. LOGIN, LOGIN_ERROR)
 * @param realmId   originating realm id
 * @param clientId  OAuth client id involved in the event
 * @param userId    Keycloak user id (not the banking id); may be {@code null}
 * @param username  login name from the event details; may be {@code null}
 * @param ipAddress client IP address; may be {@code null}
 * @param error     Keycloak error code for failure events; may be {@code null}
 * @param time      event epoch millis
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakAuditEvent(String type, String realmId, String clientId, String userId, String username,
        String ipAddress, String error, Long time) {
}
