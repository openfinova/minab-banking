package com.openfinova.banking.customer.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class CustomerDataRetentionResponse {

    private UUID id;
    private UUID customerId;
    private LocalDate relationshipEndedAt;
    private LocalDate retentionExpiresAt;
    private int retentionYears;
    private String legalBasis;
    private boolean anonymized;
    private LocalDateTime anonymizedAt;
    private String anonymizedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public LocalDate getRelationshipEndedAt() {
        return relationshipEndedAt;
    }

    public void setRelationshipEndedAt(LocalDate relationshipEndedAt) {
        this.relationshipEndedAt = relationshipEndedAt;
    }

    public LocalDate getRetentionExpiresAt() {
        return retentionExpiresAt;
    }

    public void setRetentionExpiresAt(LocalDate retentionExpiresAt) {
        this.retentionExpiresAt = retentionExpiresAt;
    }

    public int getRetentionYears() {
        return retentionYears;
    }

    public void setRetentionYears(int retentionYears) {
        this.retentionYears = retentionYears;
    }

    public String getLegalBasis() {
        return legalBasis;
    }

    public void setLegalBasis(String legalBasis) {
        this.legalBasis = legalBasis;
    }

    public boolean isAnonymized() {
        return anonymized;
    }

    public void setAnonymized(boolean anonymized) {
        this.anonymized = anonymized;
    }

    public LocalDateTime getAnonymizedAt() {
        return anonymizedAt;
    }

    public void setAnonymizedAt(LocalDateTime anonymizedAt) {
        this.anonymizedAt = anonymizedAt;
    }

    public String getAnonymizedBy() {
        return anonymizedBy;
    }

    public void setAnonymizedBy(String anonymizedBy) {
        this.anonymizedBy = anonymizedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
