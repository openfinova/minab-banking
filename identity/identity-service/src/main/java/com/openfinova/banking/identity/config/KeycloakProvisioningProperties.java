package com.openfinova.banking.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the Keycloak Admin API provisioning bridge.
 *
 * Keycloak is the authentication authority, so the banking platform mirrors user lifecycle changes
 * (create, enable/disable, password reset, forced change, deprovision) into Keycloak via its Admin
 * API. When {@code keycloak.enabled} is false the bridge no-ops, so the banking app can run
 * standalone (tests, isolated identity) without a Keycloak instance.
 */
@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProvisioningProperties {

    /** Master switch for the provisioning bridge. Disabled by default; enabled in compose. */
    private boolean enabled = false;

    /** In-network base URL of Keycloak, e.g. {@code http://keycloak:8080}. */
    private String baseUrl = "http://keycloak:8080";

    /** Target realm. */
    private String realm = "openfinova";

    /** Service-account client id used for Admin API access. */
    private String adminClientId = "banking-provisioning";

    /** Service-account client secret. */
    private String adminClientSecret = "";

    /** Shared secret for Keycloak internal SPI endpoints ({@code X-Internal-Token}). */
    private String internalToken = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getAdminClientId() {
        return adminClientId;
    }

    public void setAdminClientId(String adminClientId) {
        this.adminClientId = adminClientId;
    }

    public String getAdminClientSecret() {
        return adminClientSecret;
    }

    public void setAdminClientSecret(String adminClientSecret) {
        this.adminClientSecret = adminClientSecret;
    }

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }
}
