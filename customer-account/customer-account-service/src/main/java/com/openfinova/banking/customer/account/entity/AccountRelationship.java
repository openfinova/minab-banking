package com.openfinova.banking.customer.account.entity;

import com.openfinova.banking.customer.account.api.entity.RelationshipType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
// import org.hibernate.annotations.Type;
// import io.hypersistence.utils.hibernate.type.json.JsonType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Account Relationship entity representing the association between users and
 * customer accounts.
 * Manages ownership, permissions, and beneficiary designations for shared
 * accounts.
 */
@Entity
@Table(name = "account_relationships", indexes = {
        @Index(name = "idx_account_relationships_user", columnList = "user_profile_id"),
        @Index(name = "idx_account_relationships_account", columnList = "customer_account_id"),
        @Index(name = "idx_account_relationships_type", columnList = "relationship_type") }, uniqueConstraints = {
                @UniqueConstraint(name = "uk_account_user_relationship", columnNames = { "customer_account_id",
                        "user_profile_id", "relationship_type" }) })
public class AccountRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_account_relationship_customer_account"))
    private Account customerAccount;

    @Column(name = "user_profile_id", nullable = false)
    @NotNull(message = "User profile ID is required")
    private UUID userProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 20)
    @NotNull(message = "Relationship type is required")
    private RelationshipType relationshipType;

    /**
     * Set of specific permissions granted to this user for this account.
     * Stored as a JSON array or comma-separated string.
     */
    @Convert(converter = AccountPermissionSetConverter.class)
    @Column(name = "permissions", columnDefinition = "TEXT")
    private Set<com.openfinova.banking.customer.account.api.entity.AccountPermission> permissions = new HashSet<>();

    /**
     * Percentage of the account legally owned by this user.
     * Sum of ownership percentages across all owners should typically be 100%.
     */
    @Column(name = "percentage_ownership", precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "Percentage ownership must be non-negative")
    @DecimalMax(value = "100.00", message = "Percentage ownership cannot exceed 100%")
    private BigDecimal percentageOwnership;

    /**
     * Indicates if this user is a designated beneficiary (Payable on Death).
     */
    @Column(name = "is_beneficiary", nullable = false)
    private Boolean isBeneficiary = false;

    /**
     * Percentage of funds allocated to this beneficiary upon account holder death.
     */
    @Column(name = "beneficiary_percentage", precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "Beneficiary percentage must be non-negative")
    @DecimalMax(value = "100.00", message = "Beneficiary percentage cannot exceed 100%")
    private BigDecimal beneficiaryPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.openfinova.banking.customer.account.api.entity.RelationshipStatus status = com.openfinova.banking.customer.account.api.entity.RelationshipStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_until")
    private LocalDateTime effectiveUntil;

    public AccountRelationship() {
        this.effectiveFrom = LocalDateTime.now();
    }

    public AccountRelationship(Account customerAccount, UUID userProfileId, RelationshipType relationshipType,
            String createdBy) {
        this();
        this.customerAccount = customerAccount;
        this.userProfileId = userProfileId;
        this.relationshipType = relationshipType;
        this.createdBy = createdBy;
        this.permissions = relationshipType.getDefaultPermissions();
    }

    /**
     * Checks if the user has the specified permission.
     *
     * @param permission the permission to check
     * @return true if the user has the permission
     */
    public boolean hasPermission(com.openfinova.banking.customer.account.api.entity.AccountPermission permission) {
        return permissions.contains(permission);
    }

    /**
     * Checks if the relationship is currently effective.
     *
     * @return true if the relationship is active and within effective date range
     */
    public boolean isEffective() {
        LocalDateTime now = LocalDateTime.now();
        return status == com.openfinova.banking.customer.account.api.entity.RelationshipStatus.ACTIVE
                && !now.isBefore(effectiveFrom) && (effectiveUntil == null || !now.isAfter(effectiveUntil));
    }

    /**
     * Checks if this is a primary holder relationship.
     *
     * @return true if this is a primary holder
     */
    public boolean isPrimaryHolder() {
        return RelationshipType.PRIMARY_HOLDER.equals(relationshipType);
    }

    /**
     * Validates beneficiary percentage constraints.
     *
     * @throws IllegalArgumentException if beneficiary percentage is invalid
     */
    public void validateBeneficiaryPercentage() {
        if (Boolean.TRUE.equals(isBeneficiary)) {
            if (beneficiaryPercentage == null || beneficiaryPercentage.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Beneficiary percentage must be greater than 0 for beneficiaries");
            }
            if (beneficiaryPercentage.compareTo(new BigDecimal("100.00")) > 0) {
                throw new IllegalArgumentException("Beneficiary percentage cannot exceed 100%");
            }
        } else {
            if (beneficiaryPercentage != null && beneficiaryPercentage.compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException("Non-beneficiaries cannot have beneficiary percentage");
            }
        }
    }

    /**
     * Adds a permission to this relationship.
     *
     * @param permission the permission to add
     */
    public void addPermission(com.openfinova.banking.customer.account.api.entity.AccountPermission permission) {
        if (permissions == null) {
            permissions = new HashSet<>();
        }
        permissions.add(permission);
    }

    /**
     * Removes a permission from this relationship.
     *
     * @param permission the permission to remove
     */
    public void removePermission(com.openfinova.banking.customer.account.api.entity.AccountPermission permission) {
        if (permissions != null) {
            permissions.remove(permission);
        }
    }

    /**
     * Sets the relationship as a beneficiary with the specified percentage.
     *
     * @param percentage the beneficiary percentage
     */
    public void setBeneficiary(BigDecimal percentage) {
        this.isBeneficiary = true;
        this.beneficiaryPercentage = percentage;
        validateBeneficiaryPercentage();
    }

    /**
     * Removes beneficiary designation from this relationship.
     */
    public void removeBeneficiary() {
        this.isBeneficiary = false;
        this.beneficiaryPercentage = null;
    }

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

    public UUID getUserProfileId() {
        return userProfileId;
    }

    public void setUserProfileId(UUID userProfileId) {
        this.userProfileId = userProfileId;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    public Set<com.openfinova.banking.customer.account.api.entity.AccountPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<com.openfinova.banking.customer.account.api.entity.AccountPermission> permissions) {
        this.permissions = permissions;
    }

    public BigDecimal getPercentageOwnership() {
        return percentageOwnership;
    }

    public void setPercentageOwnership(BigDecimal percentageOwnership) {
        this.percentageOwnership = percentageOwnership;
    }

    public Boolean getIsBeneficiary() {
        return isBeneficiary;
    }

    public void setIsBeneficiary(Boolean isBeneficiary) {
        this.isBeneficiary = isBeneficiary;
    }

    public BigDecimal getBeneficiaryPercentage() {
        return beneficiaryPercentage;
    }

    public void setBeneficiaryPercentage(BigDecimal beneficiaryPercentage) {
        this.beneficiaryPercentage = beneficiaryPercentage;
    }

    public com.openfinova.banking.customer.account.api.entity.RelationshipStatus getStatus() {
        return status;
    }

    public void setStatus(com.openfinova.banking.customer.account.api.entity.RelationshipStatus status) {
        this.status = status;
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

    public void setUpdatedBy(String updatedBy) {
        // For audit purposes, we can store this in a separate field or log it
        // For now, we'll just update the createdBy field as a simple implementation
        this.createdBy = updatedBy;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getEffectiveUntil() {
        return effectiveUntil;
    }

    public void setEffectiveUntil(LocalDateTime effectiveUntil) {
        this.effectiveUntil = effectiveUntil;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AccountRelationship that = (AccountRelationship) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AccountRelationship{" + "id=" + id + ", userProfileId=" + userProfileId + ", relationshipType="
                + relationshipType + ", status=" + status + '}';
    }
}