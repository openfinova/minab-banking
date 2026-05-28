package com.openfinova.banking.gl.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.exchangerate.api.ExchangeRateService;
import com.openfinova.banking.gl.api.dto.PostTransactionCommand;
import com.openfinova.banking.gl.api.entity.GLTransactionType;
import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.entity.GLRevaluationDetail;
import com.openfinova.banking.gl.entity.GLRevaluationRun;
import com.openfinova.banking.gl.entity.GLTransaction;
import com.openfinova.banking.gl.mapper.GLEntityMapper;
import com.openfinova.banking.gl.repository.GLAccountRepository;
import com.openfinova.banking.gl.repository.GLRevaluationDetailRepository;
import com.openfinova.banking.gl.repository.GLRevaluationRunRepository;

/**
 * Service responsible for period-end foreign currency revaluation.
 *
 * Calculates unrealized gains/losses for all foreign currency GL accounts based
 * on the exchange rate at a given date, posts the corresponding journal entries
 * via {@link GLTransactionService}, and creates a full audit trail through
 * {@link GLRevaluationRun} and {@link GLRevaluationDetail}.
 *
 * Extracting revaluation logic into its own service breaks the circular bean
 * dependency that would otherwise exist between {@link BalanceService},
 * {@link GLTransactionService}, and {@link GLAccountService}.
 */
@Service
@Transactional
public class RevaluationService {

    private static final Logger logger = LoggerFactory.getLogger(RevaluationService.class);

    @Value("${app.base-currency:EUR}")
    private String baseCurrency;

    private final BalanceService balanceService;
    private final GLTransactionService glTransactionService;
    private final GLAccountService glAccountService;
    private final GLAccountRepository glAccountRepository;
    private final ExchangeRateService exchangeRateService;
    private final OperationalGLAccountService operationalGLAccountService;
    private final GLRevaluationRunRepository glRevaluationRunRepository;
    private final GLRevaluationDetailRepository glRevaluationDetailRepository;

    public RevaluationService(BalanceService balanceService, GLTransactionService glTransactionService,
            GLAccountService glAccountService, GLAccountRepository glAccountRepository,
            ExchangeRateService exchangeRateService, OperationalGLAccountService operationalGLAccountService,
            GLRevaluationRunRepository glRevaluationRunRepository,
            GLRevaluationDetailRepository glRevaluationDetailRepository) {
        this.balanceService = balanceService;
        this.glTransactionService = glTransactionService;
        this.glAccountService = glAccountService;
        this.glAccountRepository = glAccountRepository;
        this.exchangeRateService = exchangeRateService;
        this.operationalGLAccountService = operationalGLAccountService;
        this.glRevaluationRunRepository = glRevaluationRunRepository;
        this.glRevaluationDetailRepository = glRevaluationDetailRepository;
    }

    /**
     * Performs period-end revaluation for all foreign currency accounts.
     * Calculates unrealized gains/losses based on the exchange rate at the end of
     * the period. Creates audit trail via GLRevaluationRun and GLRevaluationDetail.
     *
     * @param asOfDate    The date for revaluation (usually period end).
     * @param triggerType The type of trigger (MANUAL, PERIOD_CLOSE, SCHEDULED)
     * @param executedBy  The user or system performing the revaluation
     * @return The revaluation run entity containing summary information
     */
    @PreAuthorize("hasAuthority('gl:approve')")

    @Transactional
    public GLRevaluationRun performRevaluation(LocalDate asOfDate, String triggerType, String executedBy) {
        logger.info(
                "Performing currency revaluation as of: {} triggered by: {} ({})",
                asOfDate,
                executedBy,
                triggerType);

        // Create revaluation run for audit trail
        GLRevaluationRun revaluationRun = new GLRevaluationRun(asOfDate, executedBy, baseCurrency, triggerType);
        revaluationRun = glRevaluationRunRepository.save(revaluationRun);

        // Get all active accounts that are not in base currency
        List<GLAccount> foreignCurrencyAccounts = glAccountRepository.findAll().stream()
                .filter(account -> !baseCurrency.equals(account.getCurrency())).filter(GLAccount::isActive).toList();

        logger.info("Found {} foreign currency accounts for revaluation", foreignCurrencyAccounts.size());

        List<String> failures = new ArrayList<>();

        for (GLAccount account : foreignCurrencyAccounts) {
            revaluationRun.incrementAccountsProcessed();
            try {
                performAccountRevaluation(account, asOfDate, revaluationRun);
            } catch (Exception e) {
                revaluationRun.incrementAccountsFailed();
                String msg = account.getCode() + ": " + e.getMessage();
                failures.add(msg);
                logger.error("Failed to revalue account: {} on date: {}", account.getCode(), asOfDate, e);
            }
        }

        // If any account failed, roll back the entire revaluation — a partial
        // revaluation produces an incorrect FX position and must not be committed.
        if (!failures.isEmpty()) {
            throw new IllegalStateException(
                    "Revaluation aborted — " + failures.size() + " of " + revaluationRun.getAccountsProcessed()
                            + " accounts failed; " + "no entries have been posted. Failed accounts: " + failures);
        }

        // Save final statistics
        revaluationRun = glRevaluationRunRepository.save(revaluationRun);

        logger.info(
                "Currency revaluation completed for date: {}. Processed: {}, Revalued: {}, Total Adjustment: {} {}",
                asOfDate,
                revaluationRun.getAccountsProcessed(),
                revaluationRun.getAccountsRevalued(),
                revaluationRun.getTotalAdjustment(),
                baseCurrency);

        return revaluationRun;
    }

    /**
     * Convenience method for backward compatibility – performs revaluation with default trigger.
     */
    @PreAuthorize("hasAuthority('gl:approve')")
    @Transactional
    public void performRevaluation(LocalDate asOfDate) {
        performRevaluation(asOfDate, "MANUAL", "system");
    }

    /**
     * Performs revaluation for a single account and posts the resulting journal entry.
     */
    private void performAccountRevaluation(GLAccount account, LocalDate asOfDate, GLRevaluationRun revaluationRun) {
        logger.debug("Revaluing account: {} ({}) as of: {}", account.getCode(), account.getCurrency(), asOfDate);

        // Get current balance in account currency
        BigDecimal accountBalance = balanceService.getBalanceAtDate(account.getId(), asOfDate);

        if (accountBalance.compareTo(BigDecimal.ZERO) == 0) {
            logger.debug("Account {} has zero balance, skipping revaluation", account.getCode());
            return;
        }

        // Get current exchange rate (new rate)
        BigDecimal newRate = exchangeRateService.getExchangeRate(account.getCurrency(), baseCurrency, asOfDate);

        // Calculate current value in base currency (new base value)
        BigDecimal newBaseValue = accountBalance.multiply(newRate);

        // Get the last recorded base currency value (closing balance of previous day in base currency)
        BigDecimal oldBaseValue = balanceService.getBalanceInBaseCurrency(account.getId(), asOfDate.minusDays(1));

        // Calculate old rate from old base value
        BigDecimal oldRate = accountBalance.compareTo(BigDecimal.ZERO) != 0
                ? oldBaseValue.divide(accountBalance, 10, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculate unrealized gain/loss
        BigDecimal unrealizedGainLoss = newBaseValue.subtract(oldBaseValue);

        if (unrealizedGainLoss.compareTo(BigDecimal.ZERO) != 0) {
            logger.info(
                    "Account {} revaluation: {} {} -> {} {} (Gain/Loss: {} {})",
                    account.getCode(),
                    accountBalance,
                    account.getCurrency(),
                    newBaseValue,
                    baseCurrency,
                    unrealizedGainLoss,
                    baseCurrency);

            // Create and post revaluation journal entry
            GLTransaction postedTransaction = postRevaluationEntry(account, unrealizedGainLoss, asOfDate);

            // Create revaluation detail for audit trail
            GLRevaluationDetail detail = new GLRevaluationDetail(
                    revaluationRun,
                    account,
                    account.getCurrency(),
                    accountBalance,
                    oldRate,
                    newRate,
                    oldBaseValue,
                    newBaseValue,
                    unrealizedGainLoss);
            detail.setJournalTransaction(postedTransaction);
            glRevaluationDetailRepository.save(detail);

            // Update revaluation run counters
            revaluationRun.incrementAccountsRevalued();
            revaluationRun.addToTotalAdjustment(unrealizedGainLoss);
        }
    }

    /**
     * Creates and posts a revaluation journal entry.
     *
     * @param account            The account being revalued
     * @param unrealizedGainLoss The unrealized gain (positive) or loss (negative)
     * @param asOfDate           The revaluation date
     * @return The posted GLTransaction for audit trail linking
     */
    private GLTransaction postRevaluationEntry(GLAccount account, BigDecimal unrealizedGainLoss, LocalDate asOfDate) {
        // Look up the unrealized FX G/L account using operational account mapping
        UUID unrealizedGLAccountId = operationalGLAccountService
                .getOperationalGLAccount(OperationalGLAccountType.UNREALIZED_FX_GL);

        GLAccount unrealizedGLAccount = glAccountRepository.findById(unrealizedGLAccountId).orElseThrow(
                () -> new IllegalStateException(
                        "Unrealized FX G/L account not found: " + unrealizedGLAccountId
                                + ". Please configure UNREALIZED_FX_GL operational account mapping."));

        // Create journal entries
        List<PostTransactionCommand.JournalEntryCommand> entries = new ArrayList<>();

        // Determine debit/credit based on gain or loss
        boolean isGain = unrealizedGainLoss.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal absAmount = unrealizedGainLoss.abs();

        if (isGain) {
            // Gain: Debit foreign currency account, Credit unrealized G/L
            entries.add(
                    new PostTransactionCommand.JournalEntryCommand(
                            account.getId(),
                            absAmount,
                            BigDecimal.ZERO,
                            "FX revaluation gain",
                            asOfDate));
            entries.add(
                    new PostTransactionCommand.JournalEntryCommand(
                            unrealizedGLAccount.getId(),
                            BigDecimal.ZERO,
                            absAmount,
                            "FX revaluation gain - " + account.getCode(),
                            asOfDate));
        } else {
            // Loss: Debit unrealized G/L, Credit foreign currency account
            entries.add(
                    new PostTransactionCommand.JournalEntryCommand(
                            unrealizedGLAccount.getId(),
                            absAmount,
                            BigDecimal.ZERO,
                            "FX revaluation loss - " + account.getCode(),
                            asOfDate));
            entries.add(
                    new PostTransactionCommand.JournalEntryCommand(
                            account.getId(),
                            BigDecimal.ZERO,
                            absAmount,
                            "FX revaluation loss",
                            asOfDate));
        }

        // Create the transaction command
        PostTransactionCommand command = new PostTransactionCommand(
                GLTransactionType.CURRENCY_REVALUATION
                        .generateReferenceId(account.getCode() + "-" + asOfDate.toString()),
                "Currency revaluation for account " + account.getCode() + " as of " + asOfDate,
                asOfDate,
                baseCurrency, // Revaluation entries are always in base currency
                "system",
                entries);

        // Convert to entity and post
        GLTransaction transaction = GLEntityMapper.toEntity(command, glAccountService);
        glTransactionService.postTransaction(transaction);

        logger.info(
                "Posted revaluation entry for account {}: {} {} {}",
                account.getCode(),
                isGain ? "gain" : "loss",
                absAmount,
                baseCurrency);

        return transaction;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('gl:read')")
    public List<GLRevaluationRun> listRevaluationRuns(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return glRevaluationRunRepository.findByRevaluationDateBetween(from, to);
        }
        return glRevaluationRunRepository.findAllOrderByExecutedAtDesc();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('gl:read')")
    public Optional<GLRevaluationRun> getRevaluationRun(UUID id) {
        return glRevaluationRunRepository.findById(id);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('gl:read')")
    public List<GLRevaluationDetail> getRevaluationRunDetails(UUID runId) {
        if (!glRevaluationRunRepository.existsById(runId)) {
            throw new IllegalArgumentException("Revaluation run not found: " + runId);
        }
        return glRevaluationDetailRepository.findByRevaluationRunId(runId);
    }
}
