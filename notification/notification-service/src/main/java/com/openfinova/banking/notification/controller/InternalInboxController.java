package com.openfinova.banking.notification.controller;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.notification.entity.Notification;
import com.openfinova.banking.notification.repository.NotificationRepository;

@RestController
@RequestMapping("/api/v1/inbox")
public class InternalInboxController {

    /**
     * Team bucket used when compliance raises alerts without a concrete operator identity.
     */
    public static final String COMPLIANCE_SHARED_INBOX = "COMPLIANCE_INBOX";

    private static final String STAFF_NOTIFICATION_READ = "notification:read";
    private static final String COMPLIANCE_ALERT_READ = "compliance:alert:read";

    private final NotificationRepository notificationRepository;

    public InternalInboxController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public Page<Notification> getInbox(@RequestParam(required = false) Boolean unreadOnly,
            Authentication authentication, Pageable pageable) {
        List<String> recipientIds = resolveRecipientIds(authentication);
        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationRepository
                    .findByRecipientIdInAndIsReadOrderByCreatedAtDesc(recipientIds, false, pageable);
        }
        return notificationRepository.findByRecipientIdInOrderByCreatedAtDesc(recipientIds, pageable);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id, Authentication authentication) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!canAccessNotification(authentication, notification)) {
            throw new AccessDeniedException("Not authorized to update this notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok().build();
    }

    private static List<String> resolveRecipientIds(Authentication authentication) {
        Set<String> ids = new LinkedHashSet<>();
        ids.add(authentication.getName());
        if (hasAnyAuthority(authentication, STAFF_NOTIFICATION_READ, COMPLIANCE_ALERT_READ)) {
            ids.add(COMPLIANCE_SHARED_INBOX);
        }
        return new ArrayList<>(ids);
    }

    private static boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(authority::equals);
    }

    private static boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        if (authentication == null || authorities == null || authorities.length == 0) {
            return false;
        }
        for (String authority : authorities) {
            if (hasAuthority(authentication, authority)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canAccessNotification(Authentication authentication, Notification notification) {
        String principal = authentication.getName();
        if (notification.getRecipientId().equals(principal)) {
            return true;
        }
        return COMPLIANCE_SHARED_INBOX.equals(notification.getRecipientId())
                && hasAnyAuthority(authentication, STAFF_NOTIFICATION_READ, COMPLIANCE_ALERT_READ);
    }
}
