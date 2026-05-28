package com.openfinova.banking.notification.mapper;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.openfinova.banking.notification.dto.InboxNotificationResponse;
import com.openfinova.banking.notification.entity.Notification;

@Component
public class NotificationInboxMapper {

    public InboxNotificationResponse toInboxResponse(Notification notification) {
        if (notification == null) {
            return null;
        }
        InboxNotificationResponse response = new InboxNotificationResponse();
        response.setId(notification.getId());
        response.setSubject(notification.getSubject());
        response.setMessage(notification.getMessage());
        response.setChannel(notification.getChannel());
        response.setSeverity(notification.getSeverity());
        response.setRead(notification.isRead());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }

    public Page<InboxNotificationResponse> toInboxPage(Page<Notification> notifications) {
        return notifications.map(this::toInboxResponse);
    }
}
