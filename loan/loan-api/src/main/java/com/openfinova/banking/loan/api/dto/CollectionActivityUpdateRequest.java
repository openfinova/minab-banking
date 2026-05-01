package com.openfinova.banking.loan.api.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating a collection activity.
 */
public class CollectionActivityUpdateRequest {

    @NotBlank(message = "Notes are required")
    private String notes;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate followUpDate;

    private String updatedBy;

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

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
