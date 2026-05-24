package com.openfinova.banking.notification.api.entity;

/**
 * Defines the supported channels for sending notifications.
 */
public enum NotificationChannel {
    /**
     * Sent only to the internal UI dashboard inbox.
     */
    INBOX_ONLY,

    /**
     * Sent via email (no-op currently) and visible in the inbox.
     */
    EMAIL,

    /**
     * Sent via SMS (no-op currently) and visible in the inbox.
     */
    SMS
}
