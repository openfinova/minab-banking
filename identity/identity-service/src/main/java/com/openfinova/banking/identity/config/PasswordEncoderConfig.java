package com.openfinova.banking.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides the application {@link PasswordEncoder}.
 *
 * Keycloak is the authentication authority and verifies login credentials, but the banking platform
 * still encodes passwords for admin-set/self-service flows that are mirrored into Keycloak. This
 * bean previously lived in the now-removed {@code AuthorizationServerConfig}.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
