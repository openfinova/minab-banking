package com.openfinova.banking.customer.account.api.entity;

/**
 * Enumeration of account statuses with transition rules and transaction
 * permissions.
 * Defines the lifecycle states of customer accounts and valid state
 * transitions.
 */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    FROZEN,
    CLOSED,
    DORMANT;

    /**
     * Determines if this status can transition to the specified new status.
     *
     * @param newStatus the target status for transition
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(AccountStatus newStatus) {
        return switch (this) {
            case ACTIVE -> newStatus != ACTIVE;
            case SUSPENDED, FROZEN, DORMANT -> newStatus == ACTIVE || newStatus == CLOSED;
            case CLOSED -> false; // No transitions from closed
        };
    }

    /**
     * Determines if accounts with this status can perform transactions.
     *
     * @return true if transactions are allowed, false otherwise
     */
    public boolean allowsTransactions() {
        return this == ACTIVE;
    }
}