package com.openfinova.banking.keycloak.internal;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * Exposes {@code /realms/{realm}/banking-internal/...} for trusted banking-platform callbacks
 * (TOTP credential sync). Protected by {@code X-Internal-Token}, matching the banking claims SPI.
 */
public class BankingInternalResourceProviderFactory implements RealmResourceProviderFactory {

    public static final String PROVIDER_ID = "banking-internal";

    private String internalToken;

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new BankingInternalResourceProvider(session, internalToken);
    }

    @Override
    public void init(Config.Scope config) {
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
}
