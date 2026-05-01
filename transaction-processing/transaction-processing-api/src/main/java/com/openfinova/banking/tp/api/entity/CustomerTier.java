package com.openfinova.banking.tp.api.entity;

/**
 * Enumeration of customer tiers for fee calculation and velocity limits.
 * Different tiers have different fee structures and transaction limits.
 *
 * Requirements addressed:
 * - Customer tier-based fee logic
 * - Promotional fee waiver support
 * - Customer tier-based limit variations
 */
public enum CustomerTier {
    /**
     * Basic tier - standard fees and limits
     */
    BASIC("Basic", 1.0, false),

    /**
     * Premium tier - reduced fees and higher limits
     */
    PREMIUM("Premium", 0.75, false),

    /**
     * VIP tier - minimal fees and highest limits
     */
    VIP("VIP", 0.5, true),

    /**
     * Enterprise tier - custom fee structures and limits
     */
    ENTERPRISE("Enterprise", 0.25, true);

    private final String displayName;
    private final double feeMultiplier;
    private final boolean eligibleForWaivers;

    CustomerTier(String displayName, double feeMultiplier, boolean eligibleForWaivers) {
        this.displayName = displayName;
        this.feeMultiplier = feeMultiplier;
        this.eligibleForWaivers = eligibleForWaivers;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the fee multiplier for this tier
     *
     * @return multiplier to apply to base fees (1.0 = full fee, 0.5 = 50% discount)
     */
    public double getFeeMultiplier() {
        return feeMultiplier;
    }

    /**
     * Checks if this tier is eligible for promotional fee waivers
     *
     * @return true if eligible for fee waivers
     */
    public boolean isEligibleForWaivers() {
        return eligibleForWaivers;
    }

    /**
     * Checks if this is a premium tier (Premium, VIP, or Enterprise)
     *
     * @return true if premium tier
     */
    public boolean isPremium() {
        return this != BASIC;
    }
}