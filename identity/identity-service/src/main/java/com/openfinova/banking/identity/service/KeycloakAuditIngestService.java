package com.openfinova.banking.identity.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.openfinova.banking.identity.dto.KeycloakAuditEvent;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.repository.UserRepository;

/**
 * Persists authentication events forwarded by Keycloak into the banking security audit trail.
 *
 * Keycloak is the authentication authority, so login/MFA/session events originate there. This
 * service maps the Keycloak event taxonomy onto {@link SecurityAuditEventType} and records each
 * event through {@link SecurityAuditService}, preserving the audit coverage that previously came
 * from the embedded Authorization Server login handlers.
 */
@Service
public class KeycloakAuditIngestService {

    /** Keycloak event type -> banking audit event type. Unmapped events are ignored. */
    private static final Map<String, SecurityAuditEventType> EVENT_TYPE_MAP = Map.of(
            "LOGIN",
            SecurityAuditEventType.LOGIN_SUCCESS,
            "LOGIN_ERROR",
            SecurityAuditEventType.LOGIN_FAILURE,
            "LOGOUT",
            SecurityAuditEventType.LOGOUT,
            "CODE_TO_TOKEN",
            SecurityAuditEventType.OAUTH2_AUTHORIZATION_ISSUED,
            "REFRESH_TOKEN",
            SecurityAuditEventType.OAUTH2_AUTHORIZATION_ISSUED,
            "REFRESH_TOKEN_ERROR",
            SecurityAuditEventType.LOGIN_FAILURE,
            "UPDATE_PASSWORD",
            SecurityAuditEventType.PASSWORD_CHANGED,
            "RESET_PASSWORD",
            SecurityAuditEventType.PASSWORD_CHANGED,
            "UPDATE_TOTP",
            SecurityAuditEventType.MFA_ENABLED,
            "REMOVE_TOTP",
            SecurityAuditEventType.MFA_DISABLED);

    private final SecurityAuditService auditService;
    private final UserRepository userRepository;

    public KeycloakAuditIngestService(SecurityAuditService auditService, UserRepository userRepository) {
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /**
     * Records a single Keycloak authentication event, resolving the banking user id from the
     * username when present. Events outside the mapped set are silently ignored.
     *
     * @param event the forwarded Keycloak event
     */
    public void ingest(KeycloakAuditEvent event) {
        SecurityAuditEventType mapped = EVENT_TYPE_MAP.get(event.type());
        if (mapped == null) {
            return;
        }
        UUID bankingUserId = Optional.ofNullable(event.username()).flatMap(userRepository::findByUsername)
                .map(BankingUser::getId).orElse(null);

        String details = buildDetails(event);
        auditService.record(mapped, bankingUserId, event.username(), event.ipAddress(), null, details);
    }

    private String buildDetails(KeycloakAuditEvent event) {
        StringBuilder sb = new StringBuilder("Keycloak event ").append(event.type());
        if (event.clientId() != null) {
            sb.append(" client=").append(event.clientId());
        }
        if (event.error() != null) {
            sb.append(" error=").append(event.error());
        }
        return sb.toString();
    }
}
