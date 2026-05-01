package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.CollectionActivityType;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Request DTO for scheduling a follow-up activity.
 */
public class CollectionActivityFollowUpRequest {

    @NotNull(message = "Follow-up date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate followUpDate;

    @NotNull(message = "Follow-up type is required")
    private CollectionActivityType followUpType;

    private String scheduledBy;

    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDate followUpDate) {
        this.followUpDate = followUpDate;
    }

    public CollectionActivityType getFollowUpType() {
        return followUpType;
    }

    public void setFollowUpType(CollectionActivityType followUpType) {
        this.followUpType = followUpType;
    }

    public String getScheduledBy() {
        return scheduledBy;
    }

    public void setScheduledBy(String scheduledBy) {
        this.scheduledBy = scheduledBy;
    }
}
