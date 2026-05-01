package com.openfinova.banking.gl.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.gl.api.dto.BalanceSheetResponse;
import com.openfinova.banking.gl.api.dto.CashFlowStatementResponse;
import com.openfinova.banking.gl.api.dto.FinancialStatementLine;
import com.openfinova.banking.gl.api.dto.IncomeStatementResponse;
import com.openfinova.banking.gl.api.entity.BalanceType;
import com.openfinova.banking.gl.api.entity.CashFlowCategory;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.repository.GLAccountRepository;

/**
 * Generates period-end financial statement reports (Income Statement, Balance Sheet,
 * Statement of Cash Flows) by querying account balances via {@link BalanceService}.
 *
 * <p>Deliberately kept separate from {@link BalanceService} because report generation
 * is a read-only aggregation concern — it has no interest in snapshot maintenance,
 * reconciliation, or daily balance persistence.
 */
@Service
@Transactional(readOnly = true)
public class FinancialReportService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialReportService.class);

    private final BalanceService balanceService;
    private final GLAccountRepository glAccountRepository;

    public FinancialReportService(BalanceService balanceService, GLAccountRepository glAccountRepository) {
        this.balanceService = balanceService;
        this.glAccountRepository = glAccountRepository;
    }

    /**
     * Generates an income statement (profit and loss statement) for the given period.
     *
     * <p>Revenue (REVENUE type, credit-normal): period activity = balance(endDate) minus
     * balance(startDate - 1 day). A positive value represents income earned.
     * Expense (EXPENSE type, debit-normal): period activity = balance(endDate) minus
     * balance(startDate - 1 day). A positive value represents a cost incurred.
     *
     * @param startDate first day of the reporting period (inclusive)
     * @param endDate   last day of the reporting period (inclusive)
     * @return populated {@link IncomeStatementResponse}
     */
    public IncomeStatementResponse getIncomeStatement(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating income statement from {} to {}", startDate, endDate);

        LocalDate openingDate = startDate.minusDays(1);

        List<FinancialStatementLine> revenueLines = new ArrayList<>();
        List<FinancialStatementLine> expenseLines = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        // Single round-trip: fetch REVENUE + EXPENSE accounts together, partition in memory.
        Map<GLAccountType, List<GLAccount>> incomeAccounts = glAccountRepository
                .findByTypeIn(List.of(GLAccountType.REVENUE, GLAccountType.EXPENSE)).stream()
                .collect(Collectors.groupingBy(GLAccount::getType));

        // Revenue accounts (credit-normal): positive period delta = revenue earned
        for (GLAccount account : incomeAccounts.getOrDefault(GLAccountType.REVENUE, List.of())) {
            BigDecimal opening = balanceService.getBalanceAtDate(account.getId(), openingDate);
            BigDecimal closing = balanceService.getBalanceAtDate(account.getId(), endDate);
            BigDecimal periodActivity = closing.subtract(opening);
            if (periodActivity.compareTo(BigDecimal.ZERO) != 0) {
                revenueLines.add(
                        new FinancialStatementLine(
                                account.getId(),
                                account.getCode(),
                                account.getName(),
                                periodActivity.abs()));
                totalRevenue = totalRevenue.add(periodActivity.abs());
            }
        }

        // Expense accounts (debit-normal): positive period delta = cost incurred
        for (GLAccount account : incomeAccounts.getOrDefault(GLAccountType.EXPENSE, List.of())) {
            BigDecimal opening = balanceService.getBalanceAtDate(account.getId(), openingDate);
            BigDecimal closing = balanceService.getBalanceAtDate(account.getId(), endDate);
            BigDecimal periodActivity = closing.subtract(opening);
            if (periodActivity.compareTo(BigDecimal.ZERO) != 0) {
                expenseLines.add(
                        new FinancialStatementLine(
                                account.getId(),
                                account.getCode(),
                                account.getName(),
                                periodActivity.abs()));
                totalExpenses = totalExpenses.add(periodActivity.abs());
            }
        }

        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);

        logger.info(
                "Income statement generated: revenue={}, expenses={}, netIncome={}",
                totalRevenue,
                totalExpenses,
                netIncome);

        return new IncomeStatementResponse(
                startDate,
                endDate,
                revenueLines,
                expenseLines,
                totalRevenue,
                totalExpenses,
                netIncome);
    }

    /**
     * Generates a balance sheet (statement of financial position) as of the given date.
     *
     * <p>Asset accounts (debit-normal): positive balance = asset.
     * Liability accounts (credit-normal): positive balance = liability.
     * Equity accounts (credit-normal): positive balance = equity.
     *
     * @param asOfDate snapshot date
     * @return populated {@link BalanceSheetResponse}
     */
    public BalanceSheetResponse getBalanceSheet(LocalDate asOfDate) {
        logger.info("Generating balance sheet as of {}", asOfDate);

        List<FinancialStatementLine> assetLines = new ArrayList<>();
        List<FinancialStatementLine> liabilityLines = new ArrayList<>();
        List<FinancialStatementLine> equityLines = new ArrayList<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;

        // Single round-trip: fetch ASSET + LIABILITY + EQUITY accounts together, partition in memory.
        Map<GLAccountType, List<GLAccount>> bsAccounts = glAccountRepository
                .findByTypeIn(List.of(GLAccountType.ASSET, GLAccountType.LIABILITY, GLAccountType.EQUITY)).stream()
                .collect(Collectors.groupingBy(GLAccount::getType));

        for (GLAccount account : bsAccounts.getOrDefault(GLAccountType.ASSET, List.of())) {
            BigDecimal balance = balanceService.getBalanceAtDate(account.getId(), asOfDate);
            if (balance.compareTo(BigDecimal.ZERO) != 0) {
                // Contra-assets (e.g. Allowance for Credit Losses) reduce the section total.
                BigDecimal contribution = account.isContra() ? balance.negate() : balance;
                assetLines.add(
                        new FinancialStatementLine(
                                account.getId(),
                                account.getCode(),
                                account.getName(),
                                contribution));
                totalAssets = totalAssets.add(contribution);
            }
        }

        for (GLAccount account : bsAccounts.getOrDefault(GLAccountType.LIABILITY, List.of())) {
            BigDecimal balance = balanceService.getBalanceAtDate(account.getId(), asOfDate);
            if (balance.compareTo(BigDecimal.ZERO) != 0) {
                // Contra-liabilities (e.g. Unamortised Bond Premium) reduce the section total.
                BigDecimal contribution = account.isContra() ? balance.negate() : balance;
                liabilityLines.add(
                        new FinancialStatementLine(
                                account.getId(),
                                account.getCode(),
                                account.getName(),
                                contribution));
                totalLiabilities = totalLiabilities.add(contribution);
            }
        }

        for (GLAccount account : bsAccounts.getOrDefault(GLAccountType.EQUITY, List.of())) {
            BigDecimal balance = balanceService.getBalanceAtDate(account.getId(), asOfDate);
            if (balance.compareTo(BigDecimal.ZERO) != 0) {
                // Contra-equity (e.g. Treasury Stock) reduces the section total.
                BigDecimal contribution = account.isContra() ? balance.negate() : balance;
                equityLines.add(
                        new FinancialStatementLine(
                                account.getId(),
                                account.getCode(),
                                account.getName(),
                                contribution));
                totalEquity = totalEquity.add(contribution);
            }
        }

        BigDecimal liabPlusEquity = totalLiabilities.add(totalEquity);
        boolean balanced = totalAssets.compareTo(liabPlusEquity) == 0;

        logger.info(
                "Balance sheet generated: assets={}, liabilities={}, equity={}, balanced={}",
                totalAssets,
                totalLiabilities,
                totalEquity,
                balanced);

        return new BalanceSheetResponse(
                asOfDate,
                assetLines,
                liabilityLines,
                equityLines,
                totalAssets,
                totalLiabilities,
                totalEquity,
                balanced);
    }

    /**
     * Generates a statement of cash flows for the given period (indirect method, IAS 7).
     *
     * <p>Classification is driven by {@link GLAccount#getCashFlowCategory()}, which records
     * the operational intent of each account rather than its raw balance-sheet type.
     * This allows, for example, customer loans (ASSET) to appear in Operating activities
     * and investment securities (also ASSET) to appear in Investing activities, as required
     * by IAS 7 for banks.
     *
     * <ul>
     *   <li><b>Operating</b>: Net income + working-capital changes in accounts tagged OPERATING.</li>
     *   <li><b>Investing</b>: Net changes in accounts tagged INVESTING.</li>
     *   <li><b>Financing</b>: Net changes in accounts tagged FINANCING.</li>
     *   <li>Accounts tagged {@link CashFlowCategory#NONE} (cash / nostro) are excluded;
     *       their aggregate change is the reconciling balance at the foot of the statement.</li>
     * </ul>
     *
     * <p>Sign convention (consistent with indirect method):
     * <ul>
     *   <li>Debit-normal accounts (Assets): increase = cash outflow → amount is negated.</li>
     *   <li>Credit-normal accounts (Liabilities, Equity): increase = cash inflow → amount is kept.</li>
     * </ul>
     *
     * @param startDate first day of the reporting period (inclusive)
     * @param endDate   last day of the reporting period (inclusive)
     * @return populated {@link CashFlowStatementResponse}
     */
    public CashFlowStatementResponse getCashFlowStatement(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating cash flow statement from {} to {}", startDate, endDate);

        LocalDate openingDate = startDate.minusDays(1);

        // Derive net income from income statement
        IncomeStatementResponse incomeStatement = getIncomeStatement(startDate, endDate);
        BigDecimal netIncome = incomeStatement.getNetIncome();

        // Operating: starts with net income; working-capital changes appended below
        List<FinancialStatementLine> operatingActivities = new ArrayList<>();
        operatingActivities.add(new FinancialStatementLine(null, "NET-INCOME", "Net Income / (Loss)", netIncome));
        BigDecimal totalOperating = netIncome;

        List<FinancialStatementLine> investingActivities = new ArrayList<>();
        BigDecimal totalInvesting = BigDecimal.ZERO;

        List<FinancialStatementLine> financingActivities = new ArrayList<>();
        BigDecimal totalFinancing = BigDecimal.ZERO;

        // Single round-trip: fetch ASSET + LIABILITY + EQUITY together; REVENUE/EXPENSE are in netIncome.
        Map<GLAccountType, List<GLAccount>> cfAccounts = glAccountRepository
                .findByTypeIn(List.of(GLAccountType.ASSET, GLAccountType.LIABILITY, GLAccountType.EQUITY)).stream()
                .collect(Collectors.groupingBy(GLAccount::getType));

        for (GLAccountType accountType : List.of(GLAccountType.ASSET, GLAccountType.LIABILITY, GLAccountType.EQUITY)) {
            for (GLAccount account : cfAccounts.getOrDefault(accountType, List.of())) {

                CashFlowCategory category = account.getCashFlowCategory();
                if (category == CashFlowCategory.NONE) {
                    // Cash and cash-equivalent accounts are the reconciling balance, not an activity.
                    continue;
                }

                BigDecimal opening = balanceService.getBalanceAtDate(account.getId(), openingDate);
                BigDecimal closing = balanceService.getBalanceAtDate(account.getId(), endDate);
                // Raw change in the account's own sign convention (positive = normal direction).
                BigDecimal rawChange = closing.subtract(opening);

                if (rawChange.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                // Convert to cash-flow sign:
                //   Debit-normal (Asset): increase in asset = cash outflow → negate.
                //   Credit-normal (Liability/Equity): increase in liability/equity = cash inflow → keep.
                BigDecimal cashImpact = account.getNormalBalance() == BalanceType.DEBIT ? rawChange.negate()
                        : rawChange;

                FinancialStatementLine line = new FinancialStatementLine(
                        account.getId(),
                        account.getCode(),
                        account.getName(),
                        cashImpact);

                switch (category) {
                    case OPERATING -> {
                        operatingActivities.add(line);
                        totalOperating = totalOperating.add(cashImpact);
                    }
                    case INVESTING -> {
                        investingActivities.add(line);
                        totalInvesting = totalInvesting.add(cashImpact);
                    }
                    case FINANCING -> {
                        financingActivities.add(line);
                        totalFinancing = totalFinancing.add(cashImpact);
                    }
                    default -> {
                        /* NONE already handled above */ }
                }
            }
        }

        BigDecimal netCashChange = totalOperating.add(totalInvesting).add(totalFinancing);

        logger.info(
                "Cash flow statement generated: operating={}, investing={}, financing={}, net={}",
                totalOperating,
                totalInvesting,
                totalFinancing,
                netCashChange);

        return new CashFlowStatementResponse(
                startDate,
                endDate,
                netIncome,
                operatingActivities,
                investingActivities,
                financingActivities,
                totalOperating,
                totalInvesting,
                totalFinancing,
                netCashChange);
    }
}
