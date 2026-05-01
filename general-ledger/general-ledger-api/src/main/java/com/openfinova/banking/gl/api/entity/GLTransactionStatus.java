package com.openfinova.banking.gl.api.entity;

/**
 * Status of a GL transaction in its lifecycle.
 * Supports maker-checker approval workflow for manual journal entries.
 */
public enum GLTransactionStatus {

    /**
     * Transaction is being drafted but not yet submitted.
     * Can be edited and deleted by the creator.
     */
    DRAFT,

    /**
     * Transaction has been submitted and is awaiting approval.
     * Cannot be edited while in this state.
     */
    PENDING_APPROVAL,

    /**
     * Transaction has been approved and posted to the general ledger.
     * Balances have been updated. This is the final state for successful transactions.
     */
    POSTED,

    /**
     * Transaction has been reversed by a counter-transaction.
     * Original transaction remains in POSTED status, reversal is a separate transaction.
     */
    REVERSED,

    /**
     * Transaction approval was rejected by an approver.
     * Transaction did not post to GL. Creator can view rejection reason and create new draft.
     */
    REJECTED,

    /**
     * Transaction was withdrawn/cancelled by the creator before approval.
     * Did not post to GL.
     */
    CANCELLED;

    /**
     * Check if transaction is in a final state (cannot be modified).
     */
    public boolean isFinal() {
        return this == POSTED || this == REVERSED || this == REJECTED || this == CANCELLED;
    }

    /**
     * Check if transaction can be edited.
     */
    public boolean isEditable() {
        return this == DRAFT;
    }

    /**
     * Check if transaction can be submitted for approval.
     */
    public boolean canSubmit() {
        return this == DRAFT;
    }

    /**
     * Check if transaction can be approved.
     */
    public boolean canApprove() {
        return this == PENDING_APPROVAL;
    }

    /**
     * Check if transaction is posted to GL (affects balances).
     * NOTE: REVERSED is excluded — a reversed transaction's balance impact has been
     * unwound by its counter-entry; queries that sum debits/credits must not
     * double-count it alongside the reversal entry.
     */
    public boolean isPosted() {
        return this == POSTED;
    }
}