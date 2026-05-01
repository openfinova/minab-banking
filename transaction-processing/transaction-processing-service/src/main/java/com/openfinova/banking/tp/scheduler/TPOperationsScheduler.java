package com.openfinova.banking.tp.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.openfinova.banking.tp.service.BalanceReservationService;
import com.openfinova.banking.tp.service.CompensationWorkflowService;
import com.openfinova.banking.tp.service.TransactionService;

/**
 * Scheduled tasks for Transaction Processing operational processes.
 *
 * Centralises all TP scheduling (consistent with GL's GLOperationsScheduler).
 * - Release expired balance reservations (free locked funds)
 * - Fail timed-out in-progress transactions
 * - Retry failed compensation workflows
 *
 * Release-expired and fail-timed-out are staggered (fail first, then release after 30s) to reduce
 * lock contention and double updates on the same reservation row. Release is idempotent for
 * already-released/expired reservations.
 * All tasks can be disabled via configuration.
 */
@Component
@ConditionalOnProperty(prefix = "tp.operations-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TPOperationsScheduler {

    private static final Logger log = LoggerFactory.getLogger(TPOperationsScheduler.class);

    @Value("${tp.operations-scheduler.release-expired-reservations.enabled:true}")
    private boolean releaseExpiredReservationsEnabled;

    @Value("${tp.operations-scheduler.fail-timed-out-transactions.enabled:true}")
    private boolean failTimedOutTransactionsEnabled;

    @Value("${tp.operations-scheduler.fail-timed-out-transactions.timeout-minutes:30}")
    private int transactionTimeoutMinutes;

    @Value("${tp.operations-scheduler.retry-compensation.enabled:true}")
    private boolean retryCompensationEnabled;

    private final BalanceReservationService balanceReservationService;
    private final TransactionService transactionService;
    private final CompensationWorkflowService compensationWorkflowService;

    public TPOperationsScheduler(BalanceReservationService balanceReservationService,
            TransactionService transactionService, CompensationWorkflowService compensationWorkflowService) {
        this.balanceReservationService = balanceReservationService;
        this.transactionService = transactionService;
        this.compensationWorkflowService = compensationWorkflowService;
    }

    /**
     * Marks all expired balance reservations (ACTIVE with expiresAt <= now) as EXPIRED.
     * Frees locked funds. Runs every minute with 30s initial delay so it runs after
     * failTimedOutTransactions, reducing contention on the same rows.
     */
    @Scheduled(initialDelayString = "${tp.operations-scheduler.release-expired-reservations.initial-delay-ms:30000}", fixedDelayString = "${tp.operations-scheduler.release-expired-reservations.fixed-delay-ms:60000}")
    public void releaseExpiredReservations() {
        if (!releaseExpiredReservationsEnabled) {
            log.debug(
                    "Release expired reservations is disabled via tp.operations-scheduler.release-expired-reservations.enabled");
            return;
        }
        try {
            int count = balanceReservationService.releaseAllExpiredReservations();
            if (count > 0) {
                log.info("Released {} expired reservation(s)", count);
            }
        } catch (Exception e) {
            log.error("Error during scheduled release of expired reservations", e);
        }
    }

    /**
     * Fails transactions that have been in progress longer than the configured timeout.
     * Releases their reservations and triggers compensation if applicable. Runs every minute;
     * runs first (no initial delay) so reservations are released via fail path before
     * releaseExpiredReservations runs, reducing double updates.
     */
    @Scheduled(initialDelayString = "${tp.operations-scheduler.fail-timed-out-transactions.initial-delay-ms:0}", fixedDelayString = "${tp.operations-scheduler.fail-timed-out-transactions.fixed-delay-ms:60000}")
    public void failTimedOutTransactions() {
        if (!failTimedOutTransactionsEnabled) {
            log.debug(
                    "Fail timed-out transactions is disabled via tp.operations-scheduler.fail-timed-out-transactions.enabled");
            return;
        }
        try {
            int count = transactionService.failTimedOutTransactions(transactionTimeoutMinutes);
            if (count > 0) {
                log.info("Failed {} timed-out transaction(s)", count);
            }
        } catch (Exception e) {
            log.error("Error during scheduled fail of timed-out transactions", e);
        }
    }

    /**
     * Retries compensation workflows that are ready for retry. Runs every minute.
     */
    @Scheduled(fixedDelayString = "${tp.operations-scheduler.retry-compensation.fixed-delay-ms:60000}")
    public void retryFailedCompensations() {
        if (!retryCompensationEnabled) {
            log.debug("Retry compensation is disabled via tp.operations-scheduler.retry-compensation.enabled");
            return;
        }
        try {
            compensationWorkflowService.retryFailedCompensations();
        } catch (Exception e) {
            log.error("Error during scheduled compensation retry", e);
        }
    }
}
