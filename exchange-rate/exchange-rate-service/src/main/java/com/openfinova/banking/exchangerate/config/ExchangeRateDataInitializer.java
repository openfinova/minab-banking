package com.openfinova.banking.exchangerate.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.openfinova.banking.exchangerate.repository.ExchangeRateRepository;
import com.openfinova.banking.exchangerate.sync.ExchangeRateSyncService;

/**
 * On boot, if no exchange rates exist for the configured base currency, run one sync against the
 * provider so the system starts with a usable snapshot. After the first successful seed, this is a no-op
 * — the {@code ExchangeRateSyncJob} keeps things fresh.
 *
 * <p>Provider failures during startup are logged but do not block the application from starting.
 */
@Component
public class ExchangeRateDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateDataInitializer.class);

    private final ExchangeRateRepository repository;
    private final ExchangeRateSyncService syncService;

    @Value("${app.base-currency:EUR}")
    private String baseCurrency;

    public ExchangeRateDataInitializer(ExchangeRateRepository repository, ExchangeRateSyncService syncService) {
        this.repository = repository;
        this.syncService = syncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean hasBaseRates = repository.existsBySourceCurrency(baseCurrency);
        if (hasBaseRates) {
            log.debug("Exchange-rate data already present for base {}; skipping seed.", baseCurrency);
            return;
        }

        log.info("No exchange rates found for base {}; performing initial sync.", baseCurrency);
        try {
            ExchangeRateSyncService.SyncResult result = syncService.sync();
            log.info(
                    "Initial exchange-rate sync seeded {} pairs for {} (provider {}).",
                    result.inserted().size(),
                    result.rateDate(),
                    result.providerId());
        } catch (Exception e) {
            log.warn(
                    "Initial exchange-rate sync failed: {}. Scheduler will retry; admins can also trigger /api/v1/exchange/sync manually.",
                    e.getMessage());
        }
    }
}
