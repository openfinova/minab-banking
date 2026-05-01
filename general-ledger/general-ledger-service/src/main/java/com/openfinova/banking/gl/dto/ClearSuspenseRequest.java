package com.openfinova.banking.gl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request to clear a suspense item to the target account.
 */
public class ClearSuspenseRequest {

    @NotNull(message = "Target account ID is required")
    private UUID targetAccountId;

    @NotBlank(message = "Cleared by is required")
    @Size(max = 100, message = "Cleared by must not exceed 100 characters")
    private String clearedBy;

    @Size(max = 500, message = "Resolution notes must not exceed 500 characters")
    private String resolutionNotes;

    // Constructors

    public ClearSuspenseRequest() {
    }

    public ClearSuspenseRequest(UUID targetAccountId, String clearedBy, String resolutionNotes) {
        this.targetAccountId = targetAccountId;
        this.clearedBy = clearedBy;
        this.resolutionNotes = resolutionNotes;
    }

    // Getters and Setters

    public UUID getTargetAccountId() {
        return targetAccountId;
    }

    public void setTargetAccountId(UUID targetAccountId) {
        this.targetAccountId = targetAccountId;
    }

    public String getClearedBy() {
        return clearedBy;
    }

    public void setClearedBy(String clearedBy) {
        this.clearedBy = clearedBy;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    @Override
    public String toString() {
        return "ClearSuspenseRequest{" + "targetAccountId=" + targetAccountId + ", clearedBy='" + clearedBy + '\''
                + '}';
    }
}
