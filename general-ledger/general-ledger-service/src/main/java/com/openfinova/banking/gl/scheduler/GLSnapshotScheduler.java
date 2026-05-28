package com.openfinova.banking.gl.scheduler;

import com.openfinova.banking.gl.service.GLSnapshotService;
import com.openfinova.banking.setup.api.DateTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduled tasks for GL snapshot generation and maintenance.
 *
 * This scheduler handles the snapshot lifecycle:
 * - Daily balance snapshots (end of business day)
 * - Weekly aggregations (end of week)
 * - Monthly snapshots (end of month)
 * - Missing snapshot detection and recovery
 * - Old snapshot cleanup (monthly)
 * - Snapshot archival (for long-term retention)
 *
 * All scheduled tasks can be disabled via configuration properties.
 * Manual execution is still available through REST endpoints.
 *
 * For GL operational tasks (EOD processing, reconciliation, validations),
 * see {@link GLOperationsScheduler}.
 *
 * Schedule Overview:
 * 01:00 - Daily snapshots
 * 01:00 1st - Snapshot archive (monthly)
 * 01:30 - Missing snapshots recovery
 * 02:00 Mon - Weekly snapshots
 * 02:00 1st - Snapshot cleanup (monthly)
 * 03:00 1st - Monthly snapshots
 */
@Component
@ConditionalOnProperty(prefix = "gl.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GLSnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(GLSnapshotScheduler.class);

    private final GLSnapshotService snapshotService;
    private final DateTimeService dateTimeService;

    public GLSnapshotScheduler(GLSnapshotService snapshotService, DateTimeService dateTimeService) {
        this.snapshotService = snapshotService;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Generates daily balance snapshots for all accounts.
     * Runs at 1:00 AM every day.
     *
     * Cron: 0 0 1 * * * (second, minute, hour, day, month, day-of-week)
     */
    @Scheduled(cron = "${gl.scheduler.daily-snapshots.cron:0 0 1 * * *}")
    @ConditionalOnProperty(prefix = "gl.scheduler.daily-snapshots", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void generateDailySnapshots() {
        log.info("Starting scheduled daily snapshot generation");

        try {
            snapshotService.generateDailySnapshots();
            log.info("Successfully completed scheduled daily snapshot generation");
        } catch (Exception e) {
            log.error("Error during scheduled daily snapshot generation", e);
            // Don't rethrow - let other scheduled tasks continue
        }
    }

    /**
     * Generates weekly balance aggregations.
     * Runs at 2:00 AM every Monday.
     *
     * Cron: 0 0 2 * * MON
     */
    @Scheduled(cron = "${gl.scheduler.weekly-snapshots.cron:0 0 2 * * MON}")
    @ConditionalOnProperty(prefix = "gl.scheduler.weekly-snapshots", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void generateWeeklySnapshots() {
        log.info("Starting scheduled weekly snapshot generation");

        try {
            snapshotService.generateWeeklySnapshots();
            log.info("Successfully completed scheduled weekly snapshot generation");
        } catch (Exception e) {
            log.error("Error during scheduled weekly snapshot generation", e);
        }
    }

    /**
     * Generates monthly balance aggregations.
     * Runs at 3:00 AM on the 1st day of every month.
     *
     * Cron: 0 0 3 1 * *
     */
    @Scheduled(cron = "${gl.scheduler.monthly-snapshots.cron:0 0 3 1 * *}")
    @ConditionalOnProperty(prefix = "gl.scheduler.monthly-snapshots", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void generateMonthlySnapshots() {
        log.info("Starting scheduled monthly snapshot generation");

        try {
            snapshotService.generateMonthlySnapshots();
            log.info("Successfully completed scheduled monthly snapshot generation");
        } catch (Exception e) {
            log.error("Error during scheduled monthly snapshot generation", e);
        }
    }

    /**
     * Generates missing snapshots for the previous day.
     * Runs at 1:30 AM every day (after daily snapshots).
     *
     * This catches any accounts that were missed during the main snapshot run.
     */
    @Scheduled(cron = "${gl.scheduler.missing-snapshots.cron:0 30 1 * * *}")
    @ConditionalOnProperty(prefix = "gl.scheduler.missing-snapshots", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void generateMissingSnapshots() {
        log.info("Starting scheduled missing snapshot generation");

        try {
            LocalDate yesterday = dateTimeService.today().minusDays(1);
            int count = snapshotService.generateMissingSnapshots(yesterday);
            log.info("Generated {} missing snapshots for {}", count, yesterday);
        } catch (Exception e) {
            log.error("Error during scheduled missing snapshot generation", e);
        }
    }

    /**
     * Cleans up old snapshots beyond retention period.
     * Runs at 2:00 AM on the 1st day of every month.
     *
     * Default retention: 365 days (configurable)
     */
    @Scheduled(cron = "${gl.scheduler.snapshot-cleanup.cron:0 0 2 1 * *}")
    @ConditionalOnProperty(prefix = "gl.scheduler.snapshot-cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void cleanupOldSnapshots() {
        log.info("Starting scheduled snapshot cleanup");

        try {
            int retentionDays = 365; // TODO: Make configurable
            int count = snapshotService.cleanupOldSnapshots(retentionDays);
            log.info("Cleaned up {} old snapshots (retention: {} days)", count, retentionDays);
        } catch (Exception e) {
            log.error("Error during scheduled snapshot cleanup", e);
        }
    }

    /**
     * Archives old snapshots before deletion.
     * Runs at 1:00 AM on the 1st day of every month (before cleanup).
     *
     * Archives snapshots older than 1 year to separate storage.
     */
    @Scheduled(cron = "${gl.scheduler.snapshot-archive.cron:0 0 1 1 * *}")
    @ConditionalOnProperty(prefix = "gl.scheduler.snapshot-archive", name = "enabled", havingValue = "true", matchIfMissing = true)
    public void archiveOldSnapshots() {
        log.info("Starting scheduled snapshot archival");

        try {
            LocalDate archiveBeforeDate = dateTimeService.today().minusYears(1);
            int count = snapshotService.archiveOldSnapshots(archiveBeforeDate);
            log.info("Archived {} snapshots before {}", count, archiveBeforeDate);
        } catch (Exception e) {
            log.error("Error during scheduled snapshot archival", e);
        }
    }

}
