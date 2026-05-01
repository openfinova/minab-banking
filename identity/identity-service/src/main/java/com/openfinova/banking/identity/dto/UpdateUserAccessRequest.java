package com.openfinova.banking.identity.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.identity.validation.IdentityValidation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Patchable access fields for an existing user")
public class UpdateUserAccessRequest {

    @Email
    @Size(max = 150)
    private String email;

    @Pattern(regexp = IdentityValidation.BRANCH_CODE_PATTERN, message = IdentityValidation.BRANCH_CODE_MESSAGE)
    @Size(max = 20)
    private String branchCode;

    @Size(max = 40)
    private String employeeId;

    @Size(max = 30)
    private String glApprovalRole;

    private UUID customerPartyId;

    @Schema(description = "Account validity end; clears prior expiry-warning marker when changed")
    private LocalDateTime accountExpiresAt;

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        this.email = normalizeOptionalText(v);
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

    public LocalDateTime getAccountExpiresAt() {
        return accountExpiresAt;
    }

    public void setAccountExpiresAt(LocalDateTime v) {
        this.accountExpiresAt = v;
    }

    private static String normalizeOptionalText(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        return v.strip();
    }
}
