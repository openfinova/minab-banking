package com.openfinova.banking.customer.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.customer.api.entity.ContactType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entity representing contact information (Email, Phone, etc.) for a customer.
 */
@Entity
@Table(name = "contact_details", indexes = { @Index(name = "idx_contact_details_customer", columnList = "customer_id"),
        @Index(name = "idx_contact_details_type", columnList = "type") })
public class ContactDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Contact type is required")
    private ContactType type;

    @Column(name = "value", nullable = false, length = 255)
    @NotBlank(message = "Contact value is required")
    @Size(max = 255, message = "Value must not exceed 255 characters")
    private String value;

    /**
     * Boxed so JSON null does not fail deserialization (e.g. clients sending {@code "primary": null}).
     */
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = Boolean.FALSE;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = Boolean.FALSE;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * Timestamp of when the contact value was last verified (e.g., email link clicked, OTP confirmed).
     * Null if not yet verified.
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * User or system that performed the verification.
     */
    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Soft-delete timestamp. When set, the contact is considered deleted for display
     * but retained for regulatory audit. Null means active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Constructors
    public ContactDetail() {
    }

    public ContactDetail(Customer customer, ContactType type, String value, Boolean isPrimary) {
        this.customer = customer;
        this.type = type;
        this.value = value;
        this.isPrimary = isPrimary;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public ContactType getType() {
        return type;
    }

    public void setType(ContactType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isPrimary() {
        return Boolean.TRUE.equals(isPrimary);
    }

    public void setPrimary(Boolean primary) {
        isPrimary = Boolean.TRUE.equals(primary);
    }

    public boolean isVerified() {
        return Boolean.TRUE.equals(isVerified);
    }

    public void setVerified(Boolean verified) {
        isVerified = Boolean.TRUE.equals(verified);
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
