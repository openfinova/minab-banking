package com.openfinova.banking.customer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.openfinova.banking.customer.api.entity.CustomerRelationshipType;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a relationship between two customers.
 * Used for joint accounts, authorized signers, beneficiaries, business representatives, etc.
 */
@Entity
@Table(name = "customer_relationships", indexes = {
        @Index(name = "idx_cust_rel_primary", columnList = "primary_customer_id"),
        @Index(name = "idx_cust_rel_related", columnList = "related_customer_id"),
        @Index(name = "idx_cust_rel_type", columnList = "relationship_type"),
        @Index(name = "idx_cust_rel_active", columnList = "active") }, uniqueConstraints = {
                @UniqueConstraint(name = "uk_cust_rel_primary_related_type", columnNames = { "primary_customer_id",
                        "related_customer_id", "relationship_type" }) })
public class CustomerRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_customer_id", nullable = false)
    @NotNull(message = "Primary customer is required")
    private Customer primaryCustomer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "related_customer_id", nullable = false)
    @NotNull(message = "Related customer is required")
    private Customer relatedCustomer;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 30)
    @NotNull(message = "Relationship type is required")
    private CustomerRelationshipType relationshipType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Column(name = "removed_by", length = 100)
    private String removedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * Optional contextual notes about this relationship
     * (e.g., "Joint account holder since 2022", "Power of attorney ref: POA-2024-001").
     */
    @Column(name = "notes", length = 500)
    private String notes;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public CustomerRelationship() {
    }

    public CustomerRelationship(Customer primaryCustomer, Customer relatedCustomer,
            CustomerRelationshipType relationshipType, String createdBy) {
        this.primaryCustomer = primaryCustomer;
        this.relatedCustomer = relatedCustomer;
        this.relationshipType = relationshipType;
        this.createdBy = createdBy;
    }

    // Business Logic
    public void deactivate(String removedBy, LocalDateTime removedAt) {
        this.active = false;
        this.removedAt = removedAt;
        this.removedBy = removedBy;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Customer getPrimaryCustomer() {
        return primaryCustomer;
    }

    public void setPrimaryCustomer(Customer primaryCustomer) {
        this.primaryCustomer = primaryCustomer;
    }

    public Customer getRelatedCustomer() {
        return relatedCustomer;
    }

    public void setRelatedCustomer(Customer relatedCustomer) {
        this.relatedCustomer = relatedCustomer;
    }

    public CustomerRelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(CustomerRelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(LocalDateTime removedAt) {
        this.removedAt = removedAt;
    }

    public String getRemovedBy() {
        return removedBy;
    }

    public void setRemovedBy(String removedBy) {
        this.removedBy = removedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
