package com.openfinova.banking.identity.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

/**
 * Per-client OAuth2 token policy (access / refresh TTLs, rotation, concurrent session cap).
 *
 * <p>
 * Keys under {@code identity.oauth2.clients.<client-id>.*} match registered {@code client_id}
 * values (e.g. {@code staff-portal}, {@code customer-portal}).
 */
@Component
@ConfigurationProperties(prefix = "identity.oauth2")
public class OAuth2TokenPolicyProperties {

    public static final String CLIENT_STAFF_PORTAL = "staff-portal";
    public static final String CLIENT_CUSTOMER_PORTAL = "customer-portal";
    public static final String CLIENT_STAFF_APP = "staff-app";
    /** Reserved for future native mobile client — not registered in dev yet. */
    public static final String CLIENT_MOBILE_APP = "mobile-app";

    /**
     * When true and the request is available, include the client IP in the JWT as {@code client_ip}
     * for coarse binding / audit (not a substitute for DPoP or mTLS).
     */
    private boolean includeClientIpClaim = true;

    private Map<String, OAuth2ClientTokenPolicy> clients = new LinkedHashMap<>();

    public boolean isIncludeClientIpClaim() {
        return includeClientIpClaim;
    }

    public void setIncludeClientIpClaim(boolean includeClientIpClaim) {
        this.includeClientIpClaim = includeClientIpClaim;
    }

    public Map<String, OAuth2ClientTokenPolicy> getClients() {
        return clients;
    }

    public void setClients(Map<String, OAuth2ClientTokenPolicy> clients) {
        this.clients = clients;
    }

    /**
     * Returns policy for the given {@code client_id}, or a conservative default when unset.
     */
    public OAuth2ClientTokenPolicy policyForClientId(String clientId) {
        OAuth2ClientTokenPolicy policy = clients.get(clientId);
        if (policy != null) {
            return policy;
        }
        OAuth2ClientTokenPolicy fallback = new OAuth2ClientTokenPolicy();
        fallback.setClientId(clientId);
        fallback.setAccessTokenTtlMinutes(60);
        fallback.setRefreshTokenTtlMinutes(0);
        fallback.setReuseRefreshTokens(false);
        fallback.setMaxActiveAuthorizationsPerUser(0);
        return fallback;
    }

    /**
     * Builds Spring Authorization Server {@link TokenSettings} for a registered client.
     */
    public TokenSettings toTokenSettings(String clientId) {
        OAuth2ClientTokenPolicy policy = policyForClientId(clientId);
        var builder = TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(policy.getAccessTokenTtlMinutes()))
                .reuseRefreshTokens(policy.isReuseRefreshTokens());
        if (policy.getRefreshTokenTtlMinutes() > 0) {
            builder.refreshTokenTimeToLive(Duration.ofMinutes(policy.getRefreshTokenTtlMinutes()));
        }
        return builder.build();
    }
}
