package com.openfinova.banking.identity.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.openfinova.banking.identity.entity.SecurityAuditEvent;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Security-relevant event for audit APIs")
public class SecurityAuditEventResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private SecurityAuditEventType eventType;
    private String ipAddress;
    private String userAgent;
    private String details;
    /** Structured context fields parsed from the stored JSON; null for older events. */
    private Map<String, Object> detailsJson;
    private UUID changedByUserId;
    private String changedByUsername;
    private String previousValue;
    private String currentValue;
    private UUID approvedByUserId;
    private String approvedByUsername;
    private LocalDateTime approvalDate;
    private LocalDateTime timestamp;

    public static SecurityAuditEventResponse from(SecurityAuditEvent event) {
        SecurityAuditEventResponse r = new SecurityAuditEventResponse();
        r.id = event.getId();
        r.userId = event.getUserId();
        r.username = event.getUsername();
        r.eventType = event.getEventType();
        r.ipAddress = event.getIpAddress();
        r.userAgent = event.getUserAgent();
        r.details = event.getDetails();
        r.detailsJson = event.getDetailsJson() != null ? event.getDetailsJson().getFields() : null;
        r.changedByUserId = event.getChangedByUserId();
        r.changedByUsername = event.getChangedByUsername();
        r.previousValue = event.getPreviousValue();
        r.currentValue = event.getCurrentValue();
        r.approvedByUserId = event.getApprovedByUserId();
        r.approvedByUsername = event.getApprovedByUsername();
        r.approvalDate = event.getApprovalDate();
        r.timestamp = event.getCreatedAt();
        return r;
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

    public Map<String, Object> getDetailsJson() {
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
