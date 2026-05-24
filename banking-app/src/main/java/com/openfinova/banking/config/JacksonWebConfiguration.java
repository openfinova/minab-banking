package com.openfinova.banking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import tools.jackson.databind.json.JsonMapper;

/**
 * Primary Jackson mapper for HTTP JSON serialization. Identity module registers a separate
 * {@code oauth2AuthorizationJsonMapper} (Spring Security JDBC-only); that bean must not be the sole
 * application {@link JsonMapper} or REST bodies pick up polymorphic defaults and reject plain JSON.
 */
@Configuration
public class JacksonWebConfiguration {

    @Bean
    @Primary
    public JsonMapper jsonMapper() {
        return JsonMapper.builder().findAndAddModules().build();
    }
}
