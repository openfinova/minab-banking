package com.openfinova.banking.identity.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.validation.IdentityValidation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Create a banking user with optional initial roles")
public class CreateUserRequest {

    @NotBlank
    @Size(max = 80)
    private String username;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @Email
    @Size(max = 150)
    private String email;

    @NotNull
    private UserType userType;

    @Pattern(regexp = IdentityValidation.BRANCH_CODE_PATTERN, message = IdentityValidation.BRANCH_CODE_MESSAGE)
    private String branchCode;
    private String employeeId;
    private String glApprovalRole;
    private UUID customerPartyId;

    private Set<String> roleNames = Set.of();

    @Schema(description = "Optional account validity end (contractors, temps). Triggers expiry-warning job.")
    private LocalDateTime accountExpiresAt;

    @Schema(description = "Audit trail for eligibility / HR / KYC reference when provisioning is reviewed")
    @Size(max = 2000)
    private String provisioningEligibilityNotes;

    /**
     * Staff accounts must have a reachable email; customers may omit it (e.g. branch-only
     * onboarding).
     */
    @AssertTrue(message = "Staff users must have a non-blank email address")
    public boolean isStaffEmailRuleSatisfied() {
        if (userType == null || userType != UserType.STAFF) {
            return true;
        }
        return email != null && !email.isBlank();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String v) {
        this.username = v;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String v) {
        this.password = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        this.email = normalizeOptionalText(v);
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType v) {
        this.userType = v;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String v) {
        this.branchCode = normalizeOptionalText(v);
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String v) {
        this.employeeId = v;
    }

    public String getGlApprovalRole() {
        return glApprovalRole;
    }

    public void setGlApprovalRole(String v) {
        this.glApprovalRole = normalizeOptionalText(v);
    }

    public UUID getCustomerPartyId() {
        return customerPartyId;
    }

    public void setCustomerPartyId(UUID v) {
        this.customerPartyId = v;
    }

    public Set<String> getRoleNames() {
        return roleNames;
    }

    public void setRoleNames(Set<String> v) {
        this.roleNames = v;
    }

    public LocalDateTime getAccountExpiresAt() {
        return accountExpiresAt;
    }

    public void setAccountExpiresAt(LocalDateTime v) {
        this.accountExpiresAt = v;
    }

    public String getProvisioningEligibilityNotes() {
        return provisioningEligibilityNotes;
    }

    public void setProvisioningEligibilityNotes(String v) {
        this.provisioningEligibilityNotes = normalizeLargeOptionalText(v);
    }

    private static String normalizeOptionalText(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        return v.strip();
    }

    private static String normalizeLargeOptionalText(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        return v.strip();
    }
}
