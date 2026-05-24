package com.openfinova.banking.exchangerate.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ExchangeRateProperties.class)
public class ExchangeRateConfiguration {
}
