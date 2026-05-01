package com.openfinova.banking.tp.api.entity;

/**
 * Enumeration of fee types supported by the dynamic fee calculation engine.
 *
 * Requirements addressed:
 * - Percentage-based, fixed amount, and tiered fee structures
 */
public enum FeeType {
    /**
     * Fixed amount fee regardless of transaction amount
     */
    FIXED_AMOUNT("Fixed Amount"),

    /**
     * Percentage-based fee calculated on transaction amount
     */
    PERCENTAGE("Percentage"),

    /**
     * Tiered fee based on transaction amount ranges
     */
    TIERED("Tiered"),

    /**
     * Minimum fee (greater of fixed amount or percentage)
     */
    MINIMUM("Minimum"),

    /**
     * Maximum fee (lesser of fixed amount or percentage)
     */
    MAXIMUM("Maximum"),

    /**
     * Flat fee per transaction regardless of amount
     */
    FLAT("Flat"),

    /**
     * No fee (promotional or waived)
     */
    NONE("No Fee");

    private final String displayName;

    FeeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this fee type requires a percentage value
     *
     * @return true if percentage is required
     */
    public boolean requiresPercentage() {
        return this == PERCENTAGE || this == MINIMUM || this == MAXIMUM;
    }

    /**
     * Checks if this fee type requires a fixed amount value
     *
     * @return true if fixed amount is required
     */
    public boolean requiresFixedAmount() {
        return this == FIXED_AMOUNT || this == MINIMUM || this == MAXIMUM || this == FLAT;
    }

    /**
     * Checks if this fee type requires tier configuration
     *
     * @return true if tier configuration is required
     */
    public boolean requiresTiers() {
        return this == TIERED;
    }
}