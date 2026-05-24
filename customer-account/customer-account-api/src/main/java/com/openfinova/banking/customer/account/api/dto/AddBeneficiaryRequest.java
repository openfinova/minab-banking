package com.openfinova.banking.customer.account.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object for adding a beneficiary to an account.
 */
public class AddBeneficiaryRequest {
    @NotNull(message = "User profile ID is required")
    private UUID userProfileId;

    @NotNull(message = "Beneficiary percentage is required")
    @DecimalMin(value = "0.01", message = "Beneficiary percentage must be greater than 0")
    @DecimalMax(value = "100.00", message = "Beneficiary percentage cannot exceed 100%")
    private BigDecimal percentage;

    private String relationshipDescription;
    private LocalDate birthDate;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveUntil;

    public AddBeneficiaryRequest() {
    }

    public UUID getUserProfileId() {
        return userProfileId;
    }

    public void setUserProfileId(UUID userProfileId) {
        this.userProfileId = userProfileId;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public String getRelationshipDescription() {
        return relationshipDescription;
    }

    public void setRelationshipDescription(String relationshipDescription) {
        this.relationshipDescription = relationshipDescription;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
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
}
