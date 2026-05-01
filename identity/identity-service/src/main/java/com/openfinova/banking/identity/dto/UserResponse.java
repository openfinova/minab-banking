package com.openfinova.banking.identity.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.entity.AccountProvisioningStatus;
import com.openfinova.banking.identity.entity.BankingUser;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Full administrative view of a banking user")
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private UserType userType;
    private boolean enabled;
    private boolean accountLocked;
    private String branchCode;
    private String employeeId;
    private String glApprovalRole;
    private UUID customerPartyId;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LocalDateTime passwordExpiresAt;
    private boolean forcePasswordChange;
    private LocalDateTime lastLoginAt;
    private boolean mfaEnabled;
    private LocalDateTime accountExpiresAt;
    private LocalDateTime accountExpiryWarningNotifiedAt;
    private LocalDateTime lockedAt;
    private String lockedReason;
    private LocalDateTime suspendedAt;
    private String suspensionReason;
    private LocalDateTime suspensionUntil;
    private LocalDateTime disabledAt;
    private UUID createdBy;
    private AccountProvisioningStatus provisioningStatus;
    private String provisioningEligibilityNotes;

    public static UserResponse from(BankingUser user) {
        UserResponse r = new UserResponse();
        r.id = user.getId();
        r.username = user.getUsername();
        r.email = user.getEmail();
        r.userType = user.getUserType();
        r.enabled = user.isEnabled();
        r.accountLocked = user.isAccountLocked();
        r.branchCode = user.getBranchCode();
        r.employeeId = user.getEmployeeId();
        r.glApprovalRole = user.getGlApprovalRole();
        r.customerPartyId = user.getCustomerPartyId();
        r.roles = user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet());
        r.createdAt = user.getCreatedAt();
        r.updatedAt = user.getUpdatedAt();

        r.passwordExpiresAt = user.getPasswordExpiresAt();
        r.forcePasswordChange = user.isForcePasswordChange();
        r.lastLoginAt = user.getLastLoginAt();
        r.mfaEnabled = user.isMfaEnabled();
        r.accountExpiresAt = user.getAccountExpiresAt();
        r.accountExpiryWarningNotifiedAt = user.getAccountExpiryWarningNotifiedAt();
        r.lockedAt = user.getLockedAt();
        r.lockedReason = user.getLockedReason();
        r.suspendedAt = user.getSuspendedAt();
        r.suspensionReason = user.getSuspensionReason();
        r.suspensionUntil = user.getSuspensionUntil();
        r.disabledAt = user.getDisabledAt();
        r.createdBy = user.getCreatedBy();
        r.provisioningStatus = user.getProvisioningStatus();
        r.provisioningEligibilityNotes = user.getProvisioningEligibilityNotes();
        return r;
    }

    public UUID getId() {
        return id;
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

    public String getBranchCode() {
        return branchCode;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getGlApprovalRole() {
        return glApprovalRole;
    }

    public UUID getCustomerPartyId() {
        return customerPartyId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getPasswordExpiresAt() {
        return passwordExpiresAt;
    }

    public boolean isForcePasswordChange() {
        return forcePasswordChange;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public LocalDateTime getAccountExpiresAt() {
        return accountExpiresAt;
    }

    public LocalDateTime getAccountExpiryWarningNotifiedAt() {
        return accountExpiryWarningNotifiedAt;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public String getLockedReason() {
        return lockedReason;
    }

    public LocalDateTime getSuspendedAt() {
        return suspendedAt;
    }

    public String getSuspensionReason() {
        return suspensionReason;
    }

    public LocalDateTime getSuspensionUntil() {
        return suspensionUntil;
    }

    public LocalDateTime getDisabledAt() {
        return disabledAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public AccountProvisioningStatus getProvisioningStatus() {
        return provisioningStatus;
    }

    public String getProvisioningEligibilityNotes() {
        return provisioningEligibilityNotes;
    }
}
