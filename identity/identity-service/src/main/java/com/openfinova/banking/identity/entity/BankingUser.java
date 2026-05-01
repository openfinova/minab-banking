package com.openfinova.banking.identity.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.crypto.MfaSecretAttributeConverter;
import com.openfinova.banking.identity.validation.IdentityValidation;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Persistent user account for authentication and authorization.
 *
 * Roles drive JWT permission claims; extra fields (branchCode, employeeId, customerPartyId,
 * glApprovalRole) are emitted as custom JWT claims by {@code TokenCustomizerConfig} so that
 * downstream services can enforce object-level and channel-level rules without hitting the
 * database.
 */
@Entity
@Table(name = "identity_users", indexes = {
        @Index(name = "idx_identity_users_username", columnList = "username", unique = true),
        @Index(name = "idx_identity_users_email", columnList = "email"),
        @Index(name = "idx_identity_users_customer_party", columnList = "customer_party_id"),
        @Index(name = "idx_identity_users_employee", columnList = "employee_id"),
        @Index(name = "idx_identity_users_provisioning", columnList = "provisioning_status") })
public class BankingUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * When set, must be a valid email. Staff accounts are required to have one at creation
     * ({@link com.openfinova.banking.identity.dto.CreateUserRequest}).
     */
    @Email
    @Size(max = 150)
    @Column(length = 150)
    private String email;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    private UserType userType;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked = false;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "password_expires_at")
    private LocalDateTime passwordExpiresAt;

    /** When true the user must change password on next login before any other action. */
    @Column(name = "force_password_change", nullable = false)
    private boolean forcePasswordChange = false;

    /**
     * Recent password hashes (most recent last) used to prevent reuse. Maintained by
     * {@code PasswordPolicyService}; the list length is capped by the configured
     * {@code identity.password.history-count} (default 12).
     */
    @ElementCollection
    @CollectionTable(name = "identity_password_history", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "password_hash", nullable = false)
    @OrderColumn(name = "seq")
    private List<String> passwordHistory = new ArrayList<>();

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Size(max = 45)
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    /** If non-null and in the future, the account is temporarily locked due to brute-force. */
    @Column(name = "failed_login_locked_until")
    private LocalDateTime failedLoginLockedUntil;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    /**
     * Base32-encoded TOTP shared secret (plaintext in memory). Encrypted at rest via
     * {@link MfaSecretAttributeConverter} (AES-256-GCM).
     */
    @Size(max = 512)
    @Convert(converter = MfaSecretAttributeConverter.class)
    @Column(name = "mfa_secret", length = 512)
    private String mfaSecret;

    @ElementCollection
    @CollectionTable(name = "identity_mfa_recovery_codes", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "code_hash", nullable = false, length = 100)
    private Set<String> mfaRecoveryCodes = new HashSet<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "provisioning_status", nullable = false, length = 24)
    private AccountProvisioningStatus provisioningStatus = AccountProvisioningStatus.ACTIVE;

    /**
     * Free-text evidence that eligibility checks were performed (HR/KYC reference, ticket id,
     * etc.). Populated at provisioning time for audit.
     */
    @Size(max = 2000)
    @Column(name = "provisioning_eligibility_notes", length = 2000)
    private String provisioningEligibilityNotes;

    /** Accounts with a set expiry (e.g. contractor, temporary) become inactive after this date. */
    @Column(name = "account_expires_at")
    private LocalDateTime accountExpiresAt;

    /**
     * When set, a lead-time expiry warning was recorded for the current {@link #accountExpiresAt}.
     */
    @Column(name = "account_expiry_warning_notified_at")
    private LocalDateTime accountExpiryWarningNotifiedAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Size(max = 500)
    @Column(name = "locked_reason", length = 500)
    private String lockedReason;

    /**
     * Administrative suspension (distinct from {@link #accountLocked} brute-force / admin lock).
     */
    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @Size(max = 500)
    @Column(name = "suspension_reason", length = 500)
    private String suspensionReason;

    /** When non-null and in the future, suspension lifts automatically at this instant. */
    @Column(name = "suspension_until")
    private LocalDateTime suspensionUntil;

    /** Soft-delete timestamp. When non-null the account is considered deleted. */
    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;

    /** The admin user who created this account (by user ID). */
    @Column(name = "created_by")
    private UUID createdBy;

    /**
     * Branch or cost-centre code. Emitted as {@code branch_code} JWT claim. Allows loan/GL services
     * to restrict staff to their own branch records.
     */
    @Pattern(regexp = IdentityValidation.BRANCH_CODE_PATTERN, message = IdentityValidation.BRANCH_CODE_MESSAGE)
    @Size(max = 20)
    @Column(name = "branch_code", length = 20)
    private String branchCode;

    /** Optional department or team code for scoped role permissions. */
    @Size(max = 40)
    @Column(name = "department_code", length = 40)
    private String departmentCode;

    /**
     * HR employee identifier. Emitted as {@code employee_id} JWT claim. Used for audit trails and
     * maker-checker attribution.
     */
    @Size(max = 40)
    @Column(name = "employee_id", length = 40)
    private String employeeId;

    /**
     * GL approval role name (e.g. "ACCOUNTANT", "MANAGER", "CFO"). Must match a known tier in
     * {@link com.openfinova.banking.identity.validation.GlApprovalRoleValidation} (aligned with
     * general-ledger {@code GLApprovalRole}). Used for maker–checker and identity workflow steps.
     * Null for users who are not involved in GL approval workflows.
     */
    @Size(max = 30)
    @Column(name = "gl_approval_role", length = 30)
    private String glApprovalRole;

    /**
     * Foreign-key (by value, not constraint) to the Customer.id in customer-service. Emitted as
     * {@code customer_party_id} JWT claim so ownership checks can compare it to the
     * {@code customerId} column on loan accounts and transactions.
     */
    @Column(name = "customer_party_id")
    private UUID customerPartyId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "identity_user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @BatchSize(size = 10)
    private Set<BankingRole> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BankingUser() {
    }

    public BankingUser(String username, String passwordHash, UserType userType) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.userType = userType;
    }

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String u) {
        this.username = u;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String p) {
        this.passwordHash = p;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String e) {
        this.email = e;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType t) {
        this.userType = t;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean v) {
        this.enabled = v;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(boolean v) {
        this.accountLocked = v;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String b) {
        this.branchCode = b;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String d) {
        this.departmentCode = d;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String e) {
        this.employeeId = e;
    }

    public String getGlApprovalRole() {
        return glApprovalRole;
    }

    public void setGlApprovalRole(String r) {
        this.glApprovalRole = r;
    }

    public UUID getCustomerPartyId() {
        return customerPartyId;
    }

    public void setCustomerPartyId(UUID id) {
        this.customerPartyId = id;
    }

    public Set<BankingRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<BankingRole> r) {
        this.roles = r;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Password lifecycle
    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(LocalDateTime v) {
        this.passwordChangedAt = v;
    }

    public LocalDateTime getPasswordExpiresAt() {
        return passwordExpiresAt;
    }

    public void setPasswordExpiresAt(LocalDateTime v) {
        this.passwordExpiresAt = v;
    }

    public boolean isForcePasswordChange() {
        return forcePasswordChange;
    }

    public void setForcePasswordChange(boolean v) {
        this.forcePasswordChange = v;
    }

    public List<String> getPasswordHistory() {
        return passwordHistory;
    }

    public void setPasswordHistory(List<String> v) {
        this.passwordHistory = v;
    }

    // Login tracking
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime v) {
        this.lastLoginAt = v;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String v) {
        this.lastLoginIp = v;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int v) {
        this.failedLoginAttempts = v;
    }

    public LocalDateTime getFailedLoginLockedUntil() {
        return failedLoginLockedUntil;
    }

    public void setFailedLoginLockedUntil(LocalDateTime v) {
        this.failedLoginLockedUntil = v;
    }

    // MFA
    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean v) {
        this.mfaEnabled = v;
    }

    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String v) {
        this.mfaSecret = v;
    }

    public Set<String> getMfaRecoveryCodes() {
        return mfaRecoveryCodes;
    }

    public void setMfaRecoveryCodes(Set<String> v) {
        this.mfaRecoveryCodes = v;
    }

    // Account lifecycle
    public AccountProvisioningStatus getProvisioningStatus() {
        return provisioningStatus;
    }

    public void setProvisioningStatus(AccountProvisioningStatus v) {
        this.provisioningStatus = v;
    }

    public String getProvisioningEligibilityNotes() {
        return provisioningEligibilityNotes;
    }

    public void setProvisioningEligibilityNotes(String v) {
        this.provisioningEligibilityNotes = v;
    }

    public LocalDateTime getAccountExpiresAt() {
        return accountExpiresAt;
    }

    public void setAccountExpiresAt(LocalDateTime v) {
        this.accountExpiresAt = v;
    }

    public LocalDateTime getAccountExpiryWarningNotifiedAt() {
        return accountExpiryWarningNotifiedAt;
    }

    public void setAccountExpiryWarningNotifiedAt(LocalDateTime v) {
        this.accountExpiryWarningNotifiedAt = v;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime v) {
        this.lockedAt = v;
    }

    public String getLockedReason() {
        return lockedReason;
    }

    public void setLockedReason(String v) {
        this.lockedReason = v;
    }

    public LocalDateTime getSuspendedAt() {
        return suspendedAt;
    }

    public void setSuspendedAt(LocalDateTime v) {
        this.suspendedAt = v;
    }

    public String getSuspensionReason() {
        return suspensionReason;
    }

    public void setSuspensionReason(String v) {
        this.suspensionReason = v;
    }

    public LocalDateTime getSuspensionUntil() {
        return suspensionUntil;
    }

    public void setSuspensionUntil(LocalDateTime v) {
        this.suspensionUntil = v;
    }

    public LocalDateTime getDisabledAt() {
        return disabledAt;
    }

    public void setDisabledAt(LocalDateTime v) {
        this.disabledAt = v;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID v) {
        this.createdBy = v;
    }

    /**
     * Whether the account is under an active administrative suspension at {@code now}.
     */
    public boolean isEffectivelySuspended(LocalDateTime now) {
        if (suspendedAt == null) {
            return false;
        }
        if (suspensionUntil != null && !suspensionUntil.isAfter(now)) {
            return false;
        }
        return true;
    }
}
