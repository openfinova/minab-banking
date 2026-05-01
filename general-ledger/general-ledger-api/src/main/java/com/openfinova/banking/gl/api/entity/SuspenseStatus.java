package com.openfinova.banking.gl.api.entity;

/**
 * Status of a suspense item in its lifecycle.
 *
 * Suspense accounts are temporary holding accounts used when transactions
 * cannot be posted to their correct destination due to missing or unclear information.
 *
 * Regulatory Context:
 * - Basel Committee expects active management of suspense items
 * - Most regulators require resolution within 30-90 days
 * - Large/aged suspense balances indicate control weaknesses
 */
public enum SuspenseStatus {

    /**
     * Item newly posted to suspense, awaiting initial review.
     * Standard for items 0-7 days old.
     */
    PENDING,

    /**
     * Item is being actively investigated to determine correct account.
     * Assigned to specific user/team.
     */
    UNDER_INVESTIGATION,

    /**
     * Item has exceeded aging threshold and escalated to management.
     * Typically items >30 or >60 days based on policy.
     */
    ESCALATED,

    /**
     * Item successfully cleared from suspense to correct account.
     * Final state - item resolved.
     */
    CLEARED,

    /**
     * Item cleared automatically by system rules.
     * E.g., immaterial amounts, pattern-matched transactions.
     */
    AUTO_CLEARED,

    /**
     * Item written off after investigation (unclaimed, untraceable).
     * Requires senior management approval per regulations.
     */
    WRITTEN_OFF,

    /**
     * Item cancelled/reversed (original transaction voided).
     * Different from clearing - no valid transaction to post.
     */
    CANCELLED;

    /**
     * Check if the suspense item is still active (needs resolution).
     */
    public boolean isActive() {
        return this == PENDING || this == UNDER_INVESTIGATION || this == ESCALATED;
    }

    /**
     * Check if the suspense item is resolved (no longer pending).
     */
    public boolean isResolved() {
        return this == CLEARED || this == AUTO_CLEARED || this == WRITTEN_OFF || this == CANCELLED;
    }
}
