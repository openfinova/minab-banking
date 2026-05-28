package com.openfinova.banking.customer.account.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.common.lib.converter.MapToJsonConverter;
import com.openfinova.banking.common.lib.validation.IBAN;
import com.openfinova.banking.common.lib.validation.ValidAccountNumber;
import com.openfinova.banking.customer.account.api.entity.AccountProductType;
import com.openfinova.banking.customer.account.api.entity.AccountStatus;
import com.openfinova.banking.customer.account.api.entity.GLAccountMappingType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Customer Account entity representing a customer-facing financial account.
 * This entity bridges customer accounts with the underlying General Ledger
 * system.
 */
@Entity
@Table(name = "accounts", indexes = { @Index(name = "idx_accounts_number", columnList = "account_number"),
        @Index(name = "idx_accounts_user_product", columnList = "primary_user_profile_id, product_type"),
        @Index(name = "idx_accounts_status", columnList = "status"),
        @Index(name = "idx_accounts_created", columnList = "created_at") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique account number used for identification.
     * Format: The format is defined in account.number.regex property and checked in ValidAccountNumber
     * Generated automatically upon creation.
     */
    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    @NotBlank(message = "Account number is required")
    @ValidAccountNumber
    private String accountNumber;

    /**
     * International Bank Account Number (IBAN).
     * Must be unique across the system.
     * Validated using Modulo 97 check.
     * Max length: 34 characters.
     */
    @Column(name = "iban", unique = true, length = 34)
    @Size(max = 34, message = "IBAN must not exceed 34 characters")
    @IBAN(message = "Invalid IBAN format")
    private String iban;

    /**
     * The ID of the user profile that owns this account.
     * This links to the external User Management module.
     */
    @Column(name = "primary_user_profile_id", nullable = false)
    @NotNull(message = "Primary user profile ID is required")
    private UUID primaryUserProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 30)
    @NotNull(message = "Account product type is required")
    private AccountProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Account status is required")
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "display_name", length = 100)
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @Column(name = "description", length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * The currency of the account (ISO 4217 3-letter code).
     * Cannot be changed once the account is created.
     */
    @Column(name = "currency", nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;

    /**
     * The actual balance of the account on the general ledger.
     * Includes all posted transactions (settled).
     * Does NOT include pending transactions or holds.
     */
    @Column(name = "ledger_balance", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Ledger balance is required")
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    /**
     * The balance available for withdrawal or spending.
     * Calculated as: Ledger Balance - Holds - Pending Debits.
     * Updated in real-time.
     */
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Available balance is required")
    private BigDecimal availableBalance = BigDecimal.ZERO;

    /**
     * Denormalized total of active short-lived transaction reservations managed by TP.
     * Kept on the account row to avoid CustomerAccount -> TP read-time calls.
     */
    @Column(name = "transaction_reserved_amount", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Transaction reserved amount is required")
    private BigDecimal transactionReservedAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "customerAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<GLAccountMapping> glAccountMappings = new ArrayList<>();

    @OneToMany(mappedBy = "customerAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<AccountRelationship> relationships = new ArrayList<>();

    @OneToMany(mappedBy = "customerAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<AccountLimit> limits = new ArrayList<>();

    @OneToMany(mappedBy = "customerAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<AccountHold> holds = new ArrayList<>();

    @OneToMany(mappedBy = "customerAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<InterestRate> interestRates = new ArrayList<>();

    /**
     * Flexible metadata store for additional account properties.
     * Stored as a JSON string in the database (TEXT column).
     */
    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "metadata", columnDefinition = "TEXT")
    private Map<String, Object> metadata = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    @NotBlank(message = "Created by is required")
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closure_reason", length = 255)
    private String closureReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public Account() {
    }

    public Account(UUID primaryUserProfileId, AccountProductType productType, String createdBy) {
        this.primaryUserProfileId = primaryUserProfileId;
        this.productType = productType;
        this.createdBy = createdBy;
    }

    /**
     * Checks if the account is in active status.
     *
     * @return true if account status is ACTIVE
     */
    public boolean isActive() {
        return AccountStatus.ACTIVE.equals(status);
    }

    /**
     * Checks if the account can perform transactions.
     *
     * @return true if account is active and not frozen
     */
    public boolean canTransact() {
        return isActive();
    }

    /**
     * Changes the account status with validation and audit trail.
     *
     * @param newStatus the new status to transition to
     * @param reason    the reason for the status change
     * @param changedBy the user making the change
     * @throws IllegalStateException if the status transition is invalid
     */
    public void changeStatus(AccountStatus newStatus, String reason, String changedBy, LocalDateTime changedAt) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s", status, newStatus));
        }

        this.status = newStatus;

        if (AccountStatus.CLOSED.equals(newStatus)) {
            this.closedAt = changedAt;
            this.closureReason = reason;
        }

        // TODO Event will be published by service layer
    }

    /**
     * Adds a GL account mapping to this customer account.
     *
     * @param glAccountId the GL account ID to map
     * @param mappingType the type of mapping
     */
    public void addGLAccountMapping(UUID glAccountId, GLAccountMappingType mappingType) {
        GLAccountMapping mapping = new GLAccountMapping();
        mapping.setCustomerAccount(this);
        mapping.setGlAccountId(glAccountId);
        mapping.setMappingType(mappingType);

        glAccountMappings.add(mapping);
    }

    /**
     * Gets all active GL account IDs mapped to this customer account.
     *
     * @return list of GL account IDs
     */
    public List<UUID> getGLAccountIds() {
        return glAccountMappings.stream().filter(GLAccountMapping::isActive).map(GLAccountMapping::getGlAccountId)
                .toList();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public UUID getPrimaryUserProfileId() {
        return primaryUserProfileId;
    }

    public void setPrimaryUserProfileId(UUID primaryUserProfileId) {
        this.primaryUserProfileId = primaryUserProfileId;
    }

    public AccountProductType getProductType() {
        return productType;
    }

    public void setProductType(AccountProductType productType) {
        this.productType = productType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getLedgerBalance() {
        return ledgerBalance;
    }

    public void setLedgerBalance(BigDecimal ledgerBalance) {
        this.ledgerBalance = ledgerBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getTransactionReservedAmount() {
        return transactionReservedAmount;
    }

    public void setTransactionReservedAmount(BigDecimal transactionReservedAmount) {
        this.transactionReservedAmount = transactionReservedAmount;
    }

    public List<GLAccountMapping> getGlAccountMappings() {
        return glAccountMappings;
    }

    public void setGlAccountMappings(List<GLAccountMapping> glAccountMappings) {
        this.glAccountMappings = glAccountMappings;
    }

    public List<AccountRelationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<AccountRelationship> relationships) {
        this.relationships = relationships;
    }

    public List<AccountLimit> getLimits() {
        return limits;
    }

    public void setLimits(List<AccountLimit> limits) {
        this.limits = limits;
    }

    public List<AccountHold> getHolds() {
        return holds;
    }

    public void setHolds(List<AccountHold> holds) {
        this.holds = holds;
    }

    public List<InterestRate> getInterestRates() {
        return interestRates;
    }

    public void setInterestRates(List<InterestRate> interestRates) {
        this.interestRates = interestRates;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getClosureReason() {
        return closureReason;
    }

    public void setClosureReason(String closureReason) {
        this.closureReason = closureReason;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Account that = (Account) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CustomerAccount{" + "id=" + id + ", accountNumber='" + accountNumber + '\'' + ", productType="
                + productType + ", status=" + status + '}';
    }
}