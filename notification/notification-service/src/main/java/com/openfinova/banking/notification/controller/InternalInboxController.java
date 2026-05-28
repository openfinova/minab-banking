package com.openfinova.banking.notification.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.notification.dto.InboxNotificationResponse;
import com.openfinova.banking.notification.mapper.NotificationInboxMapper;
import com.openfinova.banking.notification.service.NotificationInboxService;

@RestController
@RequestMapping("/api/v1/inbox")
public class InternalInboxController {

    private final NotificationInboxService notificationInboxService;
    private final NotificationInboxMapper notificationInboxMapper;

    public InternalInboxController(NotificationInboxService notificationInboxService,
            NotificationInboxMapper notificationInboxMapper) {
        this.notificationInboxService = notificationInboxService;
        this.notificationInboxMapper = notificationInboxMapper;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('notification:read', 'compliance:alert:read')")
    public Page<InboxNotificationResponse> getInbox(@RequestParam(required = false) Boolean unreadOnly,
            Authentication authentication, Pageable pageable) {
        return notificationInboxMapper
                .toInboxPage(notificationInboxService.getInbox(authentication, unreadOnly, pageable));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAnyAuthority('notification:read', 'compliance:alert:read')")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id, Authentication authentication) {
        notificationInboxService.markAsRead(id, authentication);
        return ResponseEntity.ok().build();
    }
}
