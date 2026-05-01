package com.openfinova.banking.gl.api.entity;

/**
 * Types of automatic clearing rules for suspense items.
 *
 * Reduces manual workload by automatically clearing items
 * that match predictable patterns.
 */
public enum ClearingRuleType {

    /**
     * Match transaction reference against regex pattern.
     * E.g., "REF-\\d{6}" matches to specific account.
     */
    PATTERN_MATCH,

    /**
     * Auto-clear items older than threshold to designated account.
     * E.g., items >90 days cleared to "Unclaimed Funds" account.
     */
    AGE_THRESHOLD,

    /**
     * Auto-clear immaterial amounts below threshold.
     * E.g., amounts <$5 cleared to "Sundry Income/Expense".
     */
    AMOUNT_THRESHOLD,

    /**
     * Clear based on source system identifier.
     * E.g., all items from "ATM_SYSTEM" to specific clearing account.
     */
    SOURCE_SYSTEM,

    /**
     * Apply standing instruction for recurring patterns.
     * E.g., weekly payroll file → always clear to payroll clearing account.
     */
    STANDING_INSTRUCTION;
}
