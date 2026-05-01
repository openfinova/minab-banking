package com.openfinova.banking.setup.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:setup-service-default.properties")
@EnableConfigurationProperties(BankConfigProperties.class)
public class SetupServiceConfig {

    /**
     * Platform-wide clock aligned with {@link com.openfinova.banking.setup.api.DateTimeService#clock()}.
     */
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
