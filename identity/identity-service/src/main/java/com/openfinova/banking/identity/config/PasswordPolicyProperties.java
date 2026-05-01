package com.openfinova.banking.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Configurable password policy for the banking identity module. Override defaults via
 * {@code identity.password.*} properties.
 */
@ConfigurationProperties(prefix = "identity.password")
@Validated
public class PasswordPolicyProperties {

    private int minLength = 12;
    private int maxLength = 128;
    private boolean requireUppercase = true;
    private boolean requireLowercase = true;
    private boolean requireDigit = true;
    private boolean requireSpecialChar = true;
    private int maxAgeDays = 90;

    @Min(1)
    @Max(48)
    private int historyCount = 12;

    /**
     * When true, the plaintext password submitted at form login is checked against current
     * complexity rules. If it fails,
     * {@link com.openfinova.banking.identity.entity.BankingUser#setForcePasswordChange(boolean)} is
     * set so the user must change password before using APIs (see resource-server filter).
     */
    private boolean revalidateOnLogin = true;

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int v) {
        this.minLength = v;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int v) {
        this.maxLength = v;
    }

    public boolean isRequireUppercase() {
        return requireUppercase;
    }

    public void setRequireUppercase(boolean v) {
        this.requireUppercase = v;
    }

    public boolean isRequireLowercase() {
        return requireLowercase;
    }

    public void setRequireLowercase(boolean v) {
        this.requireLowercase = v;
    }

    public boolean isRequireDigit() {
        return requireDigit;
    }

    public void setRequireDigit(boolean v) {
        this.requireDigit = v;
    }

    public boolean isRequireSpecialChar() {
        return requireSpecialChar;
    }

    public void setRequireSpecialChar(boolean v) {
        this.requireSpecialChar = v;
    }

    public int getMaxAgeDays() {
        return maxAgeDays;
    }

    public void setMaxAgeDays(int v) {
        this.maxAgeDays = v;
    }

    public int getHistoryCount() {
        return historyCount;
    }

    public void setHistoryCount(int v) {
        this.historyCount = v;
    }

    public boolean isRevalidateOnLogin() {
        return revalidateOnLogin;
    }

    public void setRevalidateOnLogin(boolean v) {
        this.revalidateOnLogin = v;
    }
}
