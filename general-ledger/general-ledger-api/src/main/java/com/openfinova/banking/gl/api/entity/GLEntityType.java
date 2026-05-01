package com.openfinova.banking.gl.api.entity;

/**
 * Enumeration of GL entity types for polymorphic audit trail tracking.
 * Identifies which type of entity is being audited in the audit log.
 */
public enum GLEntityType {
    /**
     * GL Account (chart of accounts)
     */
    GL_ACCOUNT,

    /**
     * GL Transaction (financial transaction with journal entries)
     */
    GL_TRANSACTION,

    /**
     * GL Journal Entry (individual debit/credit entry)
     */
    GL_JOURNAL_ENTRY,

    /**
     * Operational GL Configuration (account type mapping)
     */
    OPERATIONAL_CONFIG,

    /**
     * Fiscal Period (accounting period management)
     */
    FISCAL_PERIOD
}
