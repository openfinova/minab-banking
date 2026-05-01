package com.openfinova.banking.tp.api.entity;

/**
 * Enumeration of transaction processing states in the transaction lifecycle.
 */
public enum TransactionStatus {
    /**
     * Transaction has been created and initial validation passed
     */
    INITIATED("Transaction Initiated"),

    /**
     * Balance reservation has been created, waiting for external authorization
     */
    PENDING_RESERVATION("Pending Balance Reservation"),

    /**
     * External authorization completed successfully, ready for GL posting
     */
    AUTHORIZED("Authorized"),

    /**
     * Transaction has been posted to the General Ledger
     */
    POSTED("Posted to General Ledger"),

    /**
     * Transaction has been reversed through compensation workflow
     */
    REVERSED("Reversed"),

    /**
     * Transaction processing failed at some stage
     */
    FAILED("Failed");

    private final String displayName;

    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this is a terminal state (no further transitions possible)
     *
     * @return true if this is a terminal state
     */
    public boolean isTerminal() {
        return this == POSTED || this == REVERSED || this == FAILED;
    }

    /**
     * Checks if this status indicates successful completion
     *
     * @return true if transaction completed successfully
     */
    public boolean isSuccessful() {
        return this == POSTED;
    }

    /**
     * Checks if this status indicates failure
     *
     * @return true if transaction failed
     */
    public boolean isFailed() {
        return this == FAILED;
    }

    /**
     * Gets valid next states from current state according to the business state
     * machine.
     *
     * Transition Rules:
     * - INITIATED: Can start reservation or fail immediately.
     * - PENDING_RESERVATION: Can be authorized after success or fail if
     * reservation/auth fails.
     * - AUTHORIZED: Can be posted to GL, reversed (voided), or fail during posting.
     * - POSTED: Only reversal is possible.
     * - REVERSED/FAILED: Terminal states. No further transitions allowed.
     *
     * @return array of valid next states
     */
    public TransactionStatus[] getValidNextStates() {
        return switch (this) {
            case INITIATED -> new TransactionStatus[] { PENDING_RESERVATION, FAILED };
            case PENDING_RESERVATION -> new TransactionStatus[] { AUTHORIZED, FAILED };
            case AUTHORIZED -> new TransactionStatus[] { POSTED, REVERSED, FAILED };
            case POSTED -> new TransactionStatus[] { REVERSED };
            case REVERSED, FAILED -> new TransactionStatus[] {};
        };
    }

    /**
     * Validates if transition to target state is allowed
     *
     * @param targetState the state to transition to
     * @return true if transition is valid
     */
    public boolean canTransitionTo(TransactionStatus targetState) {
        TransactionStatus[] validStates = getValidNextStates();
        for (TransactionStatus validState : validStates) {
            if (validState == targetState) {
                return true;
            }
        }
        return false;
    }
}