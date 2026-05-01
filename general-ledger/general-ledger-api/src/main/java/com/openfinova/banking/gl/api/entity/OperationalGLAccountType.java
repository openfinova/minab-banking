package com.openfinova.banking.gl.api.entity;

/**
 * Enumeration of operational GL account types used for bank operations.
 * These are bank-owned accounts not tied to specific customer accounts.
 */
public enum OperationalGLAccountType {
    /**
     * Fee income account - Revenue from transaction fees, account maintenance fees, etc.
     * Account Type: REVENUE
     * Normal Balance: CREDIT
     */
    FEE_INCOME("Fee Income", "Revenue from transaction and service fees", GLAccountType.REVENUE, BalanceType.CREDIT),

    /**
     * Cash vault account - Physical cash on hand at bank branches
     * Account Type: ASSET
     * Normal Balance: DEBIT
     */
    CASH_VAULT("Cash Vault", "Physical cash on hand", GLAccountType.ASSET, BalanceType.DEBIT),

    /**
     * Suspense account - Temporary holding for unmatched or pending transactions
     * Account Type: ASSET or LIABILITY (depending on balance)
     * Normal Balance: DEBIT
     */
    SUSPENSE("Suspense Account", "Temporary holding for unmatched transactions", GLAccountType.ASSET,
            BalanceType.DEBIT),

    /**
     * External clearing account - For external payments and receipts
     * Account Type: ASSET
     * Normal Balance: DEBIT
     */
    EXTERNAL_CLEARING("External Clearing", "External payment clearing account", GLAccountType.ASSET, BalanceType.DEBIT),

    /**
     * Interest expense account - Interest paid to customer deposit accounts
     * Account Type: EXPENSE
     * Normal Balance: DEBIT
     */
    INTEREST_EXPENSE("Interest Expense", "Interest paid to customers", GLAccountType.EXPENSE, BalanceType.DEBIT),

    /**
     * Interest income account - Interest earned from loans and investments
     * Account Type: REVENUE
     * Normal Balance: CREDIT
     */
    INTEREST_INCOME("Interest Income", "Interest earned from loans", GLAccountType.REVENUE, BalanceType.CREDIT),

    /**
     * Accrued loan interest receivable (asset) — pairs with interest accrual recognition.
     * Account Type: ASSET
     * Normal Balance: DEBIT
     */
    LOAN_INTEREST_RECEIVABLE("Loan Interest Receivable", "Accrued interest on loan portfolio", GLAccountType.ASSET,
            BalanceType.DEBIT),

    /**
     * Foreign exchange gain account — realised gains from currency conversions.
     *
     * <p>IAS 21 requires FX gains and losses to be disclosed separately on the
     * income statement.  Gains are income: Account Type REVENUE, normal balance CREDIT.
     */
    FX_GAIN("FX Gain", "Realised foreign exchange gains", GLAccountType.REVENUE, BalanceType.CREDIT),

    /**
     * Foreign exchange loss account — realised losses from currency conversions.
     *
     * <p>IAS 21 requires FX gains and losses to be disclosed separately on the
     * income statement.  Losses are expenses: Account Type EXPENSE, normal balance DEBIT.
     * A separate EXPENSE account avoids the structural problem of a REVENUE account
     * carrying an abnormal debit balance when net FX exposure is a loss.
     */
    FX_LOSS("FX Loss", "Realised foreign exchange losses", GLAccountType.EXPENSE, BalanceType.DEBIT),

    /**
     * ATM cash account - Cash held in ATM machines
     * Account Type: ASSET
     * Normal Balance: DEBIT
     */
    ATM_CASH("ATM Cash", "Cash held in ATM machines", GLAccountType.ASSET, BalanceType.DEBIT),

    /**
     * Card processing fees account - Fees paid to card networks
     * Account Type: EXPENSE
     * Normal Balance: DEBIT
     */
    CARD_PROCESSING_FEES("Card Processing Fees", "Fees paid to card networks", GLAccountType.EXPENSE,
            BalanceType.DEBIT),

    /**
     * Overdraft interest income - Interest earned from overdraft facilities
     * Account Type: REVENUE
     * Normal Balance: CREDIT
     */
    OVERDRAFT_INTEREST_INCOME("Overdraft Interest Income", "Interest from overdrafts", GLAccountType.REVENUE,
            BalanceType.CREDIT),

    /**
     * Retained earnings - Accumulated profits retained in the business
     * Account Type: EQUITY
     * Normal Balance: CREDIT
     * Used in period-end closing to accumulate P&L results
     */
    RETAINED_EARNINGS("Retained Earnings", "Accumulated profits retained in the business", GLAccountType.EQUITY,
            BalanceType.CREDIT),

    /**
     * Unrealized FX gains/losses - Currency revaluation adjustments for foreign currency accounts
     * Account Type: EQUITY
     * Normal Balance: CREDIT (for gains)
     */
    UNREALIZED_FX_GL("Unrealized FX G/L", "Currency revaluation gains and losses", GLAccountType.EQUITY,
            BalanceType.CREDIT);

    private final String displayName;
    private final String description;
    private final GLAccountType accountType;
    private final BalanceType normalBalance;

    OperationalGLAccountType(String displayName, String description, GLAccountType accountType,
            BalanceType normalBalance) {
        this.displayName = displayName;
        this.description = description;
        this.accountType = accountType;
        this.normalBalance = normalBalance;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public GLAccountType getAccountType() {
        return accountType;
    }

    public BalanceType getNormalBalance() {
        return normalBalance;
    }

    /**
     * Gets the code representation of this operational account type.
     *
     * @return the enum name as code
     */
    public String getCode() {
        return this.name();
    }
}
