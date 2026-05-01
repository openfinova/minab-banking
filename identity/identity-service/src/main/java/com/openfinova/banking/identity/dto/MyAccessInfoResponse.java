package com.openfinova.banking.identity.dto;

import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.entity.BankingUser;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Self-service view of the authenticated user's access info. Intentionally omits admin-only fields
 * (createdBy, lockedReason, etc.).
 */
@Schema(description = "Self-service view of roles, MFA, and password policy flags")
public class MyAccessInfoResponse {

    private String username;
    private String email;
    private UserType userType;
    private Set<String> roles;
    private boolean mfaEnabled;
    private LocalDateTime passwordExpiresAt;
    private boolean forcePasswordChange;

    public static MyAccessInfoResponse from(BankingUser user) {
        MyAccessInfoResponse r = new MyAccessInfoResponse();
        r.username = user.getUsername();
        r.email = user.getEmail();
        r.userType = user.getUserType();
        r.roles = user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet());
        r.mfaEnabled = user.isMfaEnabled();
        r.passwordExpiresAt = user.getPasswordExpiresAt();
        r.forcePasswordChange = user.isForcePasswordChange();
        return r;
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

    public Set<String> getRoles() {
        return roles;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public LocalDateTime getPasswordExpiresAt() {
        return passwordExpiresAt;
    }

    public boolean isForcePasswordChange() {
        return forcePasswordChange;
    }
}
