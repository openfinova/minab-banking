package com.openfinova.banking.notification.api.dto;

import com.openfinova.banking.notification.api.entity.NotificationChannel;
import com.openfinova.banking.notification.api.entity.NotificationSeverity;

/**
 * Command object used to request a new notification.
 */
public class SendNotificationCommand {
    private String recipientId; // Can be a User ID or Role (e.g. ROLE_COMPLIANCE)
    private String subject;
    private String message;
    private NotificationChannel channel;
    private NotificationSeverity severity;

    public SendNotificationCommand() {
    }

    public SendNotificationCommand(String recipientId, String subject, String message, NotificationChannel channel,
            NotificationSeverity severity) {
        this.recipientId = recipientId;
        this.subject = subject;
        this.message = message;
        this.channel = channel;
        this.severity = severity;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public NotificationSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(NotificationSeverity severity) {
        this.severity = severity;
    }
}
