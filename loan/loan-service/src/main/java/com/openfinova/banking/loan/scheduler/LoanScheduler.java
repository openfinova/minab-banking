package com.openfinova.banking.loan.scheduler;

import java.time.LocalDate;

import com.openfinova.banking.loan.service.LoanAccountScheduledTasks;
import com.openfinova.banking.setup.api.DateTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.openfinova.banking.loan.service.LoanAccountService;
import com.openfinova.banking.loan.service.LoanScheduleService;

/**
 * Scheduler for automated loan operations.
 *
 * Scheduled Tasks:
 * 1. Interest Accrual - Daily at 1:00 AM
 * 2. Overdue Detection - Daily at 2:00 AM
 * 3. Delinquency Status Update - Daily at 3:00 AM
 * 4. Provision Calculation - Monthly on 1st at 5:00 AM
 * 5. Quote Expiration - Daily at 4:00 AM
 * 6. Maturity Processing - Daily at 6:00 AM
 *
 * All tasks can be individually enabled/disabled via configuration properties.
 */
@Component
@ConditionalOnProperty(prefix = "loan.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoanScheduler {

    private static final Logger log = LoggerFactory.getLogger(LoanScheduler.class);

    private final LoanAccountService loanAccountService;
    private final LoanScheduleService scheduleService;
    private final LoanAccountScheduledTasks scheduledTasks;
    private final DateTimeService dateTimeService;

    public LoanScheduler(LoanAccountService loanAccountService, LoanScheduleService scheduleService,
            LoanAccountScheduledTasks scheduledTasks, DateTimeService dateTimeService) {
        this.loanAccountService = loanAccountService;
        this.scheduleService = scheduleService;
        this.scheduledTasks = scheduledTasks;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Processes daily interest accrual for all active loans.
     * Runs daily at 1:00 AM.
     *
     * This calculates and records interest charges for each active loan
     * based on the outstanding principal and interest rate.
     */
    @Scheduled(cron = "${loan.scheduler.interest-accrual.cron:0 0 1 * * *}")
    @ConditionalOnProperty(prefix = "loan.scheduler.interest-accrual", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void processInterestAccrual() {
        log.info("Starting scheduled loan interest accrual");
        try {
            LocalDate accrualDate = dateTimeService.today();
            int count = loanAccountService.processInterestAccrual(accrualDate);
            log.info("Processed interest accrual for {} loan accounts", count);
        } catch (Exception e) {
            log.error("Error during scheduled loan interest accrual", e);
        }
    }

    /**
     * Detects and marks overdue loan schedules.
     * Runs daily at 2:00 AM.
     *
     * Identifies loan payment schedules that have passed their due date
     * without full payment and marks them as overdue.
     */
    @Scheduled(cron = "${loan.scheduler.overdue-detection.cron:0 0 2 * * *}")
    @ConditionalOnProperty(prefix = "loan.scheduler.overdue-detection", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void processOverdueDetection() {
        log.info("Starting scheduled overdue detection");
        try {
            LocalDate currentDate = dateTimeService.today();
            int count = scheduleService.processOverdueSchedules(currentDate);
            log.info("Marked {} loan schedules as overdue", count);
        } catch (Exception e) {
            log.error("Error during scheduled overdue detection", e);
        }
    }

    /**
     * Updates delinquency status for all loans.
     * Runs daily at 3:00 AM.
     *
     * Calculates days past due and updates loan delinquency status
     * (Current, 30 DPD, 60 DPD, 90 DPD, etc.) for regulatory reporting.
     */
    @Scheduled(cron = "${loan.scheduler.delinquency-update.cron:0 0 3 * * *}")
    @ConditionalOnProperty(prefix = "loan.scheduler.delinquency-update", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void processDelinquencyStatusUpdate() {
        log.info("Starting scheduled delinquency status update");
        try {
            int count = scheduledTasks.updateDelinquencyStatus();
            log.info("Updated delinquency status for {} loan accounts", count);
        } catch (Exception e) {
            log.error("Error during scheduled delinquency status update", e);
        }
    }

    /**
     * Calculates loan loss provisions.
     * Runs monthly on the 1st at 5:00 AM.
     *
     * Calculates required provisions based on delinquency status
     * and expected credit losses (ECL) for regulatory compliance.
     */
    @Scheduled(cron = "${loan.scheduler.provision-calculation.cron:0 0 5 1 * *}")
    @ConditionalOnProperty(prefix = "loan.scheduler.provision-calculation", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void processProvisionCalculation() {
        log.info("Starting scheduled provision calculation");
        try {
            LocalDate calculationDate = dateTimeService.today();
            int count = scheduledTasks.calculateProvisions(calculationDate);
            log.info("Calculated provisions for {} loan accounts", count);
        } catch (Exception e) {
            log.error("Error during scheduled provision calculation", e);
        }
    }

    /**
     * Expires old settlement quotes.
     * Runs daily at 4:00 AM.
     *
     * Marks early settlement quotes that have passed their validity
     * period as expired, requiring new quotes to be generated.
     */
    @Scheduled(cron = "${loan.scheduler.quote-expiration.cron:0 0 4 * * *}")
    @ConditionalOnProperty(prefix = "loan.scheduler.quote-expiration", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void processQuoteExpiration() {
        log.info("Starting scheduled quote expiration");
        try {
            LocalDate currentDate = dateTimeService.today();
            int count = scheduledTasks.expireOldQuotes(currentDate);
            log.info("Expired {} old settlement quotes", count);
        } catch (Exception e) {
            log.error("Error during scheduled quote expiration", e);
        }
    }

    /**
     * Processes loan maturity.
     * Runs daily at 6:00 AM.
     *
     * Identifies loans that have reached their maturity date and
     * processes them accordingly (close if fully paid, mark as overdue if not).
     */
    @Scheduled(cron = "${loan.scheduler.maturity-processing.cron:0 0 6 * * *}")
    @ConditionalOnProperty(prefix = "loan.scheduler.maturity-processing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void processLoanMaturity() {
        log.info("Starting scheduled loan maturity processing");
        try {
            LocalDate currentDate = dateTimeService.today();
            int count = scheduledTasks.processMaturedLoans(currentDate);
            log.info("Processed {} matured loans", count);
        } catch (Exception e) {
            log.error("Error during scheduled loan maturity processing", e);
        }
    }

    /**
     * Generates payment reminders.
     * Runs daily at 8:00 AM.
     *
     * Identifies loans with upcoming payment due dates and generates
     * reminders for borrowers (3 days, 1 day before due date).
     */
    @Scheduled(cron = "${loan.scheduler.payment-reminders.cron:0 0 8 * * *}")
    @ConditionalOnProperty(prefix = "loan.scheduler.payment-reminders", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void generatePaymentReminders() {
        log.info("Starting scheduled payment reminder generation");
        try {
            LocalDate currentDate = dateTimeService.today();
            int reminderDays = 3; // Configurable
            int count = scheduleService.generatePaymentReminders(currentDate, reminderDays);
            log.info("Generated {} payment reminders", count);
        } catch (Exception e) {
            log.error("Error during scheduled payment reminder generation", e);
        }
    }

    /**
     * Processes automatic payments (if enabled).
     * Runs daily at 9:00 AM.
     *
     * Attempts to process automatic payments for loans with
     * auto-debit enabled and payment due today.
     */
    @Scheduled(cron = "${loan.scheduler.auto-payments.cron:0 0 9 * * *}")
    @ConditionalOnProperty(prefix = "loan.scheduler.auto-payments", name = "enabled", havingValue = "true", matchIfMissing = false)
    public void processAutomaticPayments() {
        log.info("Starting scheduled automatic payment processing");
        try {
            LocalDate currentDate = dateTimeService.today();
            int count = scheduledTasks.processAutomaticPayments(currentDate);
            log.info("Processed {} automatic payments", count);
        } catch (Exception e) {
            log.error("Error during scheduled automatic payment processing", e);
        }
    }

    /**
     * Updates loan performance metrics.
     * Runs daily at 7:00 AM.
     *
     * Calculates and updates portfolio-level metrics like
     * portfolio at risk (PAR), default rates, etc.
     */
    @Scheduled(cron = "${loan.scheduler.performance-metrics.cron:0 0 7 * * *}")
    @ConditionalOnProperty(prefix = "loan.scheduler.performance-metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void updatePerformanceMetrics() {
        log.info("Starting scheduled performance metrics update");
        try {
            LocalDate currentDate = dateTimeService.today();
            scheduledTasks.updatePerformanceMetrics(currentDate);
            log.info("Successfully updated loan performance metrics");
        } catch (Exception e) {
            log.error("Error during scheduled performance metrics update", e);
        }
    }

    /**
     * Cleans up old loan data.
     * Runs monthly on the 1st at 2:00 AM.
     *
     * Archives or deletes old loan records beyond retention period
     * (closed loans older than X years).
     */
    @Scheduled(cron = "${loan.scheduler.data-cleanup.cron:0 0 2 1 * *}")
    @ConditionalOnProperty(prefix = "loan.scheduler.data-cleanup", name = "enabled", havingValue = "true", matchIfMissing = false)
    public void cleanupOldLoanData() {
        log.info("Starting scheduled loan data cleanup");
        try {
            int retentionYears = 7; // Configurable - regulatory requirement
            int count = scheduledTasks.cleanupOldLoans(retentionYears);
            log.info("Cleaned up {} old loan records (retention: {} years)", count, retentionYears);
        } catch (Exception e) {
            log.error("Error during scheduled loan data cleanup", e);
        }
    }
}
