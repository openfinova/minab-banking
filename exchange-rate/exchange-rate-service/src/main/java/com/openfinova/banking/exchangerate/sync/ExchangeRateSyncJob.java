package com.openfinova.banking.exchangerate.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Fires {@link ExchangeRateSyncService#sync()} on the cron defined by
 * {@code app.exchange-rate.sync.cron} (default: 16:30 Europe/Brussels MON-FRI, just after the ECB
 * publishes its daily reference rates).
 *
 * <p>Failures are logged and swallowed — the next scheduled run will retry, and read paths fall back to
 * the most recent available snapshot.
 */
@Component
public class ExchangeRateSyncJob {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateSyncJob.class);

    private final ExchangeRateSyncService syncService;

    public ExchangeRateSyncJob(ExchangeRateSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(cron = "${app.exchange-rate.sync.cron:0 30 16 * * MON-FRI}", zone = "${app.exchange-rate.sync.zone:Europe/Brussels}")
    public void scheduledSync() {
        try {
            syncService.sync();
        } catch (Exception e) {
            log.error("Scheduled exchange-rate sync failed: {}", e.getMessage(), e);
        }
    }
}
