package com.openfinova.banking.identity.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AccountLifecycleProperties.class)
public class AccountLifecycleConfiguration {
}
