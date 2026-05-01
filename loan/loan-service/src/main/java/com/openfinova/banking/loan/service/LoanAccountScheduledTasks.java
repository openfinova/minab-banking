package com.openfinova.banking.loan.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.openfinova.banking.loan.api.entity.DelinquencyBucket;
import com.openfinova.banking.loan.api.entity.LoanStatus;
import com.openfinova.banking.loan.api.entity.ProvisionStage;
import com.openfinova.banking.loan.api.entity.SettlementStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.loan.entity.EarlySettlement;
import com.openfinova.banking.loan.entity.LoanAccount;
import com.openfinova.banking.loan.entity.LoanProvision;
import com.openfinova.banking.loan.entity.LoanSchedule;
import com.openfinova.banking.loan.repository.EarlySettlementRepository;
import com.openfinova.banking.loan.repository.LoanAccountRepository;
import com.openfinova.banking.loan.repository.LoanProvisionRepository;
import com.openfinova.banking.loan.repository.LoanScheduleRepository;

/**
 * Scheduled task implementations for loan account maintenance operations.
 *
 * This component is separated from the main LoanAccountService to avoid circular
 * dependencies with the LoanScheduler. It contains all scheduled background tasks
 * that run periodically to maintain loan account data integrity and compliance.
 *
 * Key Responsibilities:
 * - Delinquency tracking: Updates days past due and delinquency buckets
 * - Provision calculation: Calculates loan loss provisions per regulatory requirements
 * - Quote expiration: Expires old early settlement quotes
 * - Loan maturity: Processes loans that have reached maturity date
 * - Performance metrics: Calculates portfolio-level metrics like PAR ratios
 * - Data cleanup: Archives or removes old closed loan records
 *
 * All methods are transactional and include error handling to ensure one failure
 * does not prevent processing of other loans.
 *
 * @see com.openfinova.banking.loan.scheduler.LoanScheduler
 * @see LoanAccountService
 */
@Component
@Transactional
public class LoanAccountScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(LoanAccountScheduledTasks.class);

    private final LoanAccountRepository loanAccountRepository;
    private final LoanScheduleRepository scheduleRepository;
    private final LoanProvisionRepository provisionRepository;
    private final EarlySettlementRepository settlementRepository;
    private final LoanAccountService loanAccountService;

    public LoanAccountScheduledTasks(LoanAccountRepository loanAccountRepository,
            LoanScheduleRepository scheduleRepository, LoanProvisionRepository provisionRepository,
            EarlySettlementRepository settlementRepository, LoanAccountService loanAccountService) {
        this.loanAccountRepository = loanAccountRepository;
        this.scheduleRepository = scheduleRepository;
        this.provisionRepository = provisionRepository;
        this.settlementRepository = settlementRepository;
        this.loanAccountService = loanAccountService;
    }

    /**
     * Updates delinquency status for all active loans.
     *
     * This method calculates days past due for each active loan by finding the oldest
     * overdue schedule and computing the number of days since its due date. Loans are
     * then categorized into delinquency buckets:
     * - CURRENT: No overdue payments
     * - DPD_1_30: 1-30 days past due
     * - DPD_31_60: 31-60 days past due
     * - DPD_61_90: 61-90 days past due
     * - DPD_91_180: 91-180 days past due
     * - DPD_180_PLUS: More than 180 days past due
     *
     * Delinquency tracking is critical for:
     * - Risk management and early intervention
     * - Regulatory reporting requirements
     * - Provision calculation
     * - Collection prioritization
     * - Portfolio quality monitoring
     *
     * The method processes each loan individually with error handling to ensure
     * one failure does not prevent processing of other loans.
     *
     * Typically scheduled to run daily.
     *
     * @return the number of loans with updated delinquency status
     */
    public int updateDelinquencyStatus() {
        log.info("Starting delinquency status update for all active loans");

        List<LoanAccount> activeLoans = loanAccountRepository.findByStatus(LoanStatus.ACTIVE, Pageable.unpaged())
                .getContent();
        int updatedCount = 0;

        for (LoanAccount loan : activeLoans) {
            try {
                // Calculate days past due based on overdue schedules
                long overdueSchedules = scheduleRepository.countOverdueSchedulesByLoanAccount(loan.getId());

                if (overdueSchedules > 0) {
                    // Get the oldest overdue schedule
                    List<LoanSchedule> overdue = scheduleRepository.findOverdueSchedulesByLoanAccount(loan.getId());
                    if (!overdue.isEmpty()) {
                        LoanSchedule oldestOverdue = overdue.get(0);
                        long daysPastDue = ChronoUnit.DAYS.between(oldestOverdue.getDueDate(), LocalDate.now());

                        DelinquencyBucket bucket = DelinquencyBucket.fromDaysPastDue((int) daysPastDue);
                        loanAccountRepository
                                .updateDelinquencyStatus(loan.getId(), (int) daysPastDue, bucket, Instant.now());
                        updatedCount++;
                    }
                } else if (loan.getDaysPastDue() != null && loan.getDaysPastDue() > 0) {
                    loanAccountRepository
                            .updateDelinquencyStatus(loan.getId(), 0, DelinquencyBucket.CURRENT, Instant.now());
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("Error updating delinquency status for loan {}", loan.getId(), e);
            }
        }

        log.info("Updated delinquency status for {} loans", updatedCount);
        return updatedCount;
    }

    /**
     * Calculates loan loss provisions for all active loans.
     *
     * Provisions are reserves set aside to cover potential loan losses. This method
     * calculates provisions based on the loan's delinquency status using a tiered
     * approach aligned with IFRS 9 and Basel III requirements:
     *
     * Provision Rates by Days Past Due:
     * - Current (0 days): 1% - Stage 1 (Performing)
     * - 1-30 days: 5% - Stage 2 (Underperforming)
     * - 31-60 days: 25% - Stage 2 (Underperforming)
     * - 61-90 days: 50% - Stage 3 (Non-performing)
     * - 90+ days: 100% - Stage 3 (Non-performing)
     *
     * The provision amount is calculated as:
     * Provision = Outstanding Principal × (Provision Rate ÷ 100), where the rate is stored as a percentage (e.g. 5 for 5%).
     *
     * Each provision record includes:
     * - Provision date (calculation date)
     * - Provision amount (reserve amount)
     * - Provision rate (percentage applied)
     * - Provision stage (IFRS 9 classification)
     * - Outstanding balance (at calculation time)
     * - Currency
     *
     * Provisions are used for:
     * - Financial reporting and balance sheet accuracy
     * - Regulatory capital adequacy calculations
     * - Risk assessment and stress testing
     * - Management decision making
     *
     * Typically scheduled to run monthly or at period end.
     *
     * @param calculationDate the date for which provisions are calculated
     * @return the number of loans for which provisions were calculated
     */
    public int calculateProvisions(LocalDate calculationDate) {
        log.info("Starting provision calculation for date: {}", calculationDate);

        List<LoanAccount> activeLoans = loanAccountRepository.findByStatus(LoanStatus.ACTIVE, Pageable.unpaged())
                .getContent();
        int calculatedCount = 0;

        for (LoanAccount loan : activeLoans) {
            try {
                BigDecimal provisionAmount = calculateProvisionAmount(loan);
                BigDecimal provisionRate = calculateProvisionRate(loan);

                LoanProvision provision = new LoanProvision();
                provision.setLoanAccount(loan);
                provision.setProvisionDate(calculationDate);
                provision.setProvisionAmount(provisionAmount);
                provision.setProvisionRate(provisionRate);
                provision.setProvisionStage(determineProvisionStage(loan));
                provision.setOutstandingBalance(loan.getOutstandingPrincipal());
                provision.setCurrency(loan.getCurrency());

                provisionRepository.save(provision);
                calculatedCount++;
            } catch (Exception e) {
                log.error("Error calculating provision for loan {}", loan.getId(), e);
            }
        }

        log.info("Calculated provisions for {} loans", calculatedCount);
        return calculatedCount;
    }

    /**
     * Expires old early settlement quotes that have passed their validity period.
     *
     * Early settlement quotes have a limited validity period (typically 7-30 days)
     * because they are based on current interest rates, outstanding balances, and
     * market conditions that may change over time.
     *
     * This method:
     * - Finds all quotes in QUOTE status
     * - Checks if the validUntil date has passed
     * - Updates expired quotes to EXPIRED status
     *
     * Expired quotes cannot be used for settlement and require the borrower to
     * request a new quote with current calculations.
     *
     * Quote expiration is important for:
     * - Ensuring settlement amounts reflect current conditions
     * - Preventing use of outdated pricing
     * - Maintaining data accuracy
     * - Regulatory compliance
     *
     * Typically scheduled to run daily.
     *
     * @param currentDate the date to check against quote validity
     * @return the number of quotes that were expired
     */
    public int expireOldQuotes(LocalDate currentDate) {
        log.info("Starting expiration of old settlement quotes for date: {}", currentDate);

        List<EarlySettlement> activeQuotes = settlementRepository
                .findByStatus(SettlementStatus.QUOTE, Pageable.unpaged()).getContent();
        int expiredCount = 0;

        for (EarlySettlement quote : activeQuotes) {
            try {
                if (quote.getValidUntil().isBefore(currentDate)) {
                    quote.setStatus(SettlementStatus.EXPIRED);
                    settlementRepository.save(quote);
                    expiredCount++;
                }
            } catch (Exception e) {
                log.error("Error expiring quote {}", quote.getId(), e);
            }
        }

        log.info("Expired {} old settlement quotes", expiredCount);
        return expiredCount;
    }

    /**
     * Processes loans that have reached their maturity date.
     *
     * Loan maturity is the date when the loan term ends and all amounts should be
     * fully repaid. This method handles matured loans based on their payment status:
     *
     * Fully Paid Loans (totalOutstanding = 0):
     * - Status changed to CLOSED
     * - Loan lifecycle completed successfully
     * - No further action required
     *
     * Loans with Outstanding Balance:
     * - Remain in ACTIVE status
     * - Logged as warning for manual review
     * - May require collection action or restructuring
     * - Delinquency tracking continues
     *
     * Maturity processing is critical for:
     * - Accurate loan status tracking
     * - Portfolio reporting
     * - Identifying problem loans
     * - Triggering collection workflows
     * - Regulatory compliance
     *
     * Loans that mature with outstanding balances typically indicate:
     * - Missed final payment
     * - Balloon payment not made
     * - Accumulated arrears
     * - Need for loan restructuring
     *
     * Typically scheduled to run daily.
     *
     * @param currentDate the date to check for matured loans
     * @return the number of matured loans processed
     */
    public int processMaturedLoans(LocalDate currentDate) {
        log.info("Starting matured loan processing for date: {}", currentDate);

        List<LoanAccount> maturedLoans = loanAccountRepository.findByMaturityDate(currentDate);
        int processedCount = 0;

        for (LoanAccount loan : maturedLoans) {
            try {
                BigDecimal totalOutstanding = loan.getTotalOutstanding();

                if (totalOutstanding.compareTo(BigDecimal.ZERO) == 0) {
                    loanAccountService.closeLoanAccount(loan.getId(), currentDate, "system-maturity-job");
                    log.info("Closed fully paid matured loan: {}", loan.getLoanAccountNumber());
                } else {
                    // Mark as needing attention - keep ACTIVE but update delinquency
                    log.warn("Matured loan with outstanding balance: {}", loan.getLoanAccountNumber());
                }
                processedCount++;
            } catch (Exception e) {
                log.error("Error processing matured loan {}", loan.getId(), e);
            }
        }

        log.info("Processed {} matured loans", processedCount);
        return processedCount;
    }

    /**
     * Processes automatic payments for loans with auto-debit enabled.
     *
     * This method would handle automatic payment processing for loans where
     * borrowers have authorized automatic deductions from their accounts.
     *
     * Typical automatic payment workflow:
     * - Identify loans with upcoming due dates
     * - Check if auto-payment is enabled
     * - Verify sufficient funds in linked account
     * - Initiate payment transaction
     * - Update loan balances and schedules
     * - Send confirmation notifications
     * - Handle failed payments (retry logic, notifications)
     *
     * Benefits of automatic payments:
     * - Reduces missed payments and delinquency
     * - Improves borrower convenience
     * - Lowers operational costs
     * - Increases on-time payment rates
     *
     * Currently not implemented - placeholder for future functionality.
     *
     * Typically scheduled to run daily, processing payments due within next 1-3 days.
     *
     * @param currentDate the date to process automatic payments for
     * @return the number of automatic payments processed
     */
    public int processAutomaticPayments(LocalDate currentDate) {
        log.info("Starting automatic payment processing for date: {}", currentDate);
        // TODO: Implement automatic payment processing
        log.info("Automatic payment processing not yet implemented");
        return 0;
    }

    /**
     * Updates portfolio-level performance metrics for management reporting.
     *
     * This method calculates key performance indicators for the entire loan portfolio:
     *
     * Portfolio at Risk (PAR) Ratios:
     * - PAR30: Outstanding principal of loans 30+ days past due
     * - PAR60: Outstanding principal of loans 60+ days past due
     * - PAR90: Outstanding principal of loans 90+ days past due
     *
     * PAR ratios are calculated as:
     * PAR = Sum of Outstanding Principal (loans >= X days overdue) / Total Portfolio
     *
     * Other Metrics:
     * - Total portfolio size (sum of all outstanding principal)
     * - Total number of active loans
     * - Number of written-off loans
     * - Default rate (written-off loans / total loans)
     *
     * These metrics are essential for:
     * - Management decision making
     * - Board reporting
     * - Investor relations
     * - Regulatory reporting
     * - Risk assessment
     * - Trend analysis
     * - Benchmarking against industry standards
     *
     * PAR ratios are industry-standard indicators of portfolio quality:
     * - PAR30 < 5%: Excellent
     * - PAR30 5-10%: Good
     * - PAR30 10-15%: Acceptable
     * - PAR30 > 15%: Concerning
     *
     * Currently logs metrics only. Could be extended to:
     * - Store metrics in database for historical tracking
     * - Generate automated reports
     * - Trigger alerts for threshold breaches
     * - Feed into dashboards and analytics
     *
     * Typically scheduled to run daily or weekly.
     *
     * @param currentDate the date for which metrics are calculated
     */
    public void updatePerformanceMetrics(LocalDate currentDate) {
        log.info("Starting performance metrics update for date: {}", currentDate);

        try {
            List<LoanAccount> allActiveLoans = loanAccountRepository.findByStatus(LoanStatus.ACTIVE, Pageable.unpaged())
                    .getContent();
            BigDecimal totalPortfolio = allActiveLoans.stream().map(LoanAccount::getOutstandingPrincipal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal par30 = calculatePAR(allActiveLoans, 30);
            BigDecimal par60 = calculatePAR(allActiveLoans, 60);
            BigDecimal par90 = calculatePAR(allActiveLoans, 90);

            long totalLoans = loanAccountRepository.count();
            long writtenOffLoans = loanAccountRepository.countByStatus(LoanStatus.WRITTEN_OFF);
            double defaultRate = totalLoans > 0 ? (double) writtenOffLoans / totalLoans * 100 : 0;

            log.info(
                    "Performance Metrics - Total Portfolio: {}, PAR30: {}, PAR60: {}, PAR90: {}, Default Rate: {}%",
                    totalPortfolio,
                    par30,
                    par60,
                    par90,
                    String.format("%.2f", defaultRate));

        } catch (Exception e) {
            log.error("Error updating performance metrics", e);
        }
    }

    /**
     * Cleans up old closed loan records based on retention policy.
     *
     * This method identifies closed loans that are older than the specified retention
     * period and prepares them for archival or deletion. Proper data retention is
     * important for:
     *
     * Compliance Requirements:
     * - Regulatory retention periods (typically 5-7 years)
     * - Tax audit requirements
     * - Legal discovery obligations
     * - Consumer protection laws
     *
     * Operational Benefits:
     * - Improved database performance
     * - Reduced storage costs
     * - Faster queries on active data
     * - Simplified data management
     *
     * The method currently:
     * - Identifies closed loans older than retention period
     * - Logs loans that would be archived
     * - Does NOT actually delete data (commented out for safety)
     *
     * Before enabling actual deletion, ensure:
     * - Data is backed up or archived to separate storage
     * - Regulatory retention requirements are met
     * - Legal review is completed
     * - Audit trail is maintained
     * - Related records (payments, schedules) are handled
     *
     * Best Practice Approach:
     * 1. Archive to separate database or data warehouse
     * 2. Verify archive integrity
     * 3. Only then delete from operational database
     * 4. Maintain archive access for compliance
     *
     * Typically scheduled to run monthly or quarterly.
     *
     * @param retentionYears the number of years to retain closed loan records
     * @return the number of old loan records identified for cleanup
     */
    public int cleanupOldLoans(int retentionYears) {
        log.info("Starting cleanup of old loans with retention period: {} years", retentionYears);

        LocalDate cutoffDate = LocalDate.now().minusYears(retentionYears);
        List<LoanAccount> oldClosedLoans = loanAccountRepository.findClosedLoansBeforeDate(cutoffDate);
        int cleanedCount = 0;

        for (LoanAccount loan : oldClosedLoans) {
            try {
                log.info("Would archive old loan: {}", loan.getLoanAccountNumber());
                // Actual deletion commented out for safety
                // loanAccountRepository.delete(loan);
                cleanedCount++;
            } catch (Exception e) {
                log.error("Error cleaning up old loan {}", loan.getId(), e);
            }
        }

        log.info("Identified {} old loan records for cleanup", cleanedCount);
        return cleanedCount;
    }

    // Helper methods

    /**
     * Calculates the provision amount for a loan.
     *
     * <p>Formula: {@code outstandingPrincipal × (provisionRatePercent / 100)}.
     * The rate from {@link #calculateProvisionRate(LoanAccount)} is a percentage (e.g. 5.00 = 5%),
     * matching {@link com.openfinova.banking.loan.entity.LoanProvision#getProvisionRate()}.
     *
     * @param loan the loan account
     * @return the calculated provision amount
     */
    private BigDecimal calculateProvisionAmount(LoanAccount loan) {
        return LoanProvisionCalculation.provisionAmount(loan.getOutstandingPrincipal(), calculateProvisionRate(loan));
    }

    /**
     * Calculates the provision rate based on loan delinquency status.
     *
     * Uses a tiered approach aligned with regulatory requirements:
     * - Current: 1% (minimal risk)
     * - 1-30 days: 5% (early warning)
     * - 31-60 days: 25% (increased risk)
     * - 61-90 days: 50% (significant risk)
     * - 90+ days: 100% (full provision)
     *
     * @param loan the loan account
     * @return the provision rate as a percentage
     */
    private BigDecimal calculateProvisionRate(LoanAccount loan) {
        Integer daysPastDueObj = loan.getDaysPastDue();
        int daysPastDue = (daysPastDueObj != null) ? daysPastDueObj : 0;
        return LoanProvisionCalculation.ratePercentForDaysPastDue(daysPastDue);
    }

    /**
     * Determines the IFRS 9 provision stage based on loan delinquency.
     *
     * IFRS 9 requires loans to be classified into three stages:
     * - Stage 1: Performing (current, no significant increase in credit risk)
     * - Stage 2: Underperforming (1-30 days past due, increased credit risk)
     * - Stage 3: Non-performing (30+ days past due, credit-impaired)
     *
     * @param loan the loan account
     * @return the provision stage classification
     */
    private ProvisionStage determineProvisionStage(LoanAccount loan) {
        Integer daysPastDueObj = loan.getDaysPastDue();
        int daysPastDue = (daysPastDueObj != null) ? daysPastDueObj : 0;

        if (daysPastDue == 0)
            return ProvisionStage.STAGE_1_PERFORMING;
        if (daysPastDue <= 30)
            return ProvisionStage.STAGE_2_UNDERPERFORMING;
        return ProvisionStage.STAGE_3_NON_PERFORMING;
    }

    /**
     * Calculates Portfolio at Risk (PAR) for a given days threshold.
     *
     * PAR is the sum of outstanding principal for all loans that are
     * past due by at least the specified number of days.
     *
     * @param loans the list of loans to analyze
     * @param daysThreshold the minimum days past due to include
     * @return the total outstanding principal of loans meeting the threshold
     */
    private BigDecimal calculatePAR(List<LoanAccount> loans, int daysThreshold) {
        return loans.stream().filter(loan -> loan.getDaysPastDue() != null && loan.getDaysPastDue() >= daysThreshold)
                .map(LoanAccount::getOutstandingPrincipal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
