package com.openfinova.banking.loan.api.entity;

/**
 * Method for calculating early loan settlement amount.
 *
 * Determines how the settlement amount is calculated when a borrower
 * wants to pay off their loan before the maturity date.
 *
 * BACKGROUND:
 * Historically, various calculation methods existed for early settlement,
 * including Rule of 78 (sum-of-digits), actuarial methods, flat-rate rebates,
 * and others. Many of these methods are now:
 * - Deprecated due to unfair treatment of borrowers
 * - Banned in certain jurisdictions (e.g., Rule of 78 prohibited for loans over 61 months in US)
 * - Rarely used in modern banking systems
 * - Replaced by simpler, more transparent methods
 *
 * This enum includes only the two most common and widely accepted methods
 * used in contemporary banking systems.
 *
 * REGULATORY CONSIDERATIONS:
 * - Some jurisdictions mandate specific calculation methods
 * - Prepayment terms must be clearly disclosed to borrowers
 * - Calculation method should be documented in loan agreement
 * - Audit trail required for all settlement calculations
 */
public enum SettlementCalculationMethod {

    /**
     * Full outstanding amount without any discount.
     *
     * Settlement amount = Outstanding Principal + Outstanding Interest +
     *                     Outstanding Fees + Outstanding Penalties
     *
     * This is the standard method where the borrower pays everything owed
     * at the time of settlement. No discounts or rebates are applied.
     *
     * Use cases:
     * - Default method for most loan products
     * - When no early payment incentive is offered
     * - Regulatory requirement in some jurisdictions
     *
     * Example:
     * Outstanding Principal: $5,000
     * Outstanding Interest:  $  100
     * Outstanding Fees:      $   25
     * Outstanding Penalties: $    0
     * Settlement Amount:     $5,125
     */
    FULL_OUTSTANDING,

    /**
     * Discounted settlement with interest rebate.
     *
     * Settlement amount = Outstanding Principal + (90% of Outstanding Interest) +
     *                     Outstanding Fees + Outstanding Penalties
     *
     * Provides a 10% discount on outstanding interest to incentivize early payment.
     * This benefits both the borrower (reduced cost) and lender (early capital recovery).
     *
     * Use cases:
     * - Promotional campaigns to encourage early payoff
     * - Portfolio management to reduce exposure
     * - Customer retention strategy
     * - Liquidity management
     *
     * Business rationale:
     * - Lender recovers capital earlier for redeployment
     * - Reduces credit risk exposure
     * - Improves portfolio quality metrics
     * - Borrower saves on interest costs
     *
     * Example:
     * Outstanding Principal: $5,000
     * Outstanding Interest:  $  100
     * Interest Discount (10%): $   10
     * Outstanding Fees:      $   25
     * Outstanding Penalties: $    0
     * Settlement Amount:     $5,115 (saved $10)
     */
    DISCOUNTED
}
