package com.openfinova.banking.exchangerate.sync;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.lib.model.SupportedCurrency;
import com.openfinova.banking.exchangerate.api.entity.RateType;
import com.openfinova.banking.exchangerate.config.ExchangeRateProperties;
import com.openfinova.banking.exchangerate.entity.ExchangeRate;
import com.openfinova.banking.exchangerate.provider.ExchangeRateProvider;
import com.openfinova.banking.exchangerate.provider.ExchangeRateProvider.ProviderRates;
import com.openfinova.banking.exchangerate.provider.ExchangeRateProviderException;
import com.openfinova.banking.exchangerate.repository.ExchangeRateRepository;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Synchronizes daily exchange rates from the configured {@link ExchangeRateProvider} into the
 * {@code exchange_rates} table.
 *
 * <p>Semantics:
 * <ul>
 *   <li>Each successful sync inserts a new {@link ExchangeRate} row per managed currency for today's
 *       date with {@link RateType#SPOT}. Existing rows for today are left untouched (the daily snapshot
 *       is immutable; corrections go through the admin {@code PUT /rates/{id}} endpoint).</li>
 *   <li>If the provider fails, no rows are written. The read-side staleness fallback in
 *       {@code ExchangeRateServiceImpl} keeps lookups working against the most recent prior snapshot.</li>
 *   <li>{@code bidRate}/{@code askRate}/{@code FXSpread} are deliberately untouched. Bid/ask are derived
 *       on the fly from mid + spread configuration.</li>
 * </ul>
 */
@Service
public class ExchangeRateSyncService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateSyncService.class);
    private static final String SYSTEM_USER = "system:exchange-rate-sync";

    private final ExchangeRateRepository repository;
    private final ExchangeRateProvider provider;
    private final ExchangeRateProperties properties;
    private final DateTimeService dateTimeService;

    @Value("${app.base-currency:EUR}")
    private String baseCurrency;

    public ExchangeRateSyncService(ExchangeRateRepository repository, ExchangeRateProvider provider,
            ExchangeRateProperties properties, DateTimeService dateTimeService) {
        this.repository = repository;
        this.provider = provider;
        this.properties = properties;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Runs one sync against the configured provider. Idempotent for a given business day —
     * re-running within the same day inserts nothing if rows already exist.
     *
     * @return summary of inserted / skipped / unsupported currencies for logging and UI feedback.
     */
    @Transactional
    @CacheEvict(value = "exchangeRates", allEntries = true)
    public SyncResult sync() {
        if (!properties.getSync().isEnabled()) {
            log.info("Exchange-rate sync is disabled (app.exchange-rate.sync.enabled=false); skipping.");
            return SyncResult.disabled();
        }

        Set<String> requested = resolveTargets();
        if (requested.isEmpty()) {
            log.warn("No managed currencies configured; nothing to sync.");
            return SyncResult.empty();
        }

        LocalDate today = dateTimeService.today();

        ProviderRates response;
        try {
            response = provider.fetchLatestRates(baseCurrency, requested);
        } catch (ExchangeRateProviderException e) {
            log.warn(
                    "Provider [{}] sync failed: {}. Existing rates remain via stale-read fallback.",
                    provider.getId(),
                    e.getMessage());
            throw e;
        }

        Map<String, BigDecimal> rates = response.rates();
        List<String> inserted = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> unsupported = new ArrayList<>();

        for (String currency : requested) {
            BigDecimal rate = rates.get(currency);
            if (rate == null) {
                unsupported.add(currency);
                continue;
            }

            boolean alreadyExists = repository.existsBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(
                    baseCurrency,
                    currency,
                    today,
                    RateType.SPOT);
            if (alreadyExists) {
                skipped.add(currency);
                continue;
            }

            ExchangeRate entity = new ExchangeRate(baseCurrency, currency, rate, today, RateType.SPOT);
            entity.setCreatedBy(SYSTEM_USER);
            repository.save(entity);
            inserted.add(currency);
        }

        log.info(
                "Provider [{}] sync complete for {}: inserted={}, skippedAlreadyPresent={}, unsupportedByProvider={}",
                provider.getId(),
                today,
                inserted.size(),
                skipped.size(),
                unsupported.size());
        if (!unsupported.isEmpty()) {
            log.debug("Provider [{}] did not return rates for: {}", provider.getId(), unsupported);
        }

        return new SyncResult(today, response.publicationDate(), provider.getId(), inserted, skipped, unsupported);
    }

    /** Base currency these sync operations are quoted against (e.g. EUR). */
    public String getBaseCurrency() {
        return baseCurrency;
    }

    /**
     * Returns the set of target currency codes the scheduler should manage. Filters out:
     * the base currency itself, unsupported codes, and duplicates. Package-private so the
     * read-only "managed rates" view can reuse the same filtering rules.
     */
    Set<String> resolveTargets() {
        Set<String> result = new HashSet<>();
        for (String raw : properties.getManagedCurrencies()) {
            if (raw == null)
                continue;
            String code = raw.trim().toUpperCase(Locale.ROOT);
            if (code.isEmpty() || code.equals(baseCurrency))
                continue;
            if (!SupportedCurrency.isValid(code)) {
                log.warn("Ignoring unsupported currency code in managed list: {}", code);
                continue;
            }
            result.add(code);
        }
        return result;
    }

    /** Outcome of a single sync invocation. */
    public record SyncResult(LocalDate rateDate, LocalDate providerPublicationDate, String providerId,
            List<String> inserted, List<String> skippedAlreadyPresent, List<String> unsupportedByProvider) {

        public static SyncResult disabled() {
            return new SyncResult(null, null, "disabled", List.of(), List.of(), List.of());
        }

        public static SyncResult empty() {
            return new SyncResult(null, null, "none", List.of(), List.of(), List.of());
        }
    }
}
