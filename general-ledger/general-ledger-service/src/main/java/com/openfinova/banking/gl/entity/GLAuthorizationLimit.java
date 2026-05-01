package com.openfinova.banking.gl.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.gl.api.entity.GLTransactionSource;
import com.openfinova.banking.gl.api.entity.GLApprovalRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Authorization limits for GL transaction approval workflow.
 *
 * Defines what transaction amounts users with specific roles can:
 * - Create (maker limit)
 * - Approve (approval limit)
 *
 * Example configuration:
 * Role: ACCOUNTANT
 *   Maker Limit: $10,000 (can create transactions up to $10k)
 *   Approval Limit: $5,000 (can approve transactions up to $5k)
 *   Required Approvals: 1
 *
 * Role: MANAGER
 *   Maker Limit: $100,000 (can create transactions up to $100k)
 *   Approval Limit: $50,000 (can approve transactions up to $50k)
 *   Required Approvals: 1
 *
 * Role: CONTROLLER
 *   Maker Limit: $1,000,000
 *   Approval Limit: $500,000
 *   Required Approvals: 1
 *
 * Role: CFO
 *   Maker Limit: Unlimited
 *   Approval Limit: Unlimited
 *   Required Approvals: 2 (for amounts > $500k)
 *
 * Limits can be configured per currency and transaction source for fine-grained control.
 */
@Entity
@Table(name = "gl_authorization_limits", indexes = { @Index(name = "idx_auth_limits_role", columnList = "user_role"),
        @Index(name = "idx_auth_limits_source", columnList = "transaction_source") }, uniqueConstraints = {
                @UniqueConstraint(name = "uk_auth_limits_role_currency_source", columnNames = { "user_role", "currency",
                        "transaction_source" }) })
public class GLAuthorizationLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User role this limit applies to.
     * Defines authorization level for creating and approving transactions.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_role", nullable = false, length = 50)
    @NotNull(message = "User role is required")
    private GLApprovalRole approvalRole;

    /**
     * Maximum transaction amount this role can create (maker limit).
     * User cannot submit transactions exceeding this amount.
     * Use very large value (e.g., 999999999999.99) for "unlimited".
     */
    @Column(name = "maker_limit", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Maker limit is required")
    @Positive(message = "Maker limit must be positive")
    private BigDecimal makerLimit;

    /**
     * Maximum transaction amount this role can approve (approval limit).
     * User cannot approve transactions exceeding this amount.
     * Typically smaller than or equal to maker limit.
     */
    @Column(name = "approval_limit", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Approval limit is required")
    @Positive(message = "Approval limit must be positive")
    private BigDecimal approvalLimit;

    /**
     * Currency this limit applies to.
     * Separate limits can be configured per currency.
     */
    @Column(nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    /**
     * Transaction source this limit applies to.
     * Can set different limits for manual entries vs corrections.
     * NULL means limit applies to all sources.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_source", length = 50)
    private GLTransactionSource transactionSource;

    /**
     * Number of approvals required for transactions at this level.
     * 1 = single approval
     * 2 = dual approval (two different approvers needed)
     * etc.
     */
    @Column(name = "required_approvals", nullable = false)
    @NotNull(message = "Required approvals is required")
    @Positive(message = "Required approvals must be at least 1")
    private Integer requiredApprovals = 1;

    /**
     * Whether this limit is currently active.
     * Allows disabling limits without deleting configuration.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 100)
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    // Constructors

    public GLAuthorizationLimit() {
    }

    public GLAuthorizationLimit(GLApprovalRole approvalRole, BigDecimal makerLimit, BigDecimal approvalLimit,
            String currency, Integer requiredApprovals) {
        if (makerLimit != null && approvalLimit != null && approvalLimit.compareTo(makerLimit) > 0) {
            throw new IllegalArgumentException(
                    String.format("approvalLimit (%s) cannot exceed makerLimit (%s)", approvalLimit, makerLimit));
        }
        this.approvalRole = approvalRole;
        this.makerLimit = makerLimit;
        this.approvalLimit = approvalLimit;
        this.currency = currency;
        this.requiredApprovals = requiredApprovals;
    }

    // Business logic methods

    /**
     * Cross-field invariant: approval limit may not exceed maker limit.
     *
     * A role that can only create transactions up to X should never be able to
     * approve amounts greater than X, and — more critically — a role whose
     * approvalLimit exceeds makerLimit would create un-approvable transactions:
     * any transaction the maker can create falls below approvalLimit so it could
     * pass the approver check, but a different role with a lower approvalLimit
     * would be unable to approve it, leaving the transaction permanently stuck.
     *
     * Bean Validation fires this check on every persist/merge via
     * {@code @EntityListeners} / {@code @Validated} service layer, ensuring no
     * invalid limit record ever reaches the database.
     */
    @AssertTrue(message = "Approval limit must not exceed maker limit")
    public boolean isApprovalLimitValid() {
        if (makerLimit == null || approvalLimit == null) {
            return true; // null check handled by @NotNull on each field
        }
        return approvalLimit.compareTo(makerLimit) <= 0;
    }

    /**
     * Check if a user with this role can create a transaction of the given amount.
     *
     * @param amount transaction amount to check
     * @return true if amount is within maker limit
     */
    public boolean canCreate(BigDecimal amount) {
        return isActive && amount.compareTo(makerLimit) <= 0;
    }

    /**
     * Check if a user with this role can approve a transaction of the given amount.
     *
     * @param amount transaction amount to check
     * @return true if amount is within approval limit
     */
    public boolean canApprove(BigDecimal amount) {
        return isActive && amount.compareTo(approvalLimit) <= 0;
    }

    /**
     * Check if this limit applies to a specific transaction source.
     *
     * @param source the transaction source
     * @return true if limit applies (either NULL source or matching source)
     */
    public boolean appliesTo(GLTransactionSource source) {
        return transactionSource == null || transactionSource.equals(source);
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GLApprovalRole getApprovalRole() {
        return approvalRole;
    }

    public void setApprovalRole(GLApprovalRole approvalRole) {
        this.approvalRole = approvalRole;
    }

    public BigDecimal getMakerLimit() {
        return makerLimit;
    }

    public void setMakerLimit(BigDecimal makerLimit) {
        if (makerLimit != null && approvalLimit != null && approvalLimit.compareTo(makerLimit) > 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "makerLimit (%s) cannot be less than the existing approvalLimit (%s). "
                                    + "Lower the approvalLimit first.",
                            makerLimit,
                            approvalLimit));
        }
        this.makerLimit = makerLimit;
    }

    public BigDecimal getApprovalLimit() {
        return approvalLimit;
    }

    public void setApprovalLimit(BigDecimal approvalLimit) {
        if (approvalLimit != null && makerLimit != null && approvalLimit.compareTo(makerLimit) > 0) {
            throw new IllegalArgumentException(
                    String.format("approvalLimit (%s) cannot exceed makerLimit (%s)", approvalLimit, makerLimit));
        }
        this.approvalLimit = approvalLimit;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public GLTransactionSource getTransactionSource() {
        return transactionSource;
    }

    public void setTransactionSource(GLTransactionSource transactionSource) {
        this.transactionSource = transactionSource;
    }

    public Integer getRequiredApprovals() {
        return requiredApprovals;
    }

    public void setRequiredApprovals(Integer requiredApprovals) {
        this.requiredApprovals = requiredApprovals;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // equals, hashCode, toString

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof GLAuthorizationLimit that))
            return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "GLAuthorizationLimit{" + "id=" + id + ", approvalRole='" + approvalRole + '\'' + ", makerLimit="
                + makerLimit + ", approvalLimit=" + approvalLimit + ", currency='" + currency + '\''
                + ", transactionSource=" + transactionSource + ", requiredApprovals=" + requiredApprovals
                + ", isActive=" + isActive + '}';
    }
}
