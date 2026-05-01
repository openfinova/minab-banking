package com.openfinova.banking.gl.api.entity;

/**
 * Enumeration of GL transaction types with associated reference ID prefixes.
 * Used to distinguish between user-initiated and system-generated transactions.
 */
public enum GLTransactionType {
    /**
     * User-initiated transaction - manually entered by users
     * No prefix, uses user-provided reference ID
     */
    USER_INITIATED("", "User-initiated transaction", false),

    /**
     * Reversal transaction - system-generated to reverse another transaction
     * Prefix: REV-
     */
    REVERSAL("REV-", "Transaction reversal", true),

    /**
     * Period closing transaction - system-generated for fiscal period end
     * Prefix: CLOSING-
     */
    PERIOD_CLOSING("CLOSING-", "Period-end closing entry", true),

    /**
     * Currency revaluation transaction - system-generated for FX revaluation
     * Prefix: REVAL-
     */
    CURRENCY_REVALUATION("REVAL-", "Currency revaluation entry", true);

    private final String prefix;
    private final String description;
    private final boolean systemGenerated;

    GLTransactionType(String prefix, String description, boolean systemGenerated) {
        this.prefix = prefix;
        this.description = description;
        this.systemGenerated = systemGenerated;
    }

    /**
     * Gets the reference ID prefix for this transaction type.
     *
     * @return the reference ID prefix (empty string for user-initiated)
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Gets the description of this transaction type.
     *
     * @return the transaction type description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this transaction type is system-generated.
     *
     * @return true if system-generated, false if user-initiated
     */
    public boolean isSystemGenerated() {
        return systemGenerated;
    }

    /**
     * Generates a reference ID for this transaction type.
     *
     * @param suffix the suffix to append to the prefix
     * @return the complete reference ID
     */
    public String generateReferenceId(String suffix) {
        if (prefix.isEmpty()) {
            return suffix; // USER_INITIATED uses suffix as-is
        }
        return prefix + suffix;
    }

    /**
     * Determines the transaction type by analyzing the reference ID.
     *
     * @param referenceId the reference ID to analyze
     * @return the matching transaction type, or USER_INITIATED if no match
     */
    public static GLTransactionType fromReferenceId(String referenceId) {
        if (referenceId == null || referenceId.isEmpty()) {
            return USER_INITIATED;
        }

        for (GLTransactionType type : values()) {
            if (!type.prefix.isEmpty() && referenceId.startsWith(type.prefix)) {
                return type;
            }
        }

        return USER_INITIATED;
    }

    /**
     * Checks if a reference ID represents a system-generated transaction.
     *
     * @param referenceId the reference ID to check
     * @return true if the reference ID belongs to a system-generated transaction
     */
    public static boolean isSystemGeneratedReferenceId(String referenceId) {
        return fromReferenceId(referenceId).isSystemGenerated();
    }
}
