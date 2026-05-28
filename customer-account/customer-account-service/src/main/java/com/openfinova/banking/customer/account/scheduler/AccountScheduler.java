package com.openfinova.banking.customer.account.scheduler;

import com.openfinova.banking.customer.account.service.AccountBalanceService;
import com.openfinova.banking.customer.account.service.AccountHoldService;
import com.openfinova.banking.customer.account.service.AccountService;
import com.openfinova.banking.setup.api.DateTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduler for automated account operations.
 *
 * Scheduled Tasks:
 * 1. Interest Accrual - Daily at 2:00 AM
 * 2. Expired Holds Processing - Every hour
 * 3. Dormancy Detection - Monthly on 1st at 4:00 AM
 * 4. Balance View Refresh - Daily at 3:00 AM
 */
@Component
@ConditionalOnProperty(prefix = "account.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AccountScheduler {

    private static final Logger log = LoggerFactory.getLogger(AccountScheduler.class);

    private final AccountService accountService;
    private final AccountHoldService holdService;
    private final AccountBalanceService balanceService;
    private final DateTimeService dateTimeService;

    public AccountScheduler(AccountService accountService, AccountHoldService holdService,
            AccountBalanceService balanceService, DateTimeService dateTimeService) {
        this.accountService = accountService;
        this.holdService = holdService;
        this.balanceService = balanceService;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Processes interest accrual for all eligible accounts.
     * Runs daily at 2:00 AM.
     */
    @Scheduled(cron = "${account.scheduler.interest-accrual.cron:0 0 2 * * *}")
    @ConditionalOnProperty(prefix = "account.scheduler.interest-accrual", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void processInterestAccrual() {
        log.info("Starting scheduled interest accrual");
        try {
            LocalDate today = dateTimeService.today();
            int count = accountService.processInterestAccrual(today);
            log.info("Processed interest accrual for {} accounts", count);
        } catch (Exception e) {
            log.error("Error during scheduled interest accrual", e);
        }
    }

    /**
     * Processes expired holds automatically.
     * Runs every hour.
     */
    @Scheduled(cron = "${account.scheduler.expired-holds.cron:0 0 * * * *}")
    @ConditionalOnProperty(prefix = "account.scheduler.expired-holds", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void processExpiredHolds() {
        log.info("Starting scheduled expired holds processing");
        try {
            int count = holdService.processExpiredHolds();
            log.info("Processed {} expired holds", count);
        } catch (Exception e) {
            log.error("Error during scheduled expired holds processing", e);
        }
    }

    /**
     * Detects and marks dormant accounts.
     * Runs monthly on the 1st at 4:00 AM.
     */
    @Scheduled(cron = "${account.scheduler.dormancy-detection.cron:0 0 4 1 * *}")
    @ConditionalOnProperty(prefix = "account.scheduler.dormancy-detection", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void processDormancyDetection() {
        log.info("Starting scheduled dormancy detection");
        try {
            int inactivityMonths = 12; // Configurable via properties
            int count = accountService.processDormancyDetection(inactivityMonths);
            log.info("Marked {} accounts as dormant", count);
        } catch (Exception e) {
            log.error("Error during scheduled dormancy detection", e);
        }
    }

    /**
     * Refreshes balance views for all accounts.
     * Runs daily at 3:00 AM.
     */
    @Scheduled(cron = "${account.scheduler.balance-refresh.cron:0 0 3 * * *}")
    @ConditionalOnProperty(prefix = "account.scheduler.balance-refresh", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void refreshBalanceViews() {
        log.info("Starting scheduled balance view refresh");
        try {
            int count = balanceService.refreshAllBalanceViews();
            log.info("Refreshed {} balance views", count);
        } catch (Exception e) {
            log.error("Error during scheduled balance view refresh", e);
        }
    }
}
