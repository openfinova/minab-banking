package com.openfinova.banking.identity.config;

/**
 * OAuth2 token lifetime and session policy for a single registered client ({@code client_id}).
 *
 * <p>
 * Refresh TTL of {@code 0} means no refresh-token settings are applied; pair with omitting the
 * {@code refresh_token} grant on the client registration when refresh must not be issued.
 */
public class OAuth2ClientTokenPolicy {

    /** Registered OAuth2 {@code client_id} this policy applies to. */
    private String clientId;

    /** Access token time-to-live in minutes. */
    private int accessTokenTtlMinutes = 60;

    /**
     * Refresh token time-to-live in minutes. {@code 0} disables refresh token TTL configuration
     * (use for clients without the refresh_token grant).
     */
    private int refreshTokenTtlMinutes;

    /**
     * When false, each refresh issues a new refresh token and invalidates the previous one (OAuth
     * 2.1 refresh token rotation).
     */
    private boolean reuseRefreshTokens = false;

    /**
     * Maximum simultaneous OAuth2 authorizations per principal for this client. {@code 0} means
     * unlimited.
     */
    private int maxActiveAuthorizationsPerUser;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getAccessTokenTtlMinutes() {
        return accessTokenTtlMinutes;
    }

    public void setAccessTokenTtlMinutes(int accessTokenTtlMinutes) {
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    public int getRefreshTokenTtlMinutes() {
        return refreshTokenTtlMinutes;
    }

    public void setRefreshTokenTtlMinutes(int refreshTokenTtlMinutes) {
        this.refreshTokenTtlMinutes = refreshTokenTtlMinutes;
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

    public boolean issuesRefreshTokens() {
        return refreshTokenTtlMinutes > 0;
    }
}
