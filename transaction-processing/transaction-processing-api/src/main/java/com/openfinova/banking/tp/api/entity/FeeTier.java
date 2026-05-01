package com.openfinova.banking.tp.api.entity;

import java.math.BigDecimal;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Value object representing a fee tier for tiered fee calculations.
 * Used within FeeRule for complex fee structures based on transaction amounts.
 */
public class FeeTier {

    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;
    private final BigDecimal feeAmount;
    private final BigDecimal feePercentage;
    private final String description;

    @JsonCreator
    public FeeTier(@JsonProperty("minAmount") BigDecimal minAmount, @JsonProperty("maxAmount") BigDecimal maxAmount,
            @JsonProperty("feeAmount") BigDecimal feeAmount, @JsonProperty("feePercentage") BigDecimal feePercentage,
            @JsonProperty("description") String description) {
        this.minAmount = minAmount != null ? minAmount : BigDecimal.ZERO;
        this.maxAmount = maxAmount;
        this.feeAmount = feeAmount;
        this.feePercentage = feePercentage;
        this.description = description;

        validateTier();
    }

    /**
     * Creates a fixed amount tier
     *
     * @param minAmount   minimum transaction amount for this tier
     * @param maxAmount   maximum transaction amount for this tier (null for
     *                    unlimited)
     * @param feeAmount   fixed fee amount for this tier
     * @param description description of this tier
     * @return new FeeTier instance
     */
    public static FeeTier fixedAmount(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal feeAmount,
            String description) {
        return new FeeTier(minAmount, maxAmount, feeAmount, null, description);
    }

    /**
     * Creates a percentage-based tier
     *
     * @param minAmount     minimum transaction amount for this tier
     * @param maxAmount     maximum transaction amount for this tier (null for
     *                      unlimited)
     * @param feePercentage percentage fee for this tier (0.01 = 1%)
     * @param description   description of this tier
     * @return new FeeTier instance
     */
    public static FeeTier percentage(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal feePercentage,
            String description) {
        return new FeeTier(minAmount, maxAmount, null, feePercentage, description);
    }

    /**
     * Creates a combined tier with both fixed amount and percentage
     *
     * @param minAmount     minimum transaction amount for this tier
     * @param maxAmount     maximum transaction amount for this tier (null for
     *                      unlimited)
     * @param feeAmount     fixed fee amount
     * @param feePercentage percentage fee (will be added to fixed amount)
     * @param description   description of this tier
     * @return new FeeTier instance
     */
    public static FeeTier combined(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal feeAmount,
            BigDecimal feePercentage, String description) {
        return new FeeTier(minAmount, maxAmount, feeAmount, feePercentage, description);
    }

    /**
     * Checks if the given amount falls within this tier's range
     *
     * @param amount the transaction amount to check
     * @return true if amount is within this tier's range
     */
    public boolean appliesToAmount(BigDecimal amount) {
        if (amount.compareTo(minAmount) < 0) {
            return false;
        }

        return maxAmount == null || amount.compareTo(maxAmount) <= 0;
    }

    /**
     * Calculates the fee for the given amount using this tier's configuration
     *
     * @param amount the transaction amount
     * @return calculated fee amount
     */
    public BigDecimal calculateFee(BigDecimal amount) {
        if (!appliesToAmount(amount)) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalFee = BigDecimal.ZERO;

        // Add fixed amount if specified
        if (feeAmount != null) {
            totalFee = totalFee.add(feeAmount);
        }

        // Add percentage-based fee if specified
        if (feePercentage != null) {
            BigDecimal percentageFee = amount.multiply(feePercentage);
            totalFee = totalFee.add(percentageFee);
        }

        return totalFee;
    }

    /**
     * Validates the tier configuration
     *
     * @throws IllegalArgumentException if configuration is invalid
     */
    private void validateTier() {
        if (minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum amount cannot be negative");
        }

        if (maxAmount != null && minAmount.compareTo(maxAmount) >= 0) {
            throw new IllegalArgumentException("Minimum amount must be less than maximum amount");
        }

        if (feeAmount != null && feeAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fee amount cannot be negative");
        }

        if (feePercentage != null && feePercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fee percentage cannot be negative");
        }

        if (feeAmount == null && feePercentage == null) {
            throw new IllegalArgumentException("Either fee amount or fee percentage must be specified");
        }
    }

    // Getters

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public BigDecimal getFeePercentage() {
        return feePercentage;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this tier has a fixed fee component
     *
     * @return true if fixed fee is specified
     */
    public boolean hasFixedFee() {
        return feeAmount != null && feeAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this tier has a percentage fee component
     *
     * @return true if percentage fee is specified
     */
    public boolean hasPercentageFee() {
        return feePercentage != null && feePercentage.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this tier represents an unlimited upper bound
     *
     * @return true if maxAmount is null
     */
    public boolean isUnlimited() {
        return maxAmount == null;
    }

    // equals, hashCode, and toString

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FeeTier feeTier))
            return false;
        return Objects.equals(minAmount, feeTier.minAmount) && Objects.equals(maxAmount, feeTier.maxAmount)
                && Objects.equals(feeAmount, feeTier.feeAmount) && Objects.equals(feePercentage, feeTier.feePercentage)
                && Objects.equals(description, feeTier.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minAmount, maxAmount, feeAmount, feePercentage, description);
    }

    @Override
    public String toString() {
        return "FeeTier{" + "minAmount=" + minAmount + ", maxAmount=" + maxAmount + ", feeAmount=" + feeAmount
                + ", feePercentage=" + feePercentage + ", description='" + description + '\'' + '}';
    }
}