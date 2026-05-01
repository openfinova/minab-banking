package com.openfinova.banking.gl.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;
import com.openfinova.banking.gl.entity.GLAccount;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing the configuration mapping between operational account types
 * and actual GL accounts. This allows the system to know which GL accounts to use
 * for bank operational transactions like fee income, cash handling, etc.
 *
 * Each operational account type maps to exactly one GL account (in its base currency).
 * Currency conversion is handled at the posting level by the GL module.
 *
 * Example:
 * - OperationalGLConfig(FEE_INCOME, UUID-123 pointing to GL-4001 in USD)
 * - OperationalGLConfig(CASH_VAULT, UUID-456 pointing to GL-1001 in USD)
 */
@Entity
@Table(name = "operational_gl_config", indexes = { @Index(name = "idx_op_gl_config_type", columnList = "config_type"),
        @Index(name = "idx_op_gl_config_active", columnList = "is_active") }, uniqueConstraints = {
                @UniqueConstraint(name = "uk_op_gl_config_type", columnNames = { "config_type" }) })
public class OperationalGLConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The type of operational account (e.g., FEE_INCOME, CASH_VAULT)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "config_type", nullable = false, length = 50)
    @NotNull(message = "Configuration type is required")
    private OperationalGLAccountType configType;

    /**
     * The GL account to use for this operational account type.
     * The GL account has its own base currency, and currency exchange is handled
     * at the posting level by the GL module.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gl_account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_op_gl_config_account"))
    @NotNull(message = "GL account is required")
    private GLAccount glAccount;

    /**
     * Optional description for this configuration
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Whether this configuration is currently active
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Priority/weight for this configuration (for future use if multiple configs exist)
     */
    @Column(name = "priority")
    private Integer priority = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public OperationalGLConfig() {
    }

    public OperationalGLConfig(OperationalGLAccountType configType, GLAccount glAccount, String createdBy) {
        this.configType = configType;
        this.glAccount = glAccount;
        this.createdBy = createdBy;
        this.isActive = true;
    }

    /**
     * Checks if this configuration is currently active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Activates this configuration.
     *
     * @param activatedBy the user activating the configuration
     */
    public void activate(String activatedBy) {
        this.isActive = true;
        this.updatedBy = activatedBy;
    }

    /**
     * Deactivates this configuration.
     *
     * @param deactivatedBy the user deactivating the configuration
     */
    public void deactivate(String deactivatedBy) {
        this.isActive = false;
        this.updatedBy = deactivatedBy;
    }

    /**
     * Updates the GL account for this configuration.
     *
     * @param newGLAccount the new GL account
     * @param updatedBy the user making the update
     */
    public void updateGLAccount(GLAccount newGLAccount, String updatedBy) {
        this.glAccount = newGLAccount;
        this.updatedBy = updatedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OperationalGLAccountType getConfigType() {
        return configType;
    }

    public void setConfigType(OperationalGLAccountType configType) {
        this.configType = configType;
    }

    public GLAccount getGlAccount() {
        return glAccount;
    }

    public void setGlAccount(GLAccount glAccount) {
        this.glAccount = glAccount;
    }

    /**
     * Convenience accessor that returns the UUID of the associated GL account.
     * Avoids forcing callers to navigate through the association just for the ID.
     */
    public UUID getGlAccountId() {
        return glAccount != null ? glAccount.getId() : null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OperationalGLConfig that = (OperationalGLConfig) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OperationalGLConfig{" + "id=" + id + ", configType=" + configType + ", glAccountId="
                + (glAccount != null ? glAccount.getId() : null) + ", isActive=" + isActive + '}';
    }
}
