package com.openfinova.banking.tan.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.openfinova.banking.notification.api.NotificationService;
import com.openfinova.banking.notification.api.dto.SendNotificationCommand;
import com.openfinova.banking.notification.api.entity.NotificationChannel;
import com.openfinova.banking.notification.api.entity.NotificationSeverity;

@Component
public class TanNotificationHelper {

    private final NotificationService notificationService;

    public TanNotificationHelper(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void notifyDeviceEnrolled(UUID userId, String deviceName) {
        notificationService.sendNotification(
                new SendNotificationCommand(
                        userId.toString(),
                        "TAN device enrolled",
                        "A new TAN device was enrolled: " + deviceName,
                        NotificationChannel.EMAIL,
                        NotificationSeverity.INFO));
    }

    public void notifyDeviceRevoked(UUID userId, String deviceName) {
        notificationService.sendNotification(
                new SendNotificationCommand(
                        userId.toString(),
                        "TAN device revoked",
                        "TAN device revoked: " + deviceName,
                        NotificationChannel.EMAIL,
                        NotificationSeverity.WARNING));
    }
}
