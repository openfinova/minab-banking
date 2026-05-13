package com.openfinova.banking.identity.dto;

import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.entity.AccountProvisioningStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserSearchCriteria", description = "Optional filters for GET /api/v1/identity/users/search")
public class UserSearchCriteria {

    @Schema(description = "Free text: case-insensitive match on username or email contains, or exact user id (UUID)")
    private String q;

    @Schema(description = "Username contains / match (service-specific)")
    private String username;

    @Schema(description = "Email filter")
    private String email;

    @Schema(description = "STAFF, CUSTOMER, etc.")
    private UserType userType;

    @Schema(description = "Account enabled flag")
    private Boolean enabled;

    @Schema(description = "Account locked flag")
    private Boolean locked;

    @Schema(description = "User must have this role name")
    private String roleName;

    @Schema(description = "Branch code")
    private String branchCode;

    @Schema(description = "Filter by provisioning lifecycle status")
    private AccountProvisioningStatus provisioningStatus;

    @Schema(description = "When true, only users with an administrative suspension start timestamp set")
    private Boolean suspended;

    public String getQ() {
        return q;
    }

    public void setQ(String v) {
        this.q = v;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String v) {
        this.username = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        this.email = v;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType v) {
        this.userType = v;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean v) {
        this.enabled = v;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean v) {
        this.locked = v;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String v) {
        this.roleName = v;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String v) {
        this.branchCode = v;
    }

    public AccountProvisioningStatus getProvisioningStatus() {
        return provisioningStatus;
    }

    public void setProvisioningStatus(AccountProvisioningStatus v) {
        this.provisioningStatus = v;
    }

    public Boolean getSuspended() {
        return suspended;
    }

    public void setSuspended(Boolean v) {
        this.suspended = v;
    }
}
