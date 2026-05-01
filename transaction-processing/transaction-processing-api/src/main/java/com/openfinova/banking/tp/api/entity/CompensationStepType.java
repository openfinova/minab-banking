package com.openfinova.banking.tp.api.entity;

/**
 * Enumeration of compensation step types.
 * Defines the different types of actions that can be taken during compensation workflows.
 *
 * Requirements addressed:
 * - Maintain compensation workflow state for each transaction
 * - Create reversal transactions in GL with appropriate audit trails
 */
public enum CompensationStepType {

    /**
     * Release all active balance reservations associated with the transaction
     */
    RELEASE_RESERVATIONS("Release Reservations", "Release all active balance reservations"),

    /**
     * Reverse the GL transaction that was posted
     */
    REVERSE_GL_TRANSACTION("Reverse GL Transaction", "Reverse the general ledger transaction"),

    /**
     * Create a compensating transaction to reverse the original transaction
     */
    CREATE_COMPENSATING_TRANSACTION("Create Compensating Transaction",
            "Create a compensating transaction for full reversal"),

    /**
     * Notify external systems about the transaction reversal
     */
    NOTIFY_EXTERNAL_SYSTEMS("Notify External Systems", "Notify external systems of transaction reversal"),

    /**
     * Refund fees charged for the transaction
     */
    REFUND_FEES("Refund Fees", "Refund all fees charged for the transaction"),

    /**
     * Update account balances to reflect the reversal
     */
    UPDATE_ACCOUNT_BALANCES("Update Account Balances", "Update account balances to reflect the reversal"),

    /**
     * Send notification to customer about the reversal
     */
    NOTIFY_CUSTOMER("Notify Customer", "Send notification to customer about the transaction reversal");

    private final String displayName;
    private final String description;

    CompensationStepType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this step type requires GL integration
     *
     * @return true if GL integration is required
     */
    public boolean requiresGLIntegration() {
        return this == REVERSE_GL_TRANSACTION;
    }

    /**
     * Checks if this step type requires external system communication
     *
     * @return true if external communication is required
     */
    public boolean requiresExternalCommunication() {
        return this == NOTIFY_EXTERNAL_SYSTEMS || this == NOTIFY_CUSTOMER;
    }

    /**
     * Checks if this step type is critical for data consistency
     *
     * @return true if the step is critical
     */
    public boolean isCritical() {
        return this == RELEASE_RESERVATIONS || this == REVERSE_GL_TRANSACTION || this == UPDATE_ACCOUNT_BALANCES;
    }

    /**
     * Gets the default order priority for this step type
     * Lower numbers execute first
     *
     * @return the default execution order
     */
    public int getDefaultOrder() {
        return switch (this) {
            case RELEASE_RESERVATIONS -> 1;
            case REVERSE_GL_TRANSACTION -> 2;
            case UPDATE_ACCOUNT_BALANCES -> 3;
            case REFUND_FEES -> 4;
            case CREATE_COMPENSATING_TRANSACTION -> 5;
            case NOTIFY_EXTERNAL_SYSTEMS -> 6;
            case NOTIFY_CUSTOMER -> 7;
        };
    }
}
