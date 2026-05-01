package com.openfinova.banking.gl.api.entity;

/**
 * IAS 7 cash-flow statement classification for a GL account.
 *
 * Under IAS 7, the classification of a cash movement depends on the
 * operational intent of the account, not merely its balance-sheet
 * category. For a bank specifically:
 *
 * Customer loans and deposits move through OPERATING (core banking activity).
 * Investment securities, fixed assets, and subsidiaries move through INVESTING.
 * Long-term debt instruments and share capital move through FINANCING.
 * Cash and nostro accounts are the subject of the statement and are tagged
 * NONE — their net change is the closing reconciliation line, not a
 * classified activity.
 *
 * Each {@link GLAccount} stores its own classification so that account
 * administrators can override the default when chart-of-accounts conventions
 * differ from the rule of thumb applied at creation time.
 */
public enum CashFlowCategory {

    /**
     * Core revenue-generating operations and working-capital movements.
     * Typical bank examples: loans to customers, customer deposits,
     * interbank placements, trading assets/liabilities, accruals.
     */
    OPERATING,

    /**
     * Acquisition and disposal of long-term assets and other investments
     * not held for trading.
     * Typical bank examples: investment securities held-to-maturity,
     * property and equipment, intangibles, equity investments in subsidiaries.
     */
    INVESTING,

    /**
     * Activities that result in changes in the size and composition of
     * contributed equity and borrowings.
     * Typical bank examples: subordinated debt, senior unsecured bonds,
     * share capital, dividends paid.
     */
    FINANCING,

    /**
     * Not classified — the account itself represents cash or a cash
     * equivalent and therefore forms the opening/closing reconciliation
     * balance rather than a classified activity.
     * Typical examples: cash in vault, central-bank current accounts,
     * nostro accounts, overnight deposits with original maturity ≤ 3 months.
     */
    NONE
}
