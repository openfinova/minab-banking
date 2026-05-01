package com.openfinova.banking.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Stateless API token policy (OAuth2 access / refresh TTLs and concurrent login cap).
 * <p>
 * Maps to banking session expectations: short access tokens, bounded refresh lifetime, optional
 * limit on simultaneous authorizations per user per registered client.
 */
@Component
@ConfigurationProperties(prefix = "identity.oauth2")
public class OAuth2TokenPolicyProperties {

    /** Access token time-to-live in minutes. */
    private int accessTokenTtlMinutes = 60;

    /** Refresh token time-to-live in days. */
    private int refreshTokenTtlDays = 7;

    /**
     * When false, each refresh issues a new refresh token and the old one is invalidated (refresh
     * token rotation).
     */
    private boolean reuseRefreshTokens = false;

    /**
     * Maximum simultaneous OAuth2 authorizations per principal per registered client. Oldest
     * authorizations are revoked when exceeded. {@code 0} means unlimited.
     */
    private int maxActiveAuthorizationsPerUser = 0;

    /**
     * When true and the request is available, include the client IP in the JWT as {@code client_ip}
     * for coarse binding / audit (not a substitute for DPoP or mTLS).
     */
    private boolean includeClientIpClaim = true;

    public int getAccessTokenTtlMinutes() {
        return accessTokenTtlMinutes;
    }

    public void setAccessTokenTtlMinutes(int accessTokenTtlMinutes) {
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    public int getRefreshTokenTtlDays() {
        return refreshTokenTtlDays;
    }

    public void setRefreshTokenTtlDays(int refreshTokenTtlDays) {
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    public boolean isReuseRefreshTokens() {
        return reuseRefreshTokens;
    }

    public void setReuseRefreshTokens(boolean reuseRefreshTokens) {
        this.reuseRefreshTokens = reuseRefreshTokens;
    }

    public int getMaxActiveAuthorizationsPerUser() {
        return maxActiveAuthorizationsPerUser;
    }

    public void setMaxActiveAuthorizationsPerUser(int maxActiveAuthorizationsPerUser) {
        this.maxActiveAuthorizationsPerUser = maxActiveAuthorizationsPerUser;
    }

    public boolean isIncludeClientIpClaim() {
        return includeClientIpClaim;
    }

    public void setIncludeClientIpClaim(boolean includeClientIpClaim) {
        this.includeClientIpClaim = includeClientIpClaim;
    }
}
