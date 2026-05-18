package com.openfinova.banking.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.notification.api.NotificationService;
import com.openfinova.banking.notification.api.dto.SendNotificationCommand;
import com.openfinova.banking.notification.entity.Notification;
import com.openfinova.banking.notification.repository.NotificationRepository;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void sendNotification(SendNotificationCommand command) {
        // 1. Persist to internal inbox
        Notification notification = new Notification(
                command.getRecipientId(),
                command.getSubject(),
                command.getMessage(),
                command.getChannel(),
                command.getSeverity());
        notificationRepository.save(notification);

        // 2. Dispatch to external channel if necessary (NO-OP implementation)
        switch (command.getChannel()) {
            case EMAIL -> logger.info(
                        "NO-OP EMAIL PROVIDER: Sending email to {} with subject '{}'",
                        command.getRecipientId(),
                        command.getSubject());
            case SMS -> logger.info(
                        "NO-OP SMS PROVIDER: Sending SMS to {} with subject '{}'",
                        command.getRecipientId(),
                        command.getSubject());
            case INBOX_ONLY -> {
            }
        }
        // Already handled by step 1
    }
}
