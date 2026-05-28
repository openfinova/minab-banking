package com.openfinova.banking.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.notification.api.NotificationService;
import com.openfinova.banking.notification.api.dto.SendNotificationCommand;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDispatchService notificationDispatchService;

    public NotificationServiceImpl(NotificationDispatchService notificationDispatchService) {
        this.notificationDispatchService = notificationDispatchService;
    }

    @Override
    public void sendNotification(SendNotificationCommand command) {
        notificationDispatchService.dispatch(command);
    }
}
