package com.openfinova.banking.setup.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Spring configuration for the setup module.
 *
 * Registers the platform {@link Clock} used by {@link com.openfinova.banking.setup.SystemDateTimeService}
 * and exposed again via {@link com.openfinova.banking.setup.api.DateTimeService#clock()}. Application
 * code should use {@link com.openfinova.banking.setup.api.DateTimeService} for business time; this bean
 * is the single injectable clock backing that service.
 *
 * Keeping the clock as a bean (rather than creating it inside {@code SystemDateTimeService}) allows
 * tests to replace it with {@link Clock#fixed} so schedulers and services see a stable "now" without
 * scattering {@code LocalDate.now()} calls. Components that must take a {@code Clock} directly (for example
 * OAuth2 deserialization rebuilding {@code BankingUserDetails}) should use {@code dateTimeService.clock()}
 * so they stay aligned with the same source.
 */
@Configuration
@PropertySource("classpath:setup-service-default.properties")
@EnableConfigurationProperties(BankConfigProperties.class)
public class SetupServiceConfig {

    /**
     * Platform-wide system clock. This is the only place that may call {@code Clock.systemDefaultZone()};
     * it is exempt from the general "no Clock.system*" rule.
     *
     * @return the JVM system clock in the default zone
     */
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
