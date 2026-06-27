package com.openfinova.banking.keycloak.internal;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

/** Binds a {@link KeycloakSession} to {@link BankingInternalResource}. */
public class BankingInternalResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;
    private final String internalToken;

    BankingInternalResourceProvider(KeycloakSession session, String internalToken) {
        this.session = session;
        this.internalToken = internalToken;
    }

    @Override
    public Object getResource() {
        return new BankingInternalResource(session, internalToken);
    }

    @Override
    public void close() {
        // Nothing to release.
    }
}
