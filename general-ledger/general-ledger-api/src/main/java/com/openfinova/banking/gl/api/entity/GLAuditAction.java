package com.openfinova.banking.gl.api.entity;

/**
 * Enumeration of audit action types for tracking changes to GL entities.
 * This enum supports regulatory compliance requirements (SOX, Basel III, IFRS)
 * by categorizing all types of operations performed on general ledger data.
 */
public enum GLAuditAction {
    /**
     * Entity creation
     */
    CREATE,

    /**
     * Entity modification (field updates)
     */
    UPDATE,

    /**
     * Entity deletion or soft delete
     */
    DELETE,

    /**
     * Status change (e.g., ACTIVE to INACTIVE, PENDING to POSTED)
     */
    STATUS_CHANGE,

    /**
     * Transaction reversal - HIGH RISK, requires mandatory reason
     */
    REVERSE,

    /**
     * Account or entity reactivation
     */
    REACTIVATE,

    /**
     * Fiscal period closing
     */
    PERIOD_CLOSE,

    /**
     * Fiscal period reopening - HIGH RISK, requires mandatory reason
     */
    PERIOD_REOPEN,

    /**
     * Manual balance adjustment - HIGH RISK, requires mandatory reason
     */
    BALANCE_ADJUSTMENT,

    /**
     * Transaction or operation approval
     */
    APPROVAL,

    /**
     * Transaction or operation rejection - HIGH RISK, requires mandatory reason
     */
    REJECTION,

    /**
     * Reconciliation completion
     */
    RECONCILIATION,

    /**
     * Bulk data import operation
     */
    IMPORT,

    /**
     * Data export operation (potential data leakage risk)
     */
    EXPORT,

    /**
     * System configuration change
     */
    CONFIG_CHANGE
}
