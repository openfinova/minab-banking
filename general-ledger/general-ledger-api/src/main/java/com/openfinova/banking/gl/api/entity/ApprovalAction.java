package com.openfinova.banking.gl.api.entity;

/**
 * Action taken during transaction approval workflow.
 */
public enum ApprovalAction {

    /**
     * Transaction was approved and will be posted to GL.
     */
    APPROVED,

    /**
     * Transaction was rejected and will not be posted.
     * Rejection reason should be documented in comments.
     */
    REJECTED,

    /**
     * Transaction was returned to maker for corrections.
     * Maker can edit and resubmit.
     */
    RETURNED
}
