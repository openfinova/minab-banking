package com.openfinova.banking.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurable brute-force lockout thresholds. Override via {@code identity.lockout.*} properties.
 */
@Component
@ConfigurationProperties(prefix = "identity.lockout")
public class LockoutProperties {

    private int maxAttempts = 5;
    private int lockoutDurationMinutes = 30;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int v) {
        this.maxAttempts = v;
    }

    public int getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }

    public void setLockoutDurationMinutes(int v) {
        this.lockoutDurationMinutes = v;
    }
}
