package com.openfinova.banking.identity.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.openfinova.banking.identity.security.LoginIpRateLimiter;
import com.openfinova.banking.identity.security.LoginRateLimitFilter;

@Configuration
@EnableConfigurationProperties(LoginRateLimitProperties.class)
public class LoginRateLimitConfiguration {

    @Bean
    public LoginIpRateLimiter loginIpRateLimiter(LoginRateLimitProperties properties) {
        return new LoginIpRateLimiter(properties.getMaxAttemptsPerIpPerMinute());
    }

    @Bean
    public LoginRateLimitFilter loginRateLimitFilter(LoginIpRateLimiter limiter, LoginRateLimitProperties properties) {
        return new LoginRateLimitFilter(limiter, properties);
    }
}
