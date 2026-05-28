package com.openfinova.banking.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.notification.api.dto.SendNotificationCommand;
import com.openfinova.banking.notification.entity.Notification;
import com.openfinova.banking.notification.repository.NotificationRepository;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Persists notifications to the internal inbox and dispatches to external delivery channels.
 */
@Service
@Transactional
public class NotificationDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationRepository notificationRepository;
    private final DateTimeService dateTimeService;

    public NotificationDispatchService(NotificationRepository notificationRepository, DateTimeService dateTimeService) {
        this.notificationRepository = notificationRepository;
        this.dateTimeService = dateTimeService;
    }

    public void dispatch(SendNotificationCommand command) {
        Notification notification = new Notification(
                command.getRecipientId(),
                command.getSubject(),
                command.getMessage(),
                command.getChannel(),
                command.getSeverity(),
                dateTimeService.now());
        notificationRepository.save(notification);

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
    }
}
