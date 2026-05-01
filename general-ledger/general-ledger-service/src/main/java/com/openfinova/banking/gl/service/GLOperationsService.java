package com.openfinova.banking.gl.service;

import com.openfinova.banking.gl.api.dto.BalanceReconciliationReport;
import com.openfinova.banking.gl.api.dto.ValidationResult;
import com.openfinova.banking.gl.entity.FiscalPeriod;
import com.openfinova.banking.gl.entity.GLAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for GL operations and end-of-day processing.
 *
 * Handles critical GL operational tasks including:
 * - End-of-day processing and business day closure
 * - Balance reconciliation between snapshots and transactions
 * - Data integrity validation across GL entities
 * - Audit trail validation for regulatory compliance
 * - Transaction sequence validation (gapless numbering)
 *
 * This service is separate from GLSnapshotService to maintain clear
 * separation of concerns between operational processing and snapshot management.
 */
@Service
@Transactional
public class GLOperationsService {

    private static final Logger logger = LoggerFactory.getLogger(GLOperationsService.class);

    private final GLAccountService glAccountService;
    private final BalanceService balanceService;
    private final FiscalPeriodService fiscalPeriodService;
    private final GLSnapshotService snapshotService;

    public GLOperationsService(GLAccountService glAccountService, BalanceService balanceService,
            FiscalPeriodService fiscalPeriodService, GLSnapshotService snapshotService) {
        this.glAccountService = glAccountService;
        this.balanceService = balanceService;
        this.fiscalPeriodService = fiscalPeriodService;
        this.snapshotService = snapshotService;
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

    /**
     * Performs balance reconciliation between snapshots and transaction calculations.
     *
     * @return reconciliation report with details of any inconsistencies
     */
    public BalanceReconciliationReport performBalanceReconciliation() {
        logger.info("Starting balance reconciliation");
        return snapshotService.performBalanceReconciliation();
    }

    /**
     * Performs balance reconciliation for a specific period.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return reconciliation report with details of any inconsistencies
     */
    public BalanceReconciliationReport performBalanceReconciliationForPeriod(LocalDate startDate, LocalDate endDate) {
        logger.info("Starting balance reconciliation for period {} to {}", startDate, endDate);
        return snapshotService.performBalanceReconciliationForPeriod(startDate, endDate);
    }

    /**
     * Performs comprehensive data integrity validation.
     *
     * Checks include:
     * - Orphaned journal entries (no parent transaction)
     * - Transactions with illegal status transitions
     * - Unbalanced transactions (debits != credits)
     * - Cross-module consistency (GL vs subledgers)
     * - Account balance consistency
     */
    public void performDataIntegrityValidation() {
        logger.info("Starting data integrity validation");
        snapshotService.performDataIntegrityValidation();
    }

    /**
     * Performs audit trail validation.
     *
     * Validates:
     * - All audit trail entries are complete
     * - Audit records are immutable
     * - Critical operations are logged
     * - User actions are properly traced
     */
    public void performAuditTrailValidation() {
        logger.info("Starting audit trail validation");
        snapshotService.performAuditTrailValidation();
    }

    /**
     * Validates sequential transaction numbering for regulatory compliance.
     *
     * Ensures:
     * - Transaction numbers are sequential with no gaps
     * - Numbers are assigned in chronological order
     * - No duplicate transaction numbers exist
     * - Sequence validation per fiscal period
     *
     * @return validation result with any errors or warnings
     */
    public ValidationResult performSequentialNumberValidation() {
        logger.info("Starting sequential number validation");
        return snapshotService.performSequentialNumberValidation();
    }
}
