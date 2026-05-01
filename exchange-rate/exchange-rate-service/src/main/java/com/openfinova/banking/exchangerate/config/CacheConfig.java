package com.openfinova.banking.exchangerate.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    /**
     * Caffeine-backed CacheManager for the "exchangeRates" cache.
     *
     * Exchange rates are relatively stable within a trading day, so entries are
     * expired 1 hour after they are written. The cache is capped at 500 entries
     * to bound memory use (there are a finite number of supported currency pairs
     * × rate types × dates held in memory at any time).
     *
     * @CacheEvict calls in ExchangeRateServiceImpl remain effective because
     * Caffeine honours explicit invalidation regardless of the TTL.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("exchangeRates");
        cacheManager
                .setCaffeine(Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(500).recordStats());
        return cacheManager;
    }
}
