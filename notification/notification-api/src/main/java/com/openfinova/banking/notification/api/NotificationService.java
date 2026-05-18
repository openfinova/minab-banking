package com.openfinova.banking.notification.api;

import com.openfinova.banking.notification.api.dto.SendNotificationCommand;

/**
 * Façade interface for Notification operations.
 * Used by other modules (like compliance) to send internal or external communications.
 */
public interface NotificationService {

    /**
     * Sends a notification based on the provided command.
     *
     * @param command the notification details
     */
    void sendNotification(SendNotificationCommand command);
}
