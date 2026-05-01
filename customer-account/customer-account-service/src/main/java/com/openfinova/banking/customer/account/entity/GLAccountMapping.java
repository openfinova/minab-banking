package com.openfinova.banking.customer.account.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * GL Account Mapping entity representing the connection between customer
 * accounts
 * and underlying General Ledger accounts. Supports multiple mapping types and
 * weighted aggregation for complex account structures.
 * A single Account (customer account) can have multiple GLAccountMapping records pointing to it
 * Each GLAccountMapping belongs to exactly one Account
 * Example scenario:
 * Account (id=123, accountNumber="ACC001")
 *   ├─ GLAccountMapping (id=1, mappingType=PRIMARY_BALANCE, glAccountId=GL-001)
 *   ├─ GLAccountMapping (id=2, mappingType=INTEREST_PAYABLE, glAccountId=GL-002)
 *   └─ GLAccountMapping (id=3, mappingType=FEE_REVENUE, glAccountId=GL-003)
 * One customer account can map to multiple GL accounts for different purposes (primary balance, interest, fees, etc.),
 * so from the GLAccountMapping entity's perspective, it's a Many-to-One relationship.
 */
@Entity
@Table(name = "gl_account_mappings", indexes = {
        @Index(name = "idx_gl_mappings_customer_account", columnList = "customer_account_id"),
        @Index(name = "idx_gl_mappings_gl_account", columnList = "gl_account_id"),
        @Index(name = "idx_gl_mappings_type", columnList = "mapping_type"),
        @Index(name = "idx_gl_mappings_active", columnList = "is_active") }, uniqueConstraints = {
                @UniqueConstraint(name = "uk_gl_mapping_account_type", columnNames = { "customer_account_id",
                        "mapping_type", "gl_account_id" }) })
public class GLAccountMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the customer account that owns this mapping.
     * Many GLAccountMappings can belong to one Account.
     * A single customer account can have multiple GL account mappings for different purposes
     * (e.g., PRIMARY_BALANCE, INTEREST_PAYABLE, FEE_REVENUE).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_gl_mapping_customer_account"))
    private Account customerAccount;

    @Column(name = "gl_account_id", nullable = false)
    @NotNull(message = "GL account ID is required")
    private UUID glAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_type", nullable = false, length = 30)
    @NotNull(message = "Mapping type is required")
    private com.openfinova.banking.customer.account.api.entity.GLAccountMappingType mappingType;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Weight of the mapping, used for weighted aggregation of account balances.
     *
     * Use cases:
     * 1. Proportional balance distribution: When a customer account balance is split across
     *    multiple GL accounts, weights determine the proportion. For example:
     *    - GL Account A (weight=70): receives 70% of balance
     *    - GL Account B (weight=30): receives 30% of balance
     *
     * 2. Priority ordering: Higher weights can indicate primary vs secondary mappings
     *    when multiple mappings of the same type exist.
     *
     * 3. Aggregation calculations: When calculating total customer balance from multiple
     *    GL accounts, weights can be used to properly aggregate or average balances.
     *
     * Example scenario - Sweep account:
     * A customer checking account might have:
     * - PRIMARY_BALANCE → GL-Checking (weight=1, up to $1000)
     * - PRIMARY_BALANCE → GL-Savings (weight=1, excess over $1000)
     *
     * Example scenario - Multi-currency account:
     * A customer account with multiple currency sub-accounts:
     * - PRIMARY_BALANCE → GL-USD (weight=60)
     * - PRIMARY_BALANCE → GL-EUR (weight=30)
     * - PRIMARY_BALANCE → GL-GBP (weight=10)
     *
     * Default weight is 1 (equal weighting).
     */
    @Column(name = "weight", nullable = false)
    private Integer weight = 1;

    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "deactivation_reason", length = 255)
    private String deactivationReason;

    @Column(name = "deactivated_by", length = 100)
    private String deactivatedBy;

    public GLAccountMapping() {
    }

    public GLAccountMapping(Account customerAccount, UUID glAccountId,
            com.openfinova.banking.customer.account.api.entity.GLAccountMappingType mappingType, String createdBy) {
        this.customerAccount = customerAccount;
        this.glAccountId = glAccountId;
        this.mappingType = mappingType;
        this.createdBy = createdBy;
    }

    /**
     * Checks if this mapping is currently active.
     *
     * @return true if the mapping is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Deactivates this GL account mapping with reason and audit trail.
     *
     * @param reason        the reason for deactivation
     * @param deactivatedBy the user performing the deactivation
     */
    public void deactivate(String reason, String deactivatedBy) {
        this.isActive = false;
        this.deactivatedAt = LocalDateTime.now();
        this.deactivationReason = reason;
        this.deactivatedBy = deactivatedBy;
    }

    /**
     * Reactivates this GL account mapping.
     *
     * @param reactivatedBy the user performing the reactivation
     */
    public void reactivate(String reactivatedBy) {
        this.isActive = true;
        this.deactivatedAt = null;
        this.deactivationReason = null;
        this.deactivatedBy = null;
        // Note: We don't track reactivation details in this simple model
    }

    /**
     * Checks if this mapping is for primary balance.
     *
     * @return true if this is a primary balance mapping
     */
    public boolean isPrimaryBalance() {
        return com.openfinova.banking.customer.account.api.entity.GLAccountMappingType.PRIMARY_BALANCE
                .equals(mappingType);
    }

    /**
     * Validates the weight value.
     *
     * @throws IllegalArgumentException if weight is invalid
     */
    public void validateWeight() {
        if (weight == null || weight < 1) {
            throw new IllegalArgumentException("Weight must be a positive integer");
        }
    }

    /**
     * Sets the weight with validation.
     *
     * @param weight the weight value
     */
    public void setWeightWithValidation(Integer weight) {
        this.weight = weight;
        validateWeight();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Account getCustomerAccount() {
        return customerAccount;
    }

    public void setCustomerAccount(Account customerAccount) {
        this.customerAccount = customerAccount;
    }

    public UUID getGlAccountId() {
        return glAccountId;
    }

    public void setGlAccountId(UUID glAccountId) {
        this.glAccountId = glAccountId;
    }

    public com.openfinova.banking.customer.account.api.entity.GLAccountMappingType getMappingType() {
        return mappingType;
    }

    public void setMappingType(com.openfinova.banking.customer.account.api.entity.GLAccountMappingType mappingType) {
        this.mappingType = mappingType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public LocalDateTime getDeactivatedAt() {
        return deactivatedAt;
    }

    public void setDeactivatedAt(LocalDateTime deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
    }

    public String getDeactivationReason() {
        return deactivationReason;
    }

    public void setDeactivationReason(String deactivationReason) {
        this.deactivationReason = deactivationReason;
    }

    public String getDeactivatedBy() {
        return deactivatedBy;
    }

    public void setDeactivatedBy(String deactivatedBy) {
        this.deactivatedBy = deactivatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GLAccountMapping that = (GLAccountMapping) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "GLAccountMapping{" + "id=" + id + ", glAccountId=" + glAccountId + ", mappingType=" + mappingType
                + ", isActive=" + isActive + ", weight=" + weight + '}';
    }
}