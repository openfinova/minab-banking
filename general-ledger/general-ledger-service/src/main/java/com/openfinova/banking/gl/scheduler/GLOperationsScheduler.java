package com.openfinova.banking.gl.scheduler;

import com.openfinova.banking.gl.api.dto.BalanceReconciliationReport;
import com.openfinova.banking.gl.api.dto.ValidationResult;
import com.openfinova.banking.gl.service.GLOperationsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduled tasks for GL operational processes and validations.
 *
 * This scheduler handles critical GL operations including:
 * - End-of-day processing (business day closure)
 * - Balance reconciliation (daily)
 * - Data integrity validation (weekly)
 * - Audit trail validation (weekly)
 * - Sequential numbering validation (daily)
 *
 * All scheduled tasks can be disabled via configuration properties.
 * Manual execution is available through REST endpoints.
 *
 * Schedule Overview:
 * 23:59 - End-of-day processing (close business day)
 * 04:00 - Balance reconciliation
 * 05:00 Sun - Data integrity validation
 * 06:00 Sun - Audit trail validation
 * 07:00 - Sequential number validation
 */
@Component
@ConditionalOnProperty(prefix = "gl.operations-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GLOperationsScheduler {

    private static final Logger log = LoggerFactory.getLogger(GLOperationsScheduler.class);

    // Per-task enable flags. @ConditionalOnProperty cannot be applied at method level —
    // it is a bean-registration condition and is silently ignored on methods. These
    // @Value fields provide the correct runtime guard instead.
    @Value("${gl.operations-scheduler.eod-processing.enabled:true}")
    private boolean eodProcessingEnabled;

    @Value("${gl.operations-scheduler.balance-reconciliation.enabled:true}")
    private boolean balanceReconciliationEnabled;

    @Value("${gl.operations-scheduler.data-integrity.enabled:true}")
    private boolean dataIntegrityEnabled;

    @Value("${gl.operations-scheduler.audit-trail.enabled:true}")
    private boolean auditTrailEnabled;

    @Value("${gl.operations-scheduler.sequential-validation.enabled:true}")
    private boolean sequentialValidationEnabled;

    private final GLOperationsService operationsService;

    public GLOperationsScheduler(GLOperationsService operationsService) {
        this.operationsService = operationsService;
    }

    /**
     * Performs end-of-day processing.
     * Runs at 11:59 PM every day.
     *
     * EOD processing includes:
     * - Posting all pending system-generated transactions for the day
     * - Running daily balance calculations
     * - Performing daily reconciliations
     * - Validating all transactions are posted
     * - Preparing for next business day
     *
     * Cron: 0 59 23 * * * (runs at 23:59:00)
     */
    @Scheduled(cron = "${gl.operations-scheduler.eod-processing.cron:0 59 23 * * *}")
    public void performEndOfDayProcessing() {
        if (!eodProcessingEnabled) {
            log.debug("End-of-day processing is disabled via gl.operations-scheduler.eod-processing.enabled");
            return;
        }
        log.info("Starting scheduled end-of-day processing");

        try {
            LocalDate businessDate = LocalDate.now();
            operationsService.performEndOfDayProcessing(businessDate);
            log.info("Successfully completed end-of-day processing for {}", businessDate);
        } catch (Exception e) {
            log.error("Error during scheduled end-of-day processing", e);
            // Critical error - this should be escalated/alerted
            // TODO: Integrate with alerting system
        }
    }

    /**
     * Performs balance reconciliation.
     * Runs at 4:00 AM every day.
     *
     * Compares snapshot-based balances with transaction-based calculations
     * to detect any inconsistencies.
     *
     * Cron: 0 0 4 * * *
     */
    @Scheduled(cron = "${gl.operations-scheduler.balance-reconciliation.cron:0 0 4 * * *}")
    public void performBalanceReconciliation() {
        if (!balanceReconciliationEnabled) {
            log.debug("Balance reconciliation is disabled via gl.operations-scheduler.balance-reconciliation.enabled");
            return;
        }
        log.info("Starting scheduled balance reconciliation");

        try {
            BalanceReconciliationReport report = operationsService.performBalanceReconciliation();

            if (report.getInconsistentAccounts() > 0) {
                log.warn(
                        "Balance reconciliation found {} inconsistent accounts out of {} checked",
                        report.getInconsistentAccounts(),
                        report.getTotalAccountsChecked());
                // TODO: Send alert for inconsistencies
            } else {
                log.info(
                        "Balance reconciliation completed successfully: all {} accounts consistent",
                        report.getTotalAccountsChecked());
            }
        } catch (Exception e) {
            log.error("Error during scheduled balance reconciliation", e);
        }
    }

    /**
     * Performs data integrity validation.
     * Runs at 5:00 AM every Sunday.
     *
     * Checks for:
     * - Orphaned journal entries
     * - Illegal transaction states
     * - Unbalanced transactions
     * - Cross-module consistency
     *
     * Cron: 0 0 5 * * SUN
     */
    @Scheduled(cron = "${gl.operations-scheduler.data-integrity.cron:0 0 5 * * SUN}")
    public void performDataIntegrityValidation() {
        if (!dataIntegrityEnabled) {
            log.debug("Data integrity validation is disabled via gl.operations-scheduler.data-integrity.enabled");
            return;
        }
        log.info("Starting scheduled data integrity validation");

        try {
            operationsService.performDataIntegrityValidation();
            log.info("Successfully completed scheduled data integrity validation");
        } catch (Exception e) {
            log.error("Error during scheduled data integrity validation", e);
            // TODO: Send alert for data integrity issues
        }
    }

    /**
     * Performs audit trail validation.
     * Runs at 6:00 AM every Sunday.
     *
     * Validates sequential transaction numbers for regulatory compliance.
     *
     * Cron: 0 0 6 * * SUN
     */
    @Scheduled(cron = "${gl.operations-scheduler.audit-trail.cron:0 0 6 * * SUN}")
    public void performAuditTrailValidation() {
        if (!auditTrailEnabled) {
            log.debug("Audit trail validation is disabled via gl.operations-scheduler.audit-trail.enabled");
            return;
        }
        log.info("Starting scheduled audit trail validation");

        try {
            operationsService.performAuditTrailValidation();
            log.info("Successfully completed scheduled audit trail validation");
        } catch (Exception e) {
            log.error("Error during scheduled audit trail validation", e);
            // TODO: Send alert for audit trail issues
        }
    }

    /**
     * Validates sequential transaction numbers.
     * Runs at 7:00 AM every day.
     *
     * Ensures gapless sequence for regulatory compliance:
     * - No gaps in transaction numbers
     * - Chronological order maintained
     * - No duplicates exist
     *
     * Cron: 0 0 7 * * *
     */
    @Scheduled(cron = "${gl.operations-scheduler.sequential-validation.cron:0 0 7 * * *}")
    public void performSequentialNumberValidation() {
        if (!sequentialValidationEnabled) {
            log.debug(
                    "Sequential number validation is disabled via gl.operations-scheduler.sequential-validation.enabled");
            return;
        }
        log.info("Starting scheduled sequential number validation");

        try {
            ValidationResult result = operationsService.performSequentialNumberValidation();

            if (!result.isValid()) {
                log.warn(
                        "Sequential number validation found {} errors and {} warnings",
                        result.getErrors().size(),
                        result.getWarnings().size());
                // TODO: Send alert for sequence violations
            } else {
                log.info("Sequential number validation completed successfully");
            }
        } catch (Exception e) {
            log.error("Error during scheduled sequential number validation", e);
        }
    }
}
