package com.openfinova.banking.config;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Monolith-wide Caffeine {@link CacheManager}. Domain modules use {@link org.springframework.cache.annotation.Cacheable};
 * runnable wiring lives in banking-app only.
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

    /**
     * Default policy: hourly expiry and bounded entries; explicit {@link org.springframework.cache.annotation.CacheEvict}
     * overrides TTL as needed per feature.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                Arrays.copyOf(BankingCacheNames.ALL, BankingCacheNames.ALL.length));
        manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(500).recordStats());
        return manager;
    }
}
