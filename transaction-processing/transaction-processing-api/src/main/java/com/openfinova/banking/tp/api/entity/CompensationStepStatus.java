package com.openfinova.banking.tp.api.entity;

/**
 * Enumeration of compensation step statuses.
 * Tracks the state of individual steps within a compensation workflow.
 *
 * Requirements addressed:
 * - Maintain compensation workflow state for each transaction
 * - Automatic retry logic with exponential backoff
 */
public enum CompensationStepStatus {

    /**
     * Step is pending execution
     */
    PENDING("Pending"),

    /**
     * Step is currently being executed
     */
    IN_PROGRESS("In Progress"),

    /**
     * Step completed successfully
     */
    COMPLETED("Completed"),

    /**
     * Step failed and may be retried
     */
    FAILED("Failed"),

    /**
     * Step was skipped due to conditions or dependencies
     */
    SKIPPED("Skipped");

    private final String displayName;

    CompensationStepStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this status represents a terminal state for the step
     *
     * @return true if the step cannot be processed further
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == SKIPPED;
    }

    /**
     * Checks if this status represents a successful completion
     *
     * @return true if the step completed successfully
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    /**
     * Checks if this status represents a failed state
     *
     * @return true if the step failed
     */
    public boolean isFailed() {
        return this == FAILED;
    }
}