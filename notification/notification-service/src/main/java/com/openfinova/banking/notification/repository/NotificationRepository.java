package com.openfinova.banking.notification.repository;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.openfinova.banking.notification.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(String recipientId, boolean isRead,
            Pageable pageable);

    Page<Notification> findByRecipientIdInOrderByCreatedAtDesc(Collection<String> recipientIds, Pageable pageable);

    Page<Notification> findByRecipientIdInAndIsReadOrderByCreatedAtDesc(Collection<String> recipientIds, boolean isRead,
            Pageable pageable);
}
