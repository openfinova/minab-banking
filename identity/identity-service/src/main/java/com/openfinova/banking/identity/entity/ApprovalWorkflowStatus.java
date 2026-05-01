package com.openfinova.banking.identity.entity;

/**
 * Aggregate state of an {@link ApprovalWorkflowInstance}.
 */
public enum ApprovalWorkflowStatus {
    PENDING,
    IN_PROGRESS,
    APPROVED,
    REJECTED,
    CANCELLED
}
