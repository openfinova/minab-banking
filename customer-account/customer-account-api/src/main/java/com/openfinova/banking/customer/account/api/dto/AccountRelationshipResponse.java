package com.openfinova.banking.customer.account.api.dto;

import com.openfinova.banking.customer.account.api.entity.AccountPermission;
import com.openfinova.banking.customer.account.api.entity.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Account relationship response")
public class AccountRelationshipResponse {

    @Schema(description = "Relationship ID")
    private UUID id;

    @Schema(description = "Account ID")
    private UUID accountId;

    @Schema(description = "User profile ID")
    private UUID userProfileId;

    @Schema(description = "Relationship type")
    private RelationshipType relationshipType;

    @Schema(description = "Permissions")
    private Set<AccountPermission> permissions;

    @Schema(description = "Percentage ownership")
    private BigDecimal percentageOwnership;

    @Schema(description = "Is beneficiary")
    private Boolean isBeneficiary;

    @Schema(description = "Beneficiary percentage")
    private BigDecimal beneficiaryPercentage;

    @Schema(description = "Is active")
    private Boolean isActive;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Created by")
    private String createdBy;

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
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

    public Set<AccountPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<AccountPermission> permissions) {
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
}
