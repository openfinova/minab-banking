package com.openfinova.banking.loan.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.openfinova.banking.loan.api.entity.CollectionActivityType;
import com.openfinova.banking.loan.api.entity.CollectionStatus;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Collection Activity entity tracking collection efforts for delinquent loans.
 * Records all collection actions, notes, and follow-up schedules.
 */
@Entity
@Table(name = "collection_activities", indexes = {
        @Index(name = "idx_collection_activities_account", columnList = "loan_account_id"),
        @Index(name = "idx_collection_activities_date", columnList = "activity_date"),
        @Index(name = "idx_collection_activities_type", columnList = "activity_type"),
        @Index(name = "idx_collection_activities_status", columnList = "status"),
        @Index(name = "idx_collection_activities_follow_up", columnList = "follow_up_date") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CollectionActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    @NotNull(message = "Loan account is required")
    private LoanAccount loanAccount;

    @NotNull(message = "Activity date is required")
    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    @NotNull(message = "Activity type is required")
    private CollectionActivityType activityType;

    /**
     * Detailed notes about the collection activity.
     */
    @NotBlank(message = "Notes are required")
    @Column(nullable = false, length = 1000)
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    /**
     * Collection agent or user assigned to this activity.
     */
    @Column(name = "assigned_to", length = 100)
    @Size(max = 100, message = "Assigned to must not exceed 100 characters")
    private String assignedTo;

    /**
     * Scheduled date for follow-up action.
     */
    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Status is required")
    private CollectionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public CollectionActivity() {
    }

    // Business Logic
    public boolean isCompleted() {
        return CollectionStatus.COMPLETED.equals(status);
    }

    public boolean isPending() {
        return CollectionStatus.PENDING.equals(status);
    }

    public boolean requiresFollowUp(LocalDate currentDate) {
        return followUpDate != null && !followUpDate.isBefore(currentDate);
    }

    /**
     * Validates if the collection activity can transition to the specified status.
     *
     * @param newStatus the target status
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(CollectionStatus newStatus) {
        if (this.status == newStatus) {
            return true; // Same status is always valid
        }

        return switch (this.status) {
            case PENDING -> newStatus == CollectionStatus.IN_PROGRESS || newStatus == CollectionStatus.ESCALATED
                    || newStatus == CollectionStatus.CLOSED;
            case IN_PROGRESS -> newStatus == CollectionStatus.COMPLETED || newStatus == CollectionStatus.ESCALATED
                    || newStatus == CollectionStatus.CLOSED;
            case ESCALATED -> newStatus == CollectionStatus.IN_PROGRESS || newStatus == CollectionStatus.COMPLETED
                    || newStatus == CollectionStatus.CLOSED;
            case COMPLETED, CLOSED -> false; // Terminal states
        };
    }

    /**
     * Gets a descriptive error message for invalid status transitions.
     *
     * @param newStatus the attempted target status
     * @return error message describing why the transition is invalid
     */
    public String getTransitionErrorMessage(CollectionStatus newStatus) {
        if (this.status == newStatus) {
            return String.format("Collection activity is already in %s status", newStatus);
        }

        return switch (this.status) {
            case PENDING -> String.format(
                    "Cannot transition from PENDING to %s. Valid transitions: IN_PROGRESS, ESCALATED, CLOSED",
                    newStatus);
            case IN_PROGRESS -> String.format(
                    "Cannot transition from IN_PROGRESS to %s. Valid transitions: COMPLETED, ESCALATED, CLOSED",
                    newStatus);
            case ESCALATED -> String.format(
                    "Cannot transition from ESCALATED to %s. Valid transitions: IN_PROGRESS, COMPLETED, CLOSED",
                    newStatus);
            case COMPLETED -> "Cannot change status of COMPLETED activity. This is a terminal state";
            case CLOSED -> "Cannot change status of CLOSED activity. This is a terminal state";
        };
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LoanAccount getLoanAccount() {
        return loanAccount;
    }

    public void setLoanAccount(LoanAccount loanAccount) {
        this.loanAccount = loanAccount;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(LocalDate activityDate) {
        this.activityDate = activityDate;
    }

    public CollectionActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(CollectionActivityType activityType) {
        this.activityType = activityType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDate followUpDate) {
        this.followUpDate = followUpDate;
    }

    public CollectionStatus getStatus() {
        return status;
    }

    public void setStatus(CollectionStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
