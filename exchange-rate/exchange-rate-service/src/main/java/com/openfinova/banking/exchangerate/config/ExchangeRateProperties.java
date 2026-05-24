package com.openfinova.banking.exchangerate.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the exchange-rate sync subsystem.
 *
 * <p>Defaults target the European Central Bank reference rates feed:
 * <a href="https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml">eurofxref-daily.xml</a>
 * which publishes EUR-base mid rates each business day at ~16:00 CET.
 *
 * <p>The currency list is the full ECB daily reference set. Trim it via
 * {@code app.exchange-rate.managed-currencies} if you don't need all of them.
 */
@ConfigurationProperties(prefix = "app.exchange-rate")
public class ExchangeRateProperties {

    /** Provider identifier. Currently only {@code ecb} is implemented. */
    private String provider = "ecb";

    /**
     * When an exact-date rate lookup misses, the service falls back to the most recent
     * available rate within this many days before the requested date. Set to 0 to disable
     * the fallback (legacy behaviour: throw on miss).
     */
    private int maxStalenessDays = 7;

    /**
     * Currencies the scheduler keeps fresh, expressed as the non-base side of each pair.
     * Storage is always {@code baseCurrency -> code} so cross-rates work for any pair.
     */
    private List<String> managedCurrencies = new ArrayList<>(
            List.of(
                    "USD",
                    "JPY",
                    "BGN",
                    "CZK",
                    "DKK",
                    "GBP",
                    "HUF",
                    "PLN",
                    "RON",
                    "SEK",
                    "CHF",
                    "ISK",
                    "NOK",
                    "TRY",
                    "AUD",
                    "BRL",
                    "CAD",
                    "CNY",
                    "HKD",
                    "IDR",
                    "ILS",
                    "INR",
                    "KRW",
                    "MXN",
                    "MYR",
                    "NZD",
                    "PHP",
                    "SGD",
                    "THB",
                    "ZAR"));

    private Sync sync = new Sync();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getMaxStalenessDays() {
        return maxStalenessDays;
    }

    public void setMaxStalenessDays(int maxStalenessDays) {
        this.maxStalenessDays = maxStalenessDays;
    }

    public List<String> getManagedCurrencies() {
        return managedCurrencies;
    }

    public void setManagedCurrencies(List<String> managedCurrencies) {
        this.managedCurrencies = managedCurrencies;
    }

    public Sync getSync() {
        return sync;
    }

    public void setSync(Sync sync) {
        this.sync = sync;
    }

    /** Scheduler configuration. */
    public static class Sync {

        /** When false, neither the scheduled job nor the manual sync endpoint will hit the provider. */
        private boolean enabled = true;

        /**
         * Spring cron expression for the daily sync. Default fires at 16:30 in {@link #zone},
         * just after the ECB publishes its daily reference rates.
         */
        private String cron = "0 30 16 * * MON-FRI";

        /** Time zone the cron expression is interpreted in. */
        private String zone = "Europe/Brussels";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }
    }
}
