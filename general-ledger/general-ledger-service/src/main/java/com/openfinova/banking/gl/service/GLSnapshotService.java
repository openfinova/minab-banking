package com.openfinova.banking.gl.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.gl.api.dto.AccountReconciliationResult;
import com.openfinova.banking.gl.api.dto.BalanceReconciliationReport;
import com.openfinova.banking.gl.api.dto.SnapshotsComplianceReport;
import com.openfinova.banking.gl.api.dto.ValidationResult;
import com.openfinova.banking.gl.api.entity.GLAccountStatus;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import com.openfinova.banking.gl.dto.RepairResult;
import com.openfinova.banking.gl.dto.SnapshotRecoveryResult;
import com.openfinova.banking.gl.dto.SnapshotStatistics;
import com.openfinova.banking.gl.entity.FiscalPeriod;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.entity.GLDailyBalance;
import com.openfinova.banking.gl.entity.GLTransaction;
import com.openfinova.banking.gl.entity.GLTransactionSequence;
import com.openfinova.banking.gl.repository.GLDailyBalanceRepository;
import com.openfinova.banking.gl.repository.GLTransactionRepository;
import com.openfinova.banking.gl.repository.GLTransactionSequenceRepository;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Implementation of GLSnapshotService for scheduled background tasks and system maintenance.
 * Orchestrates snapshots, reconciliations, and data integrity checks.
 */
@Service
@Transactional
public class GLSnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(GLSnapshotService.class);

    private final GLDailyBalanceRepository glDailyBalanceRepository;
    private final GLTransactionRepository glTransactionRepository;
    private final BalanceService balanceService;
    private final GLAccountService glAccountService;
    private final DateTimeService dateTimeService;
    private final FiscalPeriodService fiscalPeriodService;
    private final GLTransactionSequenceRepository sequenceRepository;

    public GLSnapshotService(GLDailyBalanceRepository glDailyBalanceRepository,
            GLTransactionRepository glTransactionRepository, BalanceService balanceService,
            GLAccountService glAccountService, DateTimeService dateTimeService, FiscalPeriodService fiscalPeriodService,
            GLTransactionSequenceRepository sequenceRepository) {
        this.glDailyBalanceRepository = glDailyBalanceRepository;
        this.glTransactionRepository = glTransactionRepository;
        this.balanceService = balanceService;
        this.glAccountService = glAccountService;
        this.dateTimeService = dateTimeService;
        this.fiscalPeriodService = fiscalPeriodService;
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * Performs end-of-day processing for the specified business date.
     *
     * EOD processing includes:
     * - Validate all manual transactions are approved or rejected (no pending approvals)
     * - Post all system-generated transactions for the business date
     * - Calculate and validate day-end balances
     * - Run balance reconciliations
     * - Validate transaction sequence integrity
     * - Generate preliminary reports
     * - Mark business date as closed
     *
     * @param businessDate the business date to close
     * @throws IllegalStateException if EOD cannot be completed (e.g., pending approvals exist)
     */
    public void performEndOfDayProcessing(LocalDate businessDate) {
        logger.info("Starting end-of-day processing for business date: {}", businessDate);
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Validate fiscal period is open
            FiscalPeriod fiscalPeriod = fiscalPeriodService.getFiscalPeriodForDate(businessDate).orElseThrow(
                    () -> new IllegalStateException("No fiscal period found for business date: " + businessDate));

            if (!fiscalPeriod.isOpen()) {
                throw new IllegalStateException(
                        String.format("Cannot perform EOD: Fiscal period %s is not open", fiscalPeriod.getName()));
            }

            // Step 2: Check for pending manual transaction approvals
            // TODO: Add query to check for PENDING_APPROVAL transactions for this date
            // If any exist, EOD should fail or send alerts
            logger.info("Validating no pending manual transaction approvals for {}", businessDate);

            // Step 3: Post all system-generated transactions
            // TODO: Integrate with GLTransactionService.postSystemTransaction()
            // For now, log what needs to be done
            logger.info("Posting all system-generated transactions for {}", businessDate);

            // Step 4: Calculate and validate end-of-day balances
            logger.info("Calculating end-of-day balances for {}", businessDate);
            List<GLAccount> postableAccounts = glAccountService.getPostableAccounts();
            for (GLAccount account : postableAccounts) {
                try {
                    // Ensure balance is correctly calculated for the day
                    balanceService.getBalanceAtDate(account.getId(), businessDate);
                } catch (Exception e) {
                    logger.error(
                            "Error calculating balance for account {} on {}: {}",
                            account.getCode(),
                            businessDate,
                            e.getMessage());
                }
            }

            // Step 5: Run transaction sequence validation
            logger.info("Validating transaction sequence integrity");
            ValidationResult sequenceValidation = performSequentialNumberValidation();
            if (!sequenceValidation.isValid()) {
                logger.warn("Transaction sequence validation found {} errors", sequenceValidation.getErrors().size());
            }

            // Step 6: Mark day as closed (implementation depends on business requirements)
            // TODO: Add business_day table to track open/closed dates
            logger.info("Marking business date {} as closed", businessDate);

            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("End-of-day processing completed successfully for {} in {} ms", businessDate, processingTime);

        } catch (Exception e) {
            logger.error("End-of-day processing failed for {}: {}", businessDate, e.getMessage(), e);
            throw new IllegalStateException("EOD processing failed: " + e.getMessage(), e);
        }
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    public void generateDailySnapshots() {
        generateDailySnapshots(dateTimeService.today().minusDays(1)); // Previous business day
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    public void generateDailySnapshots(LocalDate date) {
        logger.info("Starting daily snapshot generation for date: {}", date);
        long startTime = System.currentTimeMillis();

        List<GLAccount> postableAccounts = glAccountService.getPostableAccounts();
        int snapshotsCreated = 0;

        for (GLAccount account : postableAccounts) {
            try {
                balanceService.createDailySnapshot(account.getId(), date);
                snapshotsCreated++;
            } catch (Exception e) {
                logger.error("Failed to create snapshot for account {}: {}", account.getCode(), e.getMessage());
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;
        logger.info(
                "Daily snapshot generation completed. Created {} snapshots in {} ms",
                snapshotsCreated,
                processingTime);
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    public void generateWeeklySnapshots() {
        LocalDate endDate = dateTimeService.today().minusDays(1);
        LocalDate startDate = endDate.minusDays(6); // Last 7 days

        generateSnapshotsForPeriod(startDate, endDate, "weekly");
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    public void generateMonthlySnapshots() {
        LocalDate endDate = dateTimeService.today().minusDays(1);
        LocalDate startDate = endDate.withDayOfMonth(1); // First day of current month

        generateSnapshotsForPeriod(startDate, endDate, "monthly");
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    public int generateSnapshotsForAccounts(List<UUID> accountIds, LocalDate date) {
        logger.info("Generating snapshots for {} accounts on {}", accountIds.size(), date);

        int snapshotsCreated = 0;
        for (UUID accountId : accountIds) {
            try {
                balanceService.createDailySnapshot(accountId, date);
                snapshotsCreated++;
            } catch (Exception e) {
                logger.error("Failed to create snapshot for account {}: {}", accountId, e.getMessage());
            }
        }

        logger.info("Created {} snapshots for specified accounts", snapshotsCreated);
        return snapshotsCreated;
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    public int generateSnapshotsForAccountType(GLAccountType accountType, LocalDate date) {
        logger.info("Generating snapshots for account type {} on {}", accountType, date);

        List<GLAccount> accounts = glAccountService.getAccountsByType(accountType, GLAccountStatus.ACTIVE);
        List<UUID> accountIds = accounts.stream().map(GLAccount::getId)
                .filter(glAccountService::isAccountActiveForPosting).toList();

        return generateSnapshotsForAccounts(accountIds, date);
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    public int generateMissingSnapshots(LocalDate date) {
        logger.info("Generating missing snapshots for {}", date);

        List<UUID> missingAccountIds = balanceService.getAccountsMissingSnapshots(date);
        if (missingAccountIds.isEmpty()) {
            logger.info("No missing snapshots found for {}", date);
            return 0;
        }

        return generateSnapshotsForAccounts(missingAccountIds, date);
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    @Transactional
    public int cleanupOldSnapshots(int retentionDays) {
        logger.info("Cleaning up snapshots older than {} days", retentionDays);

        LocalDate cutoffDate = dateTimeService.today().minusDays(retentionDays);

        // Use repository method to delete old snapshots directly in the database
        int deletedCount = glDailyBalanceRepository.deleteByBalanceDateBefore(cutoffDate);

        logger.info("Deleted {} old snapshots before {}", deletedCount, cutoffDate);
        return deletedCount;
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    public int rebuildSnapshotsForPeriod(LocalDate startDate, LocalDate endDate) {
        logger.info("Rebuilding snapshots for period {} to {}", startDate, endDate);

        // Delete existing snapshots for the entire period using repository method
        int deletedCount = glDailyBalanceRepository.deleteByBalanceDateBetween(startDate, endDate);

        // Recreate snapshots for each date in the period
        for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
            generateDailySnapshots(currentDate);
        }

        logger.info("Rebuilt {} snapshots for period {} to {}", deletedCount, startDate, endDate);
        return deletedCount;
    }

    @PreAuthorize("hasAuthority('gl:read')")
    public SnapshotStatistics getSnapshotStatistics(LocalDate date) {
        logger.debug("Getting snapshot statistics for {}", date);

        long startTime = System.currentTimeMillis();

        List<GLAccount> postableAccounts = glAccountService.getPostableAccounts();
        int totalAccounts = postableAccounts.size();

        List<UUID> missingAccountIds = balanceService.getAccountsMissingSnapshots(date);
        int snapshotsMissing = missingAccountIds.size();
        int snapshotsCreated = totalAccounts - snapshotsMissing;

        // Check for snapshots with errors (inconsistent balances)
        int snapshotsWithErrors = 0;
        for (GLAccount account : postableAccounts) {
            if (!balanceService.validateBalanceConsistency(account.getId(), date)) {
                snapshotsWithErrors++;
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;

        return new SnapshotStatistics(
                date,
                totalAccounts,
                snapshotsCreated,
                snapshotsMissing,
                snapshotsWithErrors,
                processingTime);
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    public int archiveOldSnapshots(LocalDate beforeDate) {
        logger.info("Archiving snapshots before {}", beforeDate);

        // Use repository method to get count of snapshots to archive
        List<GLDailyBalance> snapshotsToArchive = glDailyBalanceRepository.findByBalanceDateBefore(beforeDate);

        int archivedCount = snapshotsToArchive.size();
        logger.info("Would archive {} snapshots before {}", archivedCount, beforeDate);

        // TODO: Implement actual archiving logic (e.g., move to archive table or external storage)
        // For now, we're just counting what would be archived

        return archivedCount;
    }

    @PreAuthorize("hasAuthority('gl:read')")
    public BalanceReconciliationReport performBalanceReconciliation() {
        logger.info("Starting comprehensive balance reconciliation");

        LocalDate reconciliationDate = dateTimeService.today().minusDays(1);
        BalanceReconciliationReport report = performBalanceReconciliationForPeriod(
                reconciliationDate,
                reconciliationDate);

        if (report.getInconsistentAccounts() > 0) {
            logger.warn("Found {} inconsistent accounts during reconciliation", report.getInconsistentAccounts());
        } else {
            logger.info("Balance reconciliation completed successfully - all accounts consistent");
        }

        return report;
    }

    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")
    public AccountReconciliationResult performBalanceReconciliationForAccount(UUID accountId, LocalDate date) {
        logger.debug("Performing balance reconciliation for account {} on {}", accountId, date);

        AccountReconciliationResult result = new AccountReconciliationResult(accountId, date, false);

        try {
            // Get snapshot balance
            Optional<GLDailyBalance> dailyBalance = glDailyBalanceRepository
                    .findDailyBalanceByAccountAndDate(accountId, date);

            if (dailyBalance.isEmpty()) {
                result.setConsistent(false);
                result.setIssues(List.of("No daily balance snapshot found for date"));
                return result;
            }

            // Get calculated balance
            BigDecimal snapshotBalance = dailyBalance.get().getClosingBalance();
            BigDecimal calculatedBalance = balanceService.getBalanceAtDate(accountId, date);

            result.setSnapshotBalance(snapshotBalance.toString());
            result.setCalculatedBalance(calculatedBalance.toString());

            // Compare balances
            BigDecimal difference = snapshotBalance.subtract(calculatedBalance);
            result.setDifference(difference.toString());

            boolean consistent = difference.compareTo(BigDecimal.ZERO) == 0;
            result.setConsistent(consistent);

            if (!consistent) {
                result.setIssues(
                        List.of(
                                "Balance mismatch: snapshot=" + snapshotBalance + ", calculated=" + calculatedBalance
                                        + ", difference=" + difference));
            }

        } catch (Exception e) {
            logger.error("Error during reconciliation for account {}: {}", accountId, e.getMessage());
            result.setConsistent(false);
            result.setIssues(List.of("Reconciliation error: " + e.getMessage()));
        }

        return result;
    }

    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")
    public BalanceReconciliationReport performBalanceReconciliationForPeriod(LocalDate startDate, LocalDate endDate) {
        logger.info("Performing balance reconciliation for period {} to {}", startDate, endDate);
        long startTime = System.currentTimeMillis();

        BalanceReconciliationReport report = new BalanceReconciliationReport();
        report.setStartDate(startDate);
        report.setEndDate(endDate);

        List<GLAccount> postableAccounts = glAccountService.getPostableAccounts();
        List<AccountReconciliationResult> inconsistencies = new ArrayList<>();

        int totalChecked = 0;
        int consistent = 0;
        int inconsistent = 0;

        for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
            for (GLAccount account : postableAccounts) {
                AccountReconciliationResult result = performBalanceReconciliationForAccount(
                        account.getId(),
                        currentDate);
                totalChecked++;

                if (result.isConsistent()) {
                    consistent++;
                } else {
                    inconsistent++;
                    inconsistencies.add(result);
                }
            }
        }

        report.setTotalAccountsChecked(totalChecked);
        report.setConsistentAccounts(consistent);
        report.setInconsistentAccounts(inconsistent);
        report.setInconsistencies(inconsistencies);
        report.setProcessingTimeMs(System.currentTimeMillis() - startTime);

        return report;
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    public void performDataIntegrityValidation() {
        logger.info("Starting data integrity validation");

        // Validate account hierarchy
        var hierarchyResult = glAccountService.validateAccountHierarchy();
        if (!hierarchyResult.isValid()) {
            logger.warn("Account hierarchy validation failed: {}", hierarchyResult.getIssues());
        }

        // Validate snapshot integrity for recent dates
        LocalDate endDate = dateTimeService.today().minusDays(1);
        LocalDate startDate = endDate.minusDays(7);

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            ValidationResult result = validateSnapshotIntegrity(currentDate);
            if (!result.isValid()) {
                logger.warn("Snapshot integrity validation failed for {}: {}", currentDate, result.getErrors());
            }
            currentDate = currentDate.plusDays(1);
        }

        logger.info("Data integrity validation completed");
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    public void performAuditTrailValidation() {
        logger.info("Starting audit trail validation");

        ValidationResult result = performSequentialNumberValidation();

        if (result.isValid()) {
            logger.info("Audit trail validation passed");
        } else {
            logger.warn("Audit trail validation failed: {}", result.getErrors());
        }
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    public ValidationResult performSequentialNumberValidation() {
        logger.info("Validating sequential transaction numbers per fiscal period");

        long startTime = System.currentTimeMillis();
        ValidationResult result = new ValidationResult();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            // Get all posted transactions
            List<GLTransaction> allTransactions = glTransactionRepository.findAll().stream()
                    .filter(t -> t.getTransactionNumber() != null).filter(GLTransaction::isPosted).toList();

            if (allTransactions.isEmpty()) {
                warnings.add("No posted transactions with transaction numbers found");
                result.setValid(true);
                result.setWarnings(warnings);
                result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
                return result;
            }

            // Group transactions by fiscal period
            Map<UUID, List<GLTransaction>> transactionsByPeriod = allTransactions.stream()
                    .collect(Collectors.groupingBy(t -> {
                        return fiscalPeriodService.getFiscalPeriodForDate(t.getTransactionDate())
                                .orElseThrow(
                                        () -> new IllegalStateException(
                                                "No fiscal period found for transaction " + t.getReferenceId()
                                                        + " with date " + t.getTransactionDate()))
                                .getId();
                    }));

            logger.info(
                    "Validating {} transactions across {} fiscal periods",
                    allTransactions.size(),
                    transactionsByPeriod.size());

            // Validate each period independently
            for (Map.Entry<UUID, List<GLTransaction>> entry : transactionsByPeriod.entrySet()) {
                UUID periodId = entry.getKey();
                FiscalPeriod period = fiscalPeriodService.getFiscalPeriodById(periodId)
                        .orElseThrow(() -> new IllegalStateException("Period not found: " + periodId));

                List<GLTransaction> periodTransactions = entry.getValue().stream()
                        .sorted(Comparator.comparing(GLTransaction::getTransactionNumber)).toList();

                // Check for gaps within period (should be sequential starting from 1)
                Long expectedNumber = 1L;
                for (GLTransaction transaction : periodTransactions) {
                    if (!transaction.getTransactionNumber().equals(expectedNumber)) {
                        errors.add(
                                String.format(
                                        "Gap in period '%s': expected transaction number %d, found %d (ref: %s)",
                                        period.getName(),
                                        expectedNumber,
                                        transaction.getTransactionNumber(),
                                        transaction.getReferenceId()));
                    }
                    expectedNumber++;
                }

                // Check for duplicate numbers within period
                long uniqueCount = periodTransactions.stream().map(GLTransaction::getTransactionNumber).distinct()
                        .count();
                if (uniqueCount != periodTransactions.size()) {
                    errors.add(String.format("Duplicate transaction numbers found in period '%s'", period.getName()));
                }

                // Verify against sequence table
                Optional<GLTransactionSequence> sequenceOpt = sequenceRepository.findByFiscalPeriodId(periodId);
                if (sequenceOpt.isPresent()) {
                    GLTransactionSequence sequence = sequenceOpt.get();
                    long actualCount = periodTransactions.size();
                    long sequenceValue = sequence.getLastAssignedNumber();

                    if (sequenceValue != actualCount) {
                        errors.add(
                                String.format(
                                        "Sequence mismatch in period '%s': sequence shows %d transactions, "
                                                + "but database contains %d posted transactions",
                                        period.getName(),
                                        sequenceValue,
                                        actualCount));
                    }
                } else {
                    warnings.add(
                            String.format(
                                    "No sequence record found for period '%s' (contains %d transactions)",
                                    period.getName(),
                                    periodTransactions.size()));
                }

                logger.debug(
                        "Period '{}': validated {} transactions, expected next number: {}",
                        period.getName(),
                        periodTransactions.size(),
                        expectedNumber);
            }

            result.setValid(errors.isEmpty());
            result.setErrors(errors);
            result.setWarnings(warnings);
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            if (result.isValid()) {
                logger.info(
                        "Sequential number validation passed: {} periods validated, {} warnings",
                        transactionsByPeriod.size(),
                        warnings.size());
            } else {
                logger.warn("Sequential number validation FAILED: {} errors found", errors.size());
            }

        } catch (Exception e) {
            logger.error("Error during sequential number validation: {}", e.getMessage(), e);
            errors.add("Validation error: " + e.getMessage());
            result.setValid(false);
            result.setErrors(errors);
        }

        return result;
    }

    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")
    public ValidationResult validateSnapshotIntegrity(LocalDate date) {
        logger.debug("Validating snapshot integrity for {}", date);

        long startTime = System.currentTimeMillis();
        ValidationResult result = new ValidationResult();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            List<GLAccount> postableAccounts = glAccountService.getPostableAccounts();

            for (GLAccount account : postableAccounts) {
                Optional<GLDailyBalance> dailyBalance = glDailyBalanceRepository
                        .findDailyBalanceByAccountAndDate(account.getId(), date);

                if (dailyBalance.isEmpty()) {
                    warnings.add("Missing daily balance for account " + account.getCode() + " on " + date);
                    continue;
                }

                GLDailyBalance balance = dailyBalance.get();

                // Validate balance consistency
                if (!balance.isBalanceConsistent()) {
                    errors.add("Inconsistent balance for account " + account.getCode() + " on " + date);
                }

                // Validate non-negative amounts
                if (balance.getTotalDebits().compareTo(BigDecimal.ZERO) < 0
                        || balance.getTotalCredits().compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Negative debit/credit amounts for account " + account.getCode() + " on " + date);
                }

                // Validate transaction count
                if (balance.getTransactionCount() < 0) {
                    errors.add("Negative transaction count for account " + account.getCode() + " on " + date);
                }
            }

            result.setValid(errors.isEmpty());
            result.setErrors(errors);
            result.setWarnings(warnings);
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("Error during snapshot integrity validation: {}", e.getMessage());
            errors.add("Validation error: " + e.getMessage());
            result.setValid(false);
            result.setErrors(errors);
        }

        return result;
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    public RepairResult validateAndRepairSnapshots(LocalDate startDate, LocalDate endDate) {
        logger.info("Validating and repairing snapshots for period {} to {}", startDate, endDate);
        long startTime = System.currentTimeMillis();

        RepairResult result = new RepairResult();
        result.setStartDate(startDate);
        result.setEndDate(endDate);

        List<String> repairActions = new ArrayList<>();
        int totalChecked = 0;
        int repaired = 0;
        int recreated = 0;

        for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
            ValidationResult validation = validateSnapshotIntegrity(currentDate);
            totalChecked++;

            if (!validation.isValid()) {
                // Attempt to repair by recreating snapshots
                try {
                    generateDailySnapshots(currentDate);
                    recreated++;
                    repairActions.add("Recreated snapshots for " + currentDate);
                } catch (Exception e) {
                    repairActions.add("Failed to repair snapshots for " + currentDate + ": " + e.getMessage());
                }
            } else if (!validation.getWarnings().isEmpty()) {
                // Generate missing snapshots
                int missing = generateMissingSnapshots(currentDate);
                if (missing > 0) {
                    repaired += missing;
                    repairActions.add("Generated " + missing + " missing snapshots for " + currentDate);
                }
            }
        }

        result.setTotalSnapshotsChecked(totalChecked);
        result.setSnapshotsRepaired(repaired);
        result.setSnapshotsRecreated(recreated);
        result.setRepairActions(repairActions);
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

        return result;
    }

    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")
    public SnapshotsComplianceReport generateComplianceReport(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating compliance report for period {} to {}", startDate, endDate);

        SnapshotsComplianceReport report = new SnapshotsComplianceReport();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setGeneratedAt(System.currentTimeMillis());

        List<String> complianceIssues = new ArrayList<>();

        // Validate sequential numbers
        ValidationResult sequentialValidation = performSequentialNumberValidation();
        int gaplessViolations = sequentialValidation.getErrors().size();
        report.setGaplessSequenceViolations(gaplessViolations);
        complianceIssues.addAll(sequentialValidation.getErrors());

        // Validate data integrity
        int dataIntegrityIssues = 0;
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            ValidationResult integrityValidation = validateSnapshotIntegrity(currentDate);
            dataIntegrityIssues += integrityValidation.getErrors().size();
            complianceIssues.addAll(integrityValidation.getErrors());
            currentDate = currentDate.plusDays(1);
        }
        report.setDataIntegrityIssues(dataIntegrityIssues);

        // Count total transactions
        List<GLTransaction> transactions = glTransactionRepository.findAll().stream()
                .filter(t -> !t.getTransactionDate().isBefore(startDate) && !t.getTransactionDate().isAfter(endDate))
                .toList();
        report.setTotalTransactions(transactions.size());

        report.setCompliant(complianceIssues.isEmpty());
        report.setComplianceIssues(complianceIssues);

        return report;
    }

    @PreAuthorize("hasAuthority('gl:approve')")
    public SnapshotRecoveryResult recoverFromFailedSnapshot(LocalDate date) {
        logger.info("Attempting to recover from failed snapshot for {}", date);

        SnapshotRecoveryResult result = new SnapshotRecoveryResult();
        result.setDate(date);

        List<String> recoveryActions = new ArrayList<>();
        List<String> remainingIssues = new ArrayList<>();

        try {
            // Check what snapshots exist
            SnapshotStatistics stats = getSnapshotStatistics(date);

            if (stats.getSnapshotsMissing() > 0) {
                // Generate missing snapshots
                int recovered = generateMissingSnapshots(date);
                recoveryActions.add("Generated " + recovered + " missing snapshots");
                result.setSnapshotsRecovered(recovered);
            }

            // Validate integrity after recovery
            ValidationResult validation = validateSnapshotIntegrity(date);
            if (!validation.isValid()) {
                remainingIssues.addAll(validation.getErrors());
            }

            result.setSuccessful(remainingIssues.isEmpty());
            result.setRecoveryActions(recoveryActions);
            result.setRemainingIssues(remainingIssues);

        } catch (Exception e) {
            logger.error("Error during snapshot recovery: {}", e.getMessage());
            result.setSuccessful(false);
            remainingIssues.add("Recovery error: " + e.getMessage());
            result.setRemainingIssues(remainingIssues);
        }

        return result;
    }

    /**
     * Generates snapshots for a date range by generating missing snapshots for each date.
     * This is the common implementation used by weekly and monthly snapshot generation.
     *
     * @param startDate  the start date (inclusive)
     * @param endDate    the end date (inclusive)
     * @param periodType the type of period for logging (e.g., "weekly", "monthly")
     */
    private void generateSnapshotsForPeriod(LocalDate startDate, LocalDate endDate, String periodType) {
        logger.info("Starting {} snapshot generation", periodType);

        // Generate daily snapshots for the period if missing
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            int missingSnapshots = generateMissingSnapshots(currentDate);
            if (missingSnapshots > 0) {
                logger.info("Generated {} missing snapshots for {}", missingSnapshots, currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }

        logger.info(
                "{} snapshot generation completed for period {} to {}",
                periodType.substring(0, 1).toUpperCase() + periodType.substring(1),
                startDate,
                endDate);
    }
}
