package com.openfinova.banking.identity.entity;

/**
 * Status of an individual step within an {@link ApprovalWorkflowInstance}.
 */
public enum ApprovalWorkflowStepStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SKIPPED
}
