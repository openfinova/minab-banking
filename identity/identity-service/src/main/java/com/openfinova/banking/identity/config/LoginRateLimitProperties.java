package com.openfinova.banking.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate limiting for the HTML form login endpoint ({@code POST /login}) per client IP.
 */
@ConfigurationProperties(prefix = "banking.identity.login-rate-limit")
public class LoginRateLimitProperties {

    /**
     * When false, no rate limiting is applied (useful for local integration tests).
     */
    private boolean enabled = true;

    /**
     * Maximum {@code POST /login} attempts per IP per rolling minute.
     */
    private int maxAttemptsPerIpPerMinute = 40;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxAttemptsPerIpPerMinute() {
        return maxAttemptsPerIpPerMinute;
    }

    public void setMaxAttemptsPerIpPerMinute(int maxAttemptsPerIpPerMinute) {
        this.maxAttemptsPerIpPerMinute = maxAttemptsPerIpPerMinute;
    }
}
