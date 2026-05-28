package com.openfinova.banking.customer.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.customer.api.entity.CustomerRelationshipType;

public class CustomerRelationshipResponse {

    private UUID id;
    private UUID primaryCustomerId;
    private UUID relatedCustomerId;
    private CustomerRelationshipType relationshipType;
    private boolean active;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime removedAt;
    private String removedBy;
    private String notes;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPrimaryCustomerId() {
        return primaryCustomerId;
    }

    public void setPrimaryCustomerId(UUID primaryCustomerId) {
        this.primaryCustomerId = primaryCustomerId;
    }

    public UUID getRelatedCustomerId() {
        return relatedCustomerId;
    }

    public void setRelatedCustomerId(UUID relatedCustomerId) {
        this.relatedCustomerId = relatedCustomerId;
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
