package com.openfinova.banking.loan.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating overdue status.
 */
public class LoanScheduleOverdueUpdateRequest {

    @NotNull(message = "Overdue flag is required")
    private Boolean isOverdue;

    @NotNull(message = "Days past due is required")
    @Min(value = 0, message = "Days past due cannot be negative")
    private Integer daysPastDue;

    public Boolean getIsOverdue() {
        return isOverdue;
    }

    public void setIsOverdue(Boolean isOverdue) {
        this.isOverdue = isOverdue;
    }

    public Integer getDaysPastDue() {
        return daysPastDue;
    }

    public void setDaysPastDue(Integer daysPastDue) {
        this.daysPastDue = daysPastDue;
    }
}
