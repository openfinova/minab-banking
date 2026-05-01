package com.openfinova.banking.gl.api.entity;

/**
 * Escalation severity levels for aged suspense items.
 *
 * Regulatory Best Practice:
 * - 30 days: Supervisor review
 * - 60 days: Manager approval
 * - 90 days: Senior management notification
 * - 180+ days: Executive/board reporting
 */
public enum EscalationLevel {

    /**
     * Level 1: Team Lead/Supervisor review.
     * Typically 30 days old.
     */
    LEVEL_1_SUPERVISOR,

    /**
     * Level 2: Department Manager escalation.
     * Typically 60 days old.
     */
    LEVEL_2_MANAGER,

    /**
     * Level 3: Senior Management notification.
     * Typically 90 days old.
     */
    LEVEL_3_SENIOR_MANAGEMENT,

    /**
     * Level 4: Executive/C-Level escalation.
     * Typically 120+ days old.
     */
    LEVEL_4_EXECUTIVE,

    /**
     * Critical: Board/Audit Committee reporting.
     * Typically 180+ days or material amount.
     */
    CRITICAL_BOARD_LEVEL;

    /**
     * Get typical age threshold in days for this escalation level.
     */
    public int getTypicalAgeDays() {
        return switch (this) {
            case LEVEL_1_SUPERVISOR -> 30;
            case LEVEL_2_MANAGER -> 60;
            case LEVEL_3_SENIOR_MANAGEMENT -> 90;
            case LEVEL_4_EXECUTIVE -> 120;
            case CRITICAL_BOARD_LEVEL -> 180;
        };
    }
}
