package com.openfinova.banking.identity.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.identity.entity.SecurityAuditEvent;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Record of a role permission change for compliance reporting")
public class PermissionChangeReportRow {

    private UUID eventId;
    private SecurityAuditEventType changeType;
    private String details;
    private UUID changedByUserId;
    private String changedByUsername;
    private String previousPermissions;
    private String currentPermissions;
    private UUID approvedByUserId;
    private String approvedByUsername;
    private LocalDateTime approvalDate;
    private LocalDateTime timestamp;

    public static PermissionChangeReportRow from(SecurityAuditEvent event) {
        PermissionChangeReportRow row = new PermissionChangeReportRow();
        row.eventId = event.getId();
        row.changeType = event.getEventType();
        row.details = event.getDetails();
        row.changedByUserId = event.getChangedByUserId();
        row.changedByUsername = event.getChangedByUsername();
        row.previousPermissions = event.getPreviousValue();
        row.currentPermissions = event.getCurrentValue();
        row.approvedByUserId = event.getApprovedByUserId();
        row.approvedByUsername = event.getApprovedByUsername();
        row.approvalDate = event.getApprovalDate();
        row.timestamp = event.getCreatedAt();
        return row;
    }

    public UUID getEventId() {
        return eventId;
    }

    public SecurityAuditEventType getChangeType() {
        return changeType;
    }

    public String getDetails() {
        return details;
    }

    public UUID getChangedByUserId() {
        return changedByUserId;
    }

    public String getChangedByUsername() {
        return changedByUsername;
    }

    public String getPreviousPermissions() {
        return previousPermissions;
    }

    public String getCurrentPermissions() {
        return currentPermissions;
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
