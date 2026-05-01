package com.openfinova.banking.identity.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.identity.entity.SecurityAuditEvent;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login attempt record (success or failure) for compliance reporting")
public class LoginActivityReportRow {

    private UUID eventId;
    private UUID userId;
    private String username;
    private SecurityAuditEventType eventType;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;

    public static LoginActivityReportRow from(SecurityAuditEvent event) {
        LoginActivityReportRow row = new LoginActivityReportRow();
        row.eventId = event.getId();
        row.userId = event.getUserId();
        row.username = event.getUsername();
        row.eventType = event.getEventType();
        row.ipAddress = event.getIpAddress();
        row.userAgent = event.getUserAgent();
        row.timestamp = event.getCreatedAt();
        return row;
    }

    public UUID getEventId() {
        return eventId;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
