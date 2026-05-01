package com.openfinova.banking.identity.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.entity.AccountProvisioningStatus;
import com.openfinova.banking.identity.entity.BankingUser;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Snapshot of a single user's current access for regulatory reporting")
public class UserAccessReportRow {

    private UUID userId;
    private String username;
    private String email;
    private UserType userType;
    private boolean enabled;
    private boolean accountLocked;
    private AccountProvisioningStatus provisioningStatus;
    private List<String> assignedRoles;
    private LocalDateTime lastLoginAt;
    private LocalDateTime passwordExpiresAt;
    private LocalDateTime createdAt;

    public static UserAccessReportRow from(BankingUser user) {
        UserAccessReportRow row = new UserAccessReportRow();
        row.userId = user.getId();
        row.username = user.getUsername();
        row.email = user.getEmail();
        row.userType = user.getUserType();
        row.enabled = user.isEnabled();
        row.accountLocked = user.isAccountLocked();
        row.provisioningStatus = user.getProvisioningStatus();
        row.assignedRoles = user.getRoles().stream().map(r -> r.getName()).sorted().toList();
        row.lastLoginAt = user.getLastLoginAt();
        row.passwordExpiresAt = user.getPasswordExpiresAt();
        row.createdAt = user.getCreatedAt();
        return row;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public UserType getUserType() {
        return userType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public AccountProvisioningStatus getProvisioningStatus() {
        return provisioningStatus;
    }

    public List<String> getAssignedRoles() {
        return assignedRoles;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public LocalDateTime getPasswordExpiresAt() {
        return passwordExpiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
