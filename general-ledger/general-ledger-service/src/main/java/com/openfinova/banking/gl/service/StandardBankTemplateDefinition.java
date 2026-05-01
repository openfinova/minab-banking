package com.openfinova.banking.gl.service;

import java.util.ArrayList;
import java.util.List;

import com.openfinova.banking.gl.api.entity.BalanceType;
import com.openfinova.banking.gl.api.entity.GLAccountStatus;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;
import com.openfinova.banking.gl.dto.AccountExportData;
import com.openfinova.banking.gl.dto.ChartOfAccountsImport;

/**
 * Standard bank template definition for GL accounts and operational account mappings.
 *
 * This is the single source of truth for:
 * 1. The standard chart of accounts (GL accounts)
 * 2. Which GL accounts map to operational account types
 *
 * Used by:
 * - GLAccountService.createStandardChartOfAccounts() → reads GL account definitions
 * - OperationalGLAccountService.createStandardOperationalAccounts() → reads operational mappings
 */
public class StandardBankTemplateDefinition {

    /**
     * Returns the standard chart of accounts with operational account type metadata.
     *
     * @param currency the currency code for the GL accounts
     * @return ChartOfAccountsImport containing all standard GL accounts
     */
    public static ChartOfAccountsImport getStandardTemplate(String currency) {
        List<AccountExportData> accounts = new ArrayList<>();

        // ========== ASSETS (1000-1999) ==========
        // Cash and Liquidity
        addAccount(
                accounts,
                "1000",
                "Cash and Cash Equivalents",
                GLAccountType.ASSET,
                currency,
                "Vault cash, teller cash, and similar instruments",
                null,
                null);
        addAccount(
                accounts,
                "1010",
                "Vault Cash",
                GLAccountType.ASSET,
                currency,
                "Physical cash in bank vaults",
                "1000",
                OperationalGLAccountType.CASH_VAULT);
        addAccount(
                accounts,
                "1011",
                "ATM Cash",
                GLAccountType.ASSET,
                currency,
                "Cash held in ATM machines",
                "1000",
                OperationalGLAccountType.ATM_CASH);
        addAccount(
                accounts,
                "1020",
                "Due from Central Bank",
                GLAccountType.ASSET,
                currency,
                "Reserve accounts at central bank",
                "1000",
                null);
        addAccount(
                accounts,
                "1030",
                "Due from Banks",
                GLAccountType.ASSET,
                currency,
                "Nostro accounts - due from other banks",
                "1000",
                null);
        addAccount(
                accounts,
                "1040",
                "Money Market Deposits",
                GLAccountType.ASSET,
                currency,
                "Short-term deposits with other financial institutions",
                "1000",
                null);

        // Loans and Advances
        addAccount(
                accounts,
                "1100",
                "Loan Portfolio",
                GLAccountType.ASSET,
                currency,
                "Total loans and advances to customers",
                null,
                null);
        addAccount(
                accounts,
                "1105",
                "External Clearing",
                GLAccountType.ASSET,
                currency,
                "External payment clearing account",
                "1100",
                OperationalGLAccountType.EXTERNAL_CLEARING);
        addAccount(
                accounts,
                "1110",
                "Consumer Loans",
                GLAccountType.ASSET,
                currency,
                "Personal loans and consumer credit",
                "1100",
                null);
        addAccount(
                accounts,
                "1120",
                "Commercial Loans",
                GLAccountType.ASSET,
                currency,
                "Business loans and commercial credit",
                "1100",
                null);
        addAccount(
                accounts,
                "1130",
                "Mortgage Loans",
                GLAccountType.ASSET,
                currency,
                "Real estate mortgage loans",
                "1100",
                null);
        addAccount(
                accounts,
                "1140",
                "Overdrafts",
                GLAccountType.ASSET,
                currency,
                "Customer overdraft facilities",
                "1100",
                null);
        addAccount(
                accounts,
                "1150",
                "Loan Loss Provision",
                GLAccountType.ASSET,
                currency,
                "Provision for doubtful debts (contra-asset)",
                "1100",
                null,
                BalanceType.CREDIT);

        // Securities and Investments
        addAccount(
                accounts,
                "1200",
                "Investment Securities",
                GLAccountType.ASSET,
                currency,
                "Trading and held-to-maturity securities",
                null,
                null);
        addAccount(
                accounts,
                "1210",
                "Government Securities",
                GLAccountType.ASSET,
                currency,
                "Government bonds and treasury bills",
                "1200",
                null);
        addAccount(
                accounts,
                "1220",
                "Corporate Securities",
                GLAccountType.ASSET,
                currency,
                "Corporate bonds and debentures",
                "1200",
                null);
        addAccount(
                accounts,
                "1230",
                "Equity Investments",
                GLAccountType.ASSET,
                currency,
                "Equity shares and stock investments",
                "1200",
                null);

        // Fixed Assets
        addAccount(
                accounts,
                "1300",
                "Fixed Assets",
                GLAccountType.ASSET,
                currency,
                "Property, plant, and equipment",
                null,
                null);
        addAccount(
                accounts,
                "1310",
                "Bank Premises",
                GLAccountType.ASSET,
                currency,
                "Branch buildings and property",
                "1300",
                null);
        addAccount(
                accounts,
                "1320",
                "Furniture and Fittings",
                GLAccountType.ASSET,
                currency,
                "Office furniture and equipment",
                "1300",
                null);
        addAccount(
                accounts,
                "1330",
                "Accumulated Depreciation",
                GLAccountType.ASSET,
                currency,
                "Accumulated depreciation on fixed assets (contra-asset)",
                "1300",
                null,
                BalanceType.CREDIT);

        // ========== LIABILITIES (2000-2999) ==========
        addAccount(
                accounts,
                "2000",
                "Customer Deposits",
                GLAccountType.LIABILITY,
                currency,
                "Total customer deposit liabilities",
                null,
                null);
        addAccount(
                accounts,
                "2010",
                "Demand Deposits",
                GLAccountType.LIABILITY,
                currency,
                "Checking and savings accounts",
                "2000",
                null);
        addAccount(
                accounts,
                "2020",
                "Time Deposits",
                GLAccountType.LIABILITY,
                currency,
                "Fixed-term deposit accounts",
                "2000",
                null);

        addAccount(
                accounts,
                "2100",
                "Borrowings",
                GLAccountType.LIABILITY,
                currency,
                "Amounts borrowed from other banks and institutions",
                null,
                null);
        addAccount(
                accounts,
                "2110",
                "Interbank Borrowings",
                GLAccountType.LIABILITY,
                currency,
                "Short-term borrowings from other banks",
                "2100",
                null);
        addAccount(
                accounts,
                "2120",
                "Long-term Borrowings",
                GLAccountType.LIABILITY,
                currency,
                "Long-term loans and bonds issued",
                "2100",
                null);

        // ========== EQUITY (3000-3999) ==========
        addAccount(
                accounts,
                "3000",
                "Shareholders' Equity",
                GLAccountType.EQUITY,
                currency,
                "Total equity capital",
                null,
                null);
        addAccount(
                accounts,
                "3010",
                "Share Capital",
                GLAccountType.EQUITY,
                currency,
                "Issued and paid-up capital",
                "3000",
                null);
        addAccount(
                accounts,
                "3020",
                "Retained Earnings",
                GLAccountType.EQUITY,
                currency,
                "Accumulated profits and losses",
                "3000",
                null);
        addAccount(
                accounts,
                "3030",
                "Unrealized FX Gains/Losses",
                GLAccountType.EQUITY,
                currency,
                "Currency revaluation adjustments from FX Revaluation process",
                "3000",
                OperationalGLAccountType.UNREALIZED_FX_GL);

        // ========== REVENUE (4000-4999) ==========
        addAccount(
                accounts,
                "4000",
                "Operating Revenue",
                GLAccountType.REVENUE,
                currency,
                "Total operating income",
                null,
                null);
        addAccount(
                accounts,
                "4010",
                "Service Charges",
                GLAccountType.REVENUE,
                currency,
                "Income from service fees",
                "4000",
                OperationalGLAccountType.FEE_INCOME);

        addAccount(
                accounts,
                "4100",
                "Interest Income",
                GLAccountType.REVENUE,
                currency,
                "Interest earned on loans and investments",
                null,
                null);
        addAccount(
                accounts,
                "4110",
                "Loan Interest Income",
                GLAccountType.REVENUE,
                currency,
                "Interest from customer loans",
                "4100",
                OperationalGLAccountType.INTEREST_INCOME);
        addAccount(
                accounts,
                "4120",
                "Investment Interest Income",
                GLAccountType.REVENUE,
                currency,
                "Interest from securities and deposits",
                "4100",
                null);
        addAccount(
                accounts,
                "4130",
                "Overdraft Interest Income",
                GLAccountType.REVENUE,
                currency,
                "Interest from overdraft facilities",
                "4100",
                OperationalGLAccountType.OVERDRAFT_INTEREST_INCOME);

        addAccount(
                accounts,
                "4200",
                "Investment Gains",
                GLAccountType.REVENUE,
                currency,
                "Gains from investment sales and revaluations",
                null,
                null);
        addAccount(
                accounts,
                "4210",
                "Realized FX Gains",
                GLAccountType.REVENUE,
                currency,
                "Realised gains on foreign exchange transactions",
                "4200",
                OperationalGLAccountType.FX_GAIN);

        // ========== EXPENSES (5000-5999) ==========
        addAccount(
                accounts,
                "5000",
                "Operating Expenses",
                GLAccountType.EXPENSE,
                currency,
                "Total operating expenses",
                null,
                null);
        addAccount(
                accounts,
                "5010",
                "Personnel Costs",
                GLAccountType.EXPENSE,
                currency,
                "Salaries and employee benefits",
                "5000",
                null);
        addAccount(
                accounts,
                "5015",
                "Card Processing Fees",
                GLAccountType.EXPENSE,
                currency,
                "Fees paid to card networks",
                "5000",
                OperationalGLAccountType.CARD_PROCESSING_FEES);
        addAccount(
                accounts,
                "5020",
                "Occupancy Costs",
                GLAccountType.EXPENSE,
                currency,
                "Rent and facility maintenance",
                "5000",
                null);
        addAccount(
                accounts,
                "5030",
                "Technology Costs",
                GLAccountType.EXPENSE,
                currency,
                "IT infrastructure and software",
                "5000",
                null);

        addAccount(
                accounts,
                "5100",
                "Interest Expense",
                GLAccountType.EXPENSE,
                currency,
                "Interest paid on deposits and borrowings",
                null,
                null);
        addAccount(
                accounts,
                "5110",
                "Deposit Interest",
                GLAccountType.EXPENSE,
                currency,
                "Interest paid to depositors",
                "5100",
                OperationalGLAccountType.INTEREST_EXPENSE);
        addAccount(
                accounts,
                "5120",
                "Borrowing Interest",
                GLAccountType.EXPENSE,
                currency,
                "Interest paid on borrowed funds",
                "5100",
                null);

        addAccount(
                accounts,
                "5200",
                "Provisions and Write-offs",
                GLAccountType.EXPENSE,
                currency,
                "Loan loss provisions and charge-offs",
                null,
                null);

        addAccount(
                accounts,
                "5300",
                "FX Losses",
                GLAccountType.EXPENSE,
                currency,
                "Realised foreign exchange losses (IAS 21 — disclosed separately from FX gains)",
                null,
                null);
        addAccount(
                accounts,
                "5310",
                "Realized FX Losses",
                GLAccountType.EXPENSE,
                currency,
                "Realised losses on foreign exchange transactions",
                "5300",
                OperationalGLAccountType.FX_LOSS);

        // Wrap in ChartOfAccountsImport
        ChartOfAccountsImport chartImport = new ChartOfAccountsImport();
        chartImport.setAccounts(accounts);
        chartImport.setOverwriteExisting(false);
        chartImport.setImportFormat("STANDARD");

        return chartImport;
    }

    /**
     * Helper method to create and add an account to the list.
     * Uses normal balance from OperationalGLAccountType if present, otherwise uses default based on type.
     */
    private static void addAccount(List<AccountExportData> accounts, String code, String name, GLAccountType type,
            String currency, String description, String parentCode, OperationalGLAccountType operationalType) {
        // If this is an operational account, use its normal balance; otherwise use default
        BalanceType normalBalance = operationalType != null ? operationalType.getNormalBalance()
                : (type == GLAccountType.ASSET ? BalanceType.DEBIT : BalanceType.CREDIT);
        addAccount(accounts, code, name, type, currency, description, parentCode, operationalType, normalBalance);
    }

    /**
     * Helper method to create and add an account to the list with explicit balance type.
     * Use this for contra-accounts and other exceptions to standard balance rules.
     */
    private static void addAccount(List<AccountExportData> accounts, String code, String name, GLAccountType type,
            String currency, String description, String parentCode, OperationalGLAccountType operationalType,
            BalanceType normalBalance) {
        AccountExportData account = new AccountExportData();
        account.setCode(code);
        account.setName(name);
        account.setType(type);
        account.setCurrency(currency);
        account.setDescription(description);
        account.setParentCode(parentCode);
        account.setStatus(GLAccountStatus.ACTIVE);
        account.setNormalBalance(normalBalance);
        account.setOperationalAccountType(operationalType);
        accounts.add(account);
    }
}
