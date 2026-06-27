package com.openfinova.banking.keycloak.audit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.util.JsonSerialization;

/**
 * Forwards Keycloak authentication events to the banking audit trail so that
 * {@code SecurityAuditService} retains coverage of login, MFA, and session events after Keycloak
 * becomes the authentication authority.
 *
 * Best-effort and non-blocking: failures to reach the banking platform are logged and never break
 * the login flow.
 */
public class BankingAuditEventListenerProvider implements EventListenerProvider {

    private static final Logger LOG = Logger.getLogger(BankingAuditEventListenerProvider.class);
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    /** Authentication-relevant events forwarded to banking; admin/config noise is dropped. */
    private static final Set<EventType> FORWARDED = Set.of(
            EventType.LOGIN,
            EventType.LOGIN_ERROR,
            EventType.LOGOUT,
            EventType.CODE_TO_TOKEN,
            EventType.REFRESH_TOKEN,
            EventType.REFRESH_TOKEN_ERROR,
            EventType.UPDATE_PASSWORD,
            EventType.UPDATE_TOTP,
            EventType.REMOVE_TOTP,
            EventType.RESET_PASSWORD);

    private final HttpClient httpClient;
    private final String auditUrl;
    private final String internalToken;

    public BankingAuditEventListenerProvider(HttpClient httpClient, String baseUrl, String internalToken) {
        this.httpClient = httpClient;
        this.auditUrl = baseUrl + "/internal/identity/audit";
        this.internalToken = internalToken;
    }

    @Override
    public void onEvent(Event event) {
        if (!FORWARDED.contains(event.getType())) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", event.getType().name());
        payload.put("realmId", event.getRealmId());
        payload.put("clientId", event.getClientId());
        payload.put("userId", event.getUserId());
        payload.put("ipAddress", event.getIpAddress());
        payload.put("error", event.getError());
        payload.put("time", event.getTime());
        if (event.getDetails() != null) {
            // Username is fine to forward to the audit trail; never includes secrets.
            payload.put("username", event.getDetails().get("username"));
        }
        post(payload);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Admin/config events are not part of the security audit contract being preserved.
    }

    private void post(Map<String, Object> payload) {
        try {
            byte[] body = JsonSerialization.writeValueAsBytes(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(auditUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> {
                        LOG.debugf(ex, "Failed forwarding Keycloak event to banking audit");
                        return null;
                    });
        } catch (Exception ex) {
            LOG.debugf(ex, "Failed serialising Keycloak event for banking audit");
        }
    }

    @Override
    public void close() {
        // HttpClient is shared and managed by the factory.
    }
}
