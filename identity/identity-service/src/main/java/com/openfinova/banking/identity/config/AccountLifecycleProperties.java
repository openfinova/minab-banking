package com.openfinova.banking.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Account provisioning, deprovisioning, and expiry-warning behaviour.
 */
@ConfigurationProperties(prefix = "identity.lifecycle")
public class AccountLifecycleProperties {

    /**
     * When true, new users are created as {@code PENDING_APPROVAL} and cannot sign in until
     * approved.
     */
    private boolean requireProvisioningApproval = false;

    /**
     * Days before {@link com.openfinova.banking.identity.entity.BankingUser#getAccountExpiresAt()}
     * when the expiry-warning job publishes
     * {@link com.openfinova.banking.identity.event.UserAccountExpiryWarningEvent}.
     */
    private int expiryWarningLeadDays = 14;

    /**
     * Run expiry checks on this cron (Spring {@code @Scheduled} cron expression).
     */
    private String expiryNotificationCron = "0 0 7 * * *";

    private boolean expiryNotificationsEnabled = true;

    public boolean isRequireProvisioningApproval() {
        return requireProvisioningApproval;
    }

    public void setRequireProvisioningApproval(boolean requireProvisioningApproval) {
        this.requireProvisioningApproval = requireProvisioningApproval;
    }

    public int getExpiryWarningLeadDays() {
        return expiryWarningLeadDays;
    }

    public void setExpiryWarningLeadDays(int expiryWarningLeadDays) {
        this.expiryWarningLeadDays = expiryWarningLeadDays;
    }

    public String getExpiryNotificationCron() {
        return expiryNotificationCron;
    }

    public void setExpiryNotificationCron(String expiryNotificationCron) {
        this.expiryNotificationCron = expiryNotificationCron;
    }

    public boolean isExpiryNotificationsEnabled() {
        return expiryNotificationsEnabled;
    }

    public void setExpiryNotificationsEnabled(boolean expiryNotificationsEnabled) {
        this.expiryNotificationsEnabled = expiryNotificationsEnabled;
    }
}
