package com.openfinova.banking.customer.account.api.dto;

import com.openfinova.banking.customer.account.api.entity.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to add a relationship to an account")
public class AddRelationshipRequest {

    @NotNull(message = "User profile ID is required")
    @Schema(description = "User profile ID", required = true)
    private UUID userProfileId;

    @NotNull(message = "Relationship type is required")
    @Schema(description = "Relationship type", required = true)
    private RelationshipType relationshipType;

    @NotBlank(message = "Created by is required")
    @Schema(description = "User creating the relationship", required = true)
    private String createdBy;

    // Getters and setters
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
