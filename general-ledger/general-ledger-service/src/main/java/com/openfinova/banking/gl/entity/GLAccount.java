package com.openfinova.banking.gl.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.common.lib.converter.MapToJsonConverter;
import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.gl.api.entity.CashFlowCategory;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import com.openfinova.banking.gl.api.entity.GLAccountStatus;
import com.openfinova.banking.gl.api.entity.BalanceType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Account entity representing a general ledger account in the chart of
 * accounts.
 * Supports hierarchical relationships and soft delete functionality.
 */
@Entity
@Table(name = "gl_accounts", indexes = { @Index(name = "idx_gl_accounts_code", columnList = "code"),
        @Index(name = "idx_gl_accounts_parent", columnList = "parent_id"),
        @Index(name = "idx_gl_accounts_type", columnList = "type") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GLAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Version field for optimistic locking.
     * Prevents race conditions during concurrent balance updates or account modifications.
     * Automatically incremented by JPA on each update.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Unique account code used for identification and hierarchy.
     * e.g., "1000", "1001", "2000".
     */
    @Column(unique = true, nullable = false, length = 50)
    @NotBlank(message = "Account code is required")
    @Size(max = 50, message = "Account code must not exceed 50 characters")
    private String code;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "Account name is required")
    @Size(max = 255, message = "Account name must not exceed 255 characters")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Account type is required")
    private GLAccountType type;

    /**
     * Three-letter ISO currency code for the account.
     * e.g., "USD", "EUR", "GBP".
     * Defines the denomination for all transactions and balances recorded in this
     * account.
     */
    @Column(length = 3, nullable = false)
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    /**
     * Parent account for hierarchical chart of accounts.
     * Roots have null parent.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_gl_account_parent"))
    private GLAccount parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<GLAccount> children = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Account status is required")
    private GLAccountStatus status = GLAccountStatus.ACTIVE;

    /**
     * The normal balance of the account (DEBIT/CREDIT).
     * Assets/Expenses are normally DEBIT.
     * Liabilities/Equity/Revenue are normally CREDIT.
     * For contra accounts this is the opposite of the parent type's normal balance.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "normal_balance", nullable = false, length = 10)
    @NotNull(message = "Normal balance type is required")
    private BalanceType normalBalance;

    /**
     * Marks this as a contra account — an account whose normal balance is opposite
     * to that of its parent account type.  Setting this flag via {@link #setContra}
     * automatically flips {@code normalBalance}.
     *
     * <p>Example uses in banking:
     * <ul>
     *   <li>Allowance for Credit Losses — contra-ASSET, normalBalance=CREDIT</li>
     *   <li>Accumulated Amortisation (bonds) — contra-LIABILITY, normalBalance=DEBIT</li>
     *   <li>Sales Returns — contra-REVENUE, normalBalance=DEBIT</li>
     * </ul>
     *
     * <p>The account's {@code type} is intentionally preserved so the account continues
     * to appear in the correct financial-statement section (Assets, Liabilities, etc.) but
     * with a deducting sign.
     */
    @Column(name = "contra", nullable = false)
    private boolean contra = false;

    /**
     * IAS 7 cash-flow statement section this account belongs to.
     *
     * <p>Defaults are set at creation time based on account type
     * (see {@link #determineDefaultCashFlowCategory}).
     * Account administrators can override the default when the operational
     * intent differs from the balance-sheet category — for example, to move
     * customer loans from INVESTING to OPERATING, or customer deposits from
     * FINANCING to OPERATING.
     *
     * <p>REVENUE and EXPENSE accounts use {@link CashFlowCategory#NONE}; their
     * effect on cash is already captured in the Net Income line.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "cash_flow_category", nullable = false, length = 10)
    private CashFlowCategory cashFlowCategory;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", nullable = false, length = 100)
    @NotBlank(message = "Created by is required")
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    @Size(max = 100, message = "Updated by must not exceed 100 characters")
    private String updatedBy;

    @Column(length = 1000)
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    /**
     * Flexible metadata store for additional account properties.
     * Stored as a JSON string in the database (TEXT column).
     */
    @Convert(converter = MapToJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> metadata;

    /**
     * Date when the account was inactivated.
     * Null if account is active.
     */
    @Column(name = "inactivated_on")
    private LocalDate inactivatedOn;

    /**
     * Reason for inactivation for audit purposes.
     * e.g., "Merged with Account 2000", "Policy change", "No longer used"
     */
    @Column(name = "inactivation_reason", length = 500)
    @Size(max = 500, message = "Inactivation reason must not exceed 500 characters")
    private String inactivationReason;

    public GLAccount() {
    }

    public GLAccount(String code, String name, GLAccountType type, String currency, String createdBy) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.createdBy = createdBy;
        this.normalBalance = determineNormalBalance(type, false);
        this.cashFlowCategory = determineDefaultCashFlowCategory(type);
    }

    /**
     * Helper method to determine normal balance based on account type,
     * optionally flipped for contra accounts.
     *
     * @param accountType the account classification
     * @param isContra    {@code true} to return the opposite (contra) normal balance
     * @return DEBIT or CREDIT normal balance
     */
    private BalanceType determineNormalBalance(GLAccountType accountType, boolean isContra) {
        BalanceType base = switch (accountType) {
            case ASSET, EXPENSE -> BalanceType.DEBIT;
            case LIABILITY, EQUITY, REVENUE -> BalanceType.CREDIT;
        };
        if (isContra) {
            return base == BalanceType.DEBIT ? BalanceType.CREDIT : BalanceType.DEBIT;
        }
        return base;
    }

    /**
     * Derives the IAS 7 default cash-flow category from the account type.
     *
     * <ul>
     *   <li>ASSET   → {@link CashFlowCategory#INVESTING} (override to OPERATING for loans/receivables)</li>
     *   <li>LIABILITY → {@link CashFlowCategory#FINANCING} (override to OPERATING for customer deposits)</li>
     *   <li>EQUITY  → {@link CashFlowCategory#FINANCING}</li>
     *   <li>REVENUE, EXPENSE → {@link CashFlowCategory#NONE} (already in net income)</li>
     * </ul>
     */
    private CashFlowCategory determineDefaultCashFlowCategory(GLAccountType accountType) {
        return switch (accountType) {
            case ASSET -> CashFlowCategory.INVESTING;
            case LIABILITY, EQUITY -> CashFlowCategory.FINANCING;
            case REVENUE, EXPENSE -> CashFlowCategory.NONE;
        };
    }

    /**
     * Checks if this account has child accounts
     *
     * @return true if this account has children, false otherwise
     */
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    /**
     * Checks if this account is active
     *
     * @return true if status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return GLAccountStatus.ACTIVE.equals(status);
    }

    /**
     * Marks this account as inactive (soft delete) with reason tracking.
     *
     * @param reason the reason for inactivation
     */
    public void markInactive(String reason) {
        this.status = GLAccountStatus.INACTIVE;
        this.inactivatedOn = LocalDate.now();
        this.inactivationReason = reason;
    }

    /**
     * Activates this account. Not recommended for use - once inactivated,
     * create a new account instead for accounting integrity.
     */
    public void activate() {
        this.status = GLAccountStatus.ACTIVE;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GLAccountType getType() {
        return type;
    }

    public GLAccountType getAccountType() {
        return type;
    }

    public void setType(GLAccountType type) {
        this.type = type;
        // Update normal balance when type changes, preserving the contra flag
        if (type != null) {
            this.normalBalance = determineNormalBalance(type, this.contra);
        }
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public GLAccount getParent() {
        return parent;
    }

    public void setParent(GLAccount parent) {
        this.parent = parent;
    }

    public List<GLAccount> getChildren() {
        return children;
    }

    public void setChildren(List<GLAccount> children) {
        this.children = children;
    }

    public GLAccountStatus getStatus() {
        return status;
    }

    public void setStatus(GLAccountStatus status) {
        this.status = status;
    }

    public BalanceType getNormalBalance() {
        return normalBalance;
    }

    public void setNormalBalance(BalanceType normalBalance) {
        this.normalBalance = normalBalance;
    }

    public boolean isContra() {
        return contra;
    }

    /**
     * Marks or unmarks this account as a contra account.
     *
     * <p>Setting {@code contra = true} automatically flips {@link #normalBalance}
     * to the opposite of the base normal balance for this account's {@link #type}.
     * Setting {@code contra = false} restores the standard normal balance.
     *
     * @param contra {@code true} to designate as a contra account
     */
    public void setContra(boolean contra) {
        this.contra = contra;
        this.normalBalance = determineNormalBalance(this.type, contra);
    }

    public CashFlowCategory getCashFlowCategory() {
        return cashFlowCategory;
    }

    public void setCashFlowCategory(CashFlowCategory cashFlowCategory) {
        this.cashFlowCategory = cashFlowCategory;
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

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDate getInactivatedOn() {
        return inactivatedOn;
    }

    public void setInactivatedOn(LocalDate inactivatedOn) {
        this.inactivatedOn = inactivatedOn;
    }

    public String getInactivationReason() {
        return inactivationReason;
    }

    public void setInactivationReason(String inactivationReason) {
        this.inactivationReason = inactivationReason;
    }

    // equals, hashCode, and toString

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof GLAccount account))
            return false;
        return code != null && code.equals(account.code);
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Account{" + "id=" + id + ", code='" + code + '\'' + ", name='" + name + '\'' + ", type=" + type
                + ", status=" + status + ", normalBalance=" + normalBalance + '}';
    }
}
