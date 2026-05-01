package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.CollectionActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a collection activity.
 */
public class CollectionActivityRequest {

    @NotNull(message = "Loan account ID is required")
    private UUID loanAccountId;

    @NotNull(message = "Activity type is required")
    private CollectionActivityType activityType;

    @NotNull(message = "Activity date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate activityDate;

    @NotBlank(message = "Notes are required")
    private String notes;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate followUpDate;

    private String assignedTo;

    private String createdBy;

    public UUID getLoanAccountId() {
        return loanAccountId;
    }

    public void setLoanAccountId(UUID loanAccountId) {
        this.loanAccountId = loanAccountId;
    }

    public CollectionActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(CollectionActivityType activityType) {
        this.activityType = activityType;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(LocalDate activityDate) {
        this.activityDate = activityDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDate followUpDate) {
        this.followUpDate = followUpDate;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
