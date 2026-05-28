package com.openfinova.banking.loan.service;

import com.openfinova.banking.loan.api.dto.CollectionActivityReportResponse;
import com.openfinova.banking.loan.api.dto.CollectionActivityResponse;
import com.openfinova.banking.loan.api.entity.CollectionActivityType;
import com.openfinova.banking.loan.api.entity.CollectionStatus;
import com.openfinova.banking.loan.entity.CollectionActivity;
import com.openfinova.banking.loan.entity.LoanAccount;
import com.openfinova.banking.loan.mapper.CollectionActivityMapper;
import com.openfinova.banking.loan.repository.CollectionActivityRepository;
import com.openfinova.banking.loan.repository.LoanAccountRepository;
import com.openfinova.banking.setup.api.DateTimeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of CollectionActivityService for managing loan collection activities.
 *
 * This service handles:
 * - Recording collection activities (calls, visits, letters, etc.)
 * - Tracking activity status and follow-ups
 * - Scheduling and managing follow-up activities
 * - Generating collection activity reports
 * - Monitoring overdue follow-ups
 *
 * Collection activities are used to track all interactions with borrowers
 * regarding loan repayment and collection efforts.
 *
 * @see CollectionActivityService
 * @see CollectionActivity
 * @see com.openfinova.banking.loan.api.entity.CollectionStatus
 */
@Service
@Transactional
public class CollectionActivityService {

    private final CollectionActivityRepository activityRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final DateTimeService dateTimeService;

    public CollectionActivityService(CollectionActivityRepository activityRepository,
            LoanAccountRepository loanAccountRepository, DateTimeService dateTimeService) {
        this.activityRepository = activityRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Creates a new collection activity for a loan account.
     *
     * The activity is created with PENDING status by default. This method records
     * all collection efforts such as phone calls, visits, letters, or legal actions.
     *
     * @param loanAccountId the ID of the loan account
     * @param activityType the type of collection activity
     * @param activityDate the date when the activity occurred
     * @param notes detailed notes about the activity
     * @param followUpDate optional date for follow-up (can be null)
     * @param createdBy the user who created the activity
     * @return the created collection activity
     * @throws IllegalArgumentException if loan account not found
     */
    public CollectionActivity createActivity(UUID loanAccountId, CollectionActivityType activityType,
            LocalDate activityDate, String notes, LocalDate followUpDate, String createdBy) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));

        CollectionActivity activity = new CollectionActivity();
        activity.setLoanAccount(loanAccount);
        activity.setActivityType(activityType);
        activity.setActivityDate(activityDate);
        activity.setNotes(notes);
        activity.setFollowUpDate(followUpDate);
        activity.setStatus(CollectionStatus.PENDING);

        return activityRepository.save(activity);
    }

    /**
     * Retrieves a collection activity by its unique identifier.
     *
     * @param id the activity ID
     * @return Optional containing the activity if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<CollectionActivity> getActivityById(UUID id) {
        return activityRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<CollectionActivity> getActivityForLoanAccount(UUID loanAccountId, UUID activityId) {
        return activityRepository.findById(activityId)
                .filter(a -> a.getLoanAccount() != null && loanAccountId.equals(a.getLoanAccount().getId()));
    }

    /**
     * Retrieves all collection activities for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of all collection activities for the loan account
     */
    @Transactional(readOnly = true)
    public List<CollectionActivity> getActivitiesByLoanAccount(UUID loanAccountId) {
        return activityRepository.findByLoanAccountId(loanAccountId);
    }

    /**
     * Retrieves the most recent collection activity for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return Optional containing the latest activity if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<CollectionActivity> getLatestActivity(UUID loanAccountId) {
        return activityRepository.findTopByLoanAccountIdOrderByActivityDateDesc(loanAccountId);
    }

    /**
     * Retrieves collection activities by type with pagination.
     *
     * @param activityType the type of collection activity to filter by
     * @param pageable pagination parameters
     * @return page of activities matching the specified type
     */
    @Transactional(readOnly = true)
    public Page<CollectionActivity> getActivitiesByType(CollectionActivityType activityType, Pageable pageable) {
        return activityRepository.findByActivityType(activityType, pageable);
    }

    /**
     * Retrieves collection activities by status with pagination.
     *
     * @param status the collection status to filter by
     * @param pageable pagination parameters
     * @return page of activities matching the specified status
     */
    @Transactional(readOnly = true)
    public Page<CollectionActivity> getActivitiesByStatus(CollectionStatus status, Pageable pageable) {
        return activityRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CollectionActivity> getActivitiesByLoanAccountAndStatus(UUID loanAccountId, CollectionStatus status,
            Pageable pageable) {
        return activityRepository.findByLoanAccount_IdAndStatus(loanAccountId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CollectionActivity> getActivitiesByLoanAccountAndDateRange(UUID loanAccountId, LocalDate startDate,
            LocalDate endDate, Pageable pageable) {
        return activityRepository
                .findByLoanAccountIdAndActivityDateBetweenPage(loanAccountId, startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public long countPendingActivitiesForLoanAccount(UUID loanAccountId) {
        return activityRepository.countByLoanAccount_IdAndStatus(loanAccountId, CollectionStatus.PENDING);
    }

    /**
     * Retrieves all pending collection activities with pagination.
     *
     * @param pageable pagination parameters
     * @return page of pending activities
     */
    @Transactional(readOnly = true)
    public Page<CollectionActivity> getPendingActivities(Pageable pageable) {
        return activityRepository.findByStatus(CollectionStatus.PENDING, pageable);
    }

    /**
     * Retrieves activities scheduled for follow-up on a specific date.
     *
     * @param followUpDate the date to check for follow-ups
     * @param pageable pagination parameters
     * @return page of activities with follow-up on the specified date
     */
    @Transactional(readOnly = true)
    public Page<CollectionActivity> getActivitiesWithFollowUpOnDate(LocalDate followUpDate, Pageable pageable) {
        return activityRepository.findByFollowUpDate(followUpDate, pageable);
    }

    /**
     * Retrieves overdue follow-up activities.
     *
     * Returns pending activities where the follow-up date is before the current date,
     * indicating they are overdue and need immediate attention.
     *
     * @param currentDate the current date to compare against
     * @param pageable pagination parameters
     * @return page of overdue pending activities
     */
    @Transactional(readOnly = true)
    public Page<CollectionActivity> getOverdueFollowUps(LocalDate currentDate, Pageable pageable) {
        return activityRepository.findByFollowUpDateBeforeAndStatus(currentDate, CollectionStatus.PENDING, pageable);
    }

    /**
     * Retrieves activities within a date range with pagination.
     *
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (inclusive)
     * @param pageable pagination parameters
     * @return page of activities within the specified date range
     */
    @Transactional(readOnly = true)
    public Page<CollectionActivity> getActivitiesByDateRange(LocalDate startDate, LocalDate endDate,
            Pageable pageable) {
        return activityRepository.findByActivityDateBetween(startDate, endDate, pageable);
    }

    /**
     * Updates an existing collection activity.
     *
     * Allows updating the notes and follow-up date of an activity.
     *
     * @param activityId the ID of the activity to update
     * @param notes updated notes about the activity
     * @param followUpDate updated follow-up date (can be null)
     * @param updatedBy the user performing the update
     * @return the updated collection activity
     * @throws IllegalArgumentException if activity not found
     */
    public CollectionActivity updateActivityForLoanAccount(UUID loanAccountId, UUID activityId, String notes,
            LocalDate followUpDate, String updatedBy) {
        requireActivityOnLoan(loanAccountId, activityId);
        return updateActivity(activityId, notes, followUpDate, updatedBy);
    }

    public CollectionActivity updateActivity(UUID activityId, String notes, LocalDate followUpDate, String updatedBy) {
        CollectionActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + activityId));

        activity.setNotes(notes);
        activity.setFollowUpDate(followUpDate);

        return activityRepository.save(activity);
    }

    /**
     * Updates the status of a collection activity with state transition validation.
     *
     * This method validates that the status transition is allowed according
     * to the collection activity state machine rules:
     * - PENDING → IN_PROGRESS, ESCALATED, CLOSED
     * - IN_PROGRESS → COMPLETED, ESCALATED, CLOSED
     * - ESCALATED → IN_PROGRESS, COMPLETED, CLOSED
     * - COMPLETED → (no transitions allowed)
     * - CLOSED → (no transitions allowed)
     *
     * @param activityId the ID of the activity to update
     * @param newStatus the new status to transition to
     * @param updatedBy the user performing the update
     * @return the updated collection activity
     * @throws IllegalArgumentException if any parameter is invalid or activity not found
     * @throws IllegalStateException if the status transition is not allowed
     */
    public CollectionActivity updateActivityStatusForLoanAccount(UUID loanAccountId, UUID activityId,
            CollectionStatus newStatus, String updatedBy) {
        requireActivityOnLoan(loanAccountId, activityId);
        return updateActivityStatus(activityId, newStatus, updatedBy);
    }

    public CollectionActivity updateActivityStatus(UUID activityId, CollectionStatus newStatus, String updatedBy) {
        if (activityId == null) {
            throw new IllegalArgumentException("Activity ID cannot be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        if (updatedBy == null || updatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Updated by cannot be null or empty");
        }

        CollectionActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Activity not found: %s", activityId)));

        // Validate state transition
        if (!activity.canTransitionTo(newStatus)) {
            throw new IllegalStateException(activity.getTransitionErrorMessage(newStatus));
        }

        activity.setStatus(newStatus);
        return activityRepository.save(activity);
    }

    /**
     * Marks a collection activity as completed.
     *
     * Sets the activity status to COMPLETED and appends the outcome to the notes.
     * Validates that the activity can transition to COMPLETED status.
     *
     * @param activityId the ID of the activity to complete
     * @param outcome the outcome or result of the activity
     * @param completedBy the user who completed the activity
     * @return the completed collection activity
     * @throws IllegalArgumentException if any parameter is invalid or activity not found
     * @throws IllegalStateException if activity cannot transition to COMPLETED status
     */
    public CollectionActivity completeActivityForLoanAccount(UUID loanAccountId, UUID activityId, String outcome,
            String completedBy) {
        requireActivityOnLoan(loanAccountId, activityId);
        return completeActivity(activityId, outcome, completedBy);
    }

    public CollectionActivity completeActivity(UUID activityId, String outcome, String completedBy) {
        if (activityId == null) {
            throw new IllegalArgumentException("Activity ID cannot be null");
        }
        if (outcome == null || outcome.trim().isEmpty()) {
            throw new IllegalArgumentException("Outcome cannot be null or empty");
        }
        if (completedBy == null || completedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Completed by cannot be null or empty");
        }

        CollectionActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Activity not found: %s", activityId)));

        // Validate state transition
        if (!activity.canTransitionTo(CollectionStatus.COMPLETED)) {
            throw new IllegalStateException(activity.getTransitionErrorMessage(CollectionStatus.COMPLETED));
        }

        activity.setStatus(CollectionStatus.COMPLETED);
        // Append outcome to notes since there's no separate outcome field
        String updatedNotes = String.format("%s\nOutcome: %s", activity.getNotes(), outcome);
        activity.setNotes(updatedNotes);

        return activityRepository.save(activity);
    }

    /**
     * Schedules a follow-up activity based on an existing activity.
     *
     * Creates a new collection activity as a follow-up to an existing one.
     * The new activity is created with PENDING status and references the original activity.
     *
     * Validation rules:
     * - Activity ID must not be null
     * - Follow-up date must not be null
     * - Follow-up date must not be in the past
     * - Follow-up type must not be null
     * - Scheduled by must not be null or empty
     *
     * @param activityId the ID of the original activity
     * @param followUpDate the date for the follow-up activity (must not be in the past)
     * @param followUpType the type of follow-up activity
     * @param scheduledBy the user scheduling the follow-up
     * @return the newly created follow-up activity
     * @throws IllegalArgumentException if any parameter is invalid or activity not found
     */
    public CollectionActivity scheduleFollowUpForLoanAccount(UUID loanAccountId, UUID activityId,
            LocalDate followUpDate, CollectionActivityType followUpType, String scheduledBy) {
        requireActivityOnLoan(loanAccountId, activityId);
        return scheduleFollowUp(activityId, followUpDate, followUpType, scheduledBy);
    }

    public CollectionActivity scheduleFollowUp(UUID activityId, LocalDate followUpDate,
            CollectionActivityType followUpType, String scheduledBy) {
        if (activityId == null) {
            throw new IllegalArgumentException("Activity ID cannot be null");
        }
        if (followUpDate == null) {
            throw new IllegalArgumentException("Follow-up date cannot be null");
        }
        if (followUpDate.isBefore(dateTimeService.today())) {
            throw new IllegalArgumentException("Follow-up date cannot be in the past");
        }
        if (followUpType == null) {
            throw new IllegalArgumentException("Follow-up type cannot be null");
        }
        if (scheduledBy == null || scheduledBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Scheduled by cannot be null or empty");
        }

        CollectionActivity originalActivity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Activity not found: %s", activityId)));

        CollectionActivity followUp = new CollectionActivity();
        followUp.setLoanAccount(originalActivity.getLoanAccount());
        followUp.setActivityType(followUpType);
        followUp.setActivityDate(followUpDate);
        followUp.setStatus(CollectionStatus.PENDING);
        followUp.setNotes(String.format("Follow-up from activity: %s", activityId));

        return activityRepository.save(followUp);
    }

    /**
     * Counts the total number of collection activities for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return the count of collection activities
     */
    @Transactional(readOnly = true)
    public long countActivitiesByLoanAccount(UUID loanAccountId) {
        return activityRepository.countByLoanAccountId(loanAccountId);
    }

    /**
     * Counts collection activities by type for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @param activityType the type of activity to count
     * @return the count of activities matching the specified type
     */
    @Transactional(readOnly = true)
    public long countActivitiesByType(UUID loanAccountId, CollectionActivityType activityType) {
        return activityRepository.countByLoanAccountIdAndActivityType(loanAccountId, activityType);
    }

    /**
     * Checks if a loan account has any pending collection activities.
     *
     * @param loanAccountId the loan account ID
     * @return true if at least one pending activity exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasPendingActivities(UUID loanAccountId) {
        return activityRepository.existsByLoanAccountIdAndStatus(loanAccountId, CollectionStatus.PENDING);
    }

    /**
     * Generates a collection activity report for a loan account within a date range.
     *
     * The report includes:
     * - Total number of activities
     * - Count of completed activities
     * - Count of pending activities
     * - List of all activities in the date range
     *
     * @param loanAccountId the loan account ID
     * @param fromDate the start date of the report period (inclusive)
     * @param toDate the end date of the report period (inclusive)
     * @return a comprehensive collection activity report
     */
    @Transactional(readOnly = true)
    public CollectionActivityReportResponse generateActivityReport(UUID loanAccountId, LocalDate fromDate,
            LocalDate toDate) {
        List<CollectionActivity> activities = activityRepository
                .findByLoanAccountIdAndActivityDateBetween(loanAccountId, fromDate, toDate);

        List<CollectionActivityResponse> activityResponses = activities.stream()
                .map(CollectionActivityMapper::toResponse).toList();

        CollectionActivityReportResponse report = new CollectionActivityReportResponse();
        report.setLoanAccountId(loanAccountId);
        report.setFromDate(fromDate);
        report.setToDate(toDate);
        report.setTotalActivities(activityResponses.size());

        long completed = activities.stream().filter(a -> CollectionStatus.COMPLETED.equals(a.getStatus())).count();
        long pending = activities.stream().filter(a -> CollectionStatus.PENDING.equals(a.getStatus())).count();

        report.setCompletedActivities((int) completed);
        report.setPendingActivities((int) pending);
        report.setActivities(activityResponses);

        return report;
    }

    private void requireActivityOnLoan(UUID loanAccountId, UUID activityId) {
        CollectionActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + activityId));
        if (activity.getLoanAccount() == null || !loanAccountId.equals(activity.getLoanAccount().getId())) {
            throw new IllegalArgumentException("Collection activity does not belong to this loan account");
        }
    }
}
