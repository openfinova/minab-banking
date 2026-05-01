package com.openfinova.banking.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import com.openfinova.banking.identity.audit.AuditEventDetail;
import com.openfinova.banking.identity.audit.SecurityAuditExtensions;
import com.openfinova.banking.identity.converter.AuditEventDetailConverter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable record of a security-relevant event (login, lockout, role change, etc.). Written by
 * {@code SecurityAuditService} and queried by administrators.
 */
@Entity
@Immutable
@Table(name = "identity_audit_events", indexes = { @Index(name = "idx_audit_user_id", columnList = "user_id"),
        @Index(name = "idx_audit_event_type", columnList = "event_type"),
        @Index(name = "idx_audit_created_at", columnList = "created_at"),
        @Index(name = "idx_audit_username", columnList = "username"),
        @Index(name = "idx_audit_changed_by_user", columnList = "changed_by_user_id") })
public class SecurityAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The user this event relates to. Nullable for failed logins by unknown usernames. */
    @Column(name = "user_id")
    private UUID userId;

    @Size(max = 80)
    @Column(length = 80)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private SecurityAuditEventType eventType;

    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Size(max = 500)
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Size(max = 1000)
    @Column(length = 1000)
    private String details;

    /** Structured JSON context for this event; complements the human-readable {@link #details}. */
    @Convert(converter = AuditEventDetailConverter.class)
    @Column(name = "details_json", columnDefinition = "TEXT")
    private AuditEventDetail detailsJson;

    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    @Size(max = 80)
    @Column(name = "changed_by_username", length = 80)
    private String changedByUsername;

    @Size(max = 2000)
    @Column(name = "previous_value", length = 2000)
    private String previousValue;

    @Size(max = 2000)
    @Column(name = "current_value", length = 2000)
    private String currentValue;

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    @Size(max = 80)
    @Column(name = "approved_by_username", length = 80)
    private String approvedByUsername;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SecurityAuditEvent() {
    }

    public SecurityAuditEvent(SecurityAuditEventType eventType, UUID userId, String username, String ipAddress,
            String userAgent, String details) {
        this(eventType, userId, username, ipAddress, userAgent, details, SecurityAuditExtensions.NONE, null);
    }

    public SecurityAuditEvent(SecurityAuditEventType eventType, UUID userId, String username, String ipAddress,
            String userAgent, String details, SecurityAuditExtensions ext) {
        this(eventType, userId, username, ipAddress, userAgent, details, ext, null);
    }

    public SecurityAuditEvent(SecurityAuditEventType eventType, UUID userId, String username, String ipAddress,
            String userAgent, String details, SecurityAuditExtensions ext, AuditEventDetail detailsJson) {
        SecurityAuditExtensions e = ext != null ? ext : SecurityAuditExtensions.NONE;
        this.eventType = eventType;
        this.userId = userId;
        this.username = username;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.details = details;
        this.detailsJson = detailsJson;
        this.changedByUserId = e.changedByUserId();
        this.changedByUsername = e.changedByUsername();
        this.previousValue = e.previousValue();
        this.currentValue = e.currentValue();
        this.approvedByUserId = e.approvedByUserId();
        this.approvedByUsername = e.approvedByUsername();
        this.approvalDate = e.approvalDate();
    }

    @PreUpdate
    @PreRemove
    @SuppressWarnings("unused")
    private void rejectMutation() {
        throw new UnsupportedOperationException("Security audit events are append-only");
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public SecurityAuditEventType getEventType() {
        return eventType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getDetails() {
        return details;
    }

    public AuditEventDetail getDetailsJson() {
        return detailsJson;
    }

    public UUID getChangedByUserId() {
        return changedByUserId;
    }

    public String getChangedByUsername() {
        return changedByUsername;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public UUID getApprovedByUserId() {
        return approvedByUserId;
    }

    public String getApprovedByUsername() {
        return approvedByUsername;
    }

    public LocalDateTime getApprovalDate() {
        return approvalDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
