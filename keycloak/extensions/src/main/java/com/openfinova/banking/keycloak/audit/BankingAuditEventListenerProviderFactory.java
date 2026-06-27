package com.openfinova.banking.keycloak.audit;

import java.net.http.HttpClient;
import java.time.Duration;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for {@link BankingAuditEventListenerProvider}, registered under
 * {@code banking-audit-listener} and enabled via {@code eventsListeners} in the realm config.
 */
public class BankingAuditEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String PROVIDER_ID = "banking-audit-listener";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private String baseUrl;
    private String internalToken;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new BankingAuditEventListenerProvider(httpClient, baseUrl, internalToken);
    }

    @Override
    public void init(Config.Scope config) {
        this.baseUrl = stripTrailingSlash(config("BANKING_INTERNAL_URL", "http://banking-app:8080"));
        this.internalToken = config("BANKING_INTERNAL_TOKEN", "");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-init.
    }

    @Override
    public void close() {
        // Nothing to release.
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    private static String config(String envName, String defaultValue) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = System.getProperty(envName.toLowerCase().replace('_', '.'));
        }
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
