package com.openfinova.banking.tp.api.entity;

/**
 * Enumeration of compensation workflow statuses.
 * Tracks the state of compensation workflows through their lifecycle.
 *
 * Requirements addressed:
 * - Maintain compensation workflow state for each transaction
 * - Automatic retry logic with exponential backoff
 * - Escalation to manual review queue
 */
public enum CompensationStatus {

    /**
     * Compensation workflow has been initiated but not yet started
     */
    INITIATED("Initiated"),

    /**
     * Compensation workflow is currently in progress
     */
    IN_PROGRESS("In Progress"),

    /**
     * Compensation workflow completed successfully
     */
    COMPLETED("Completed"),

    /**
     * Compensation workflow failed and is awaiting retry
     */
    FAILED("Failed"),

    /**
     * Compensation workflow has been escalated to manual review
     */
    ESCALATED("Escalated"),

    /**
     * Compensation workflow has been cancelled
     */
    CANCELLED("Cancelled");

    private final String displayName;

    CompensationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this status represents a terminal state
     *
     * @return true if the workflow cannot proceed further
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == ESCALATED || this == CANCELLED;
    }

    /**
     * Checks if this status represents a successful completion
     *
     * @return true if the workflow completed successfully
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    /**
     * Checks if this status represents a failed state
     *
     * @return true if the workflow failed
     */
    public boolean isFailed() {
        return this == FAILED;
    }

    /**
     * Checks if this status allows for retry
     *
     * @return true if the workflow can be retried
     */
    public boolean canRetry() {
        return this == FAILED;
    }

    /**
     * Checks if this status can transition to the target status
     *
     * @param targetStatus the target status
     * @return true if transition is valid
     */
    public boolean canTransitionTo(CompensationStatus targetStatus) {
        return switch (this) {
            case INITIATED -> targetStatus == IN_PROGRESS || targetStatus == CANCELLED;
            case IN_PROGRESS -> targetStatus == COMPLETED || targetStatus == FAILED || targetStatus == ESCALATED;
            case FAILED -> targetStatus == IN_PROGRESS || targetStatus == ESCALATED || targetStatus == CANCELLED;
            case COMPLETED, ESCALATED, CANCELLED -> false; // Terminal states
        };
    }
}