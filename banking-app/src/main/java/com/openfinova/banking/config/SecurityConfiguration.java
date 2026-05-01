package com.openfinova.banking.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.openfinova.banking.identity.api.principal.BankingPrincipal;

/**
 * Resource-server security configuration for the banking API.
 *
 * This filter chain ({@code Order(3)}) runs after the Authorization Server
 * chains defined in {@code AuthorizationServerConfig} ({@code Order(1)} and
 * {@code Order(2)}).
 *
 * Every banking API endpoint requires a valid JWT. Permissions are expressed as
 * authorities derived from the {@code permissions} JWT claim, enabling
 * fine-grained {@code @PreAuthorize("hasAuthority('loan:read')")} guards.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    @Order(3)
    public SecurityFilterChain bankingApiFilterChain(HttpSecurity http,
            PasswordManagementEnforcementFilter passwordManagementFilter,
            AuthenticationEntryPoint bankingBearerAuthenticationEntryPoint,
            AccessDeniedHandler bankingApiAccessDeniedHandler) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(
                        ex -> ex.authenticationEntryPoint(bankingBearerAuthenticationEntryPoint)
                                .accessDeniedHandler(bankingApiAccessDeniedHandler))
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers(
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                // Auth server endpoints are protected by the Order(1) chain; allow passthrough here
                                "/oauth2/**",
                                "/connect/**",
                                "/.well-known/**",
                                "/login",
                                "/error").permitAll().anyRequest().authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(bankingJwtAuthenticationConverter())))
                .addFilterAfter(passwordManagementFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Maps the {@code permissions} JWT claim to Spring Security
     * {@link org.springframework.security.core.GrantedAuthority} objects so
     * that {@code @PreAuthorize("hasAuthority('loan:read')")} expressions work.
     *
     * The {@code sub} field becomes the principal name; the full JWT is
     * available as the token in the
     * {@link org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken}
     * and can be accessed via
     * {@link BankingPrincipal#from(org.springframework.security.core.Authentication)}.
     */
    @Bean
    public JwtAuthenticationConverter bankingJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfiguration::extractPermissions);
        return converter;
    }

    private static Collection<GrantedAuthority> extractPermissions(Jwt jwt) {
        List<String> permissions = jwt.getClaimAsStringList(BankingPrincipal.CLAIM_PERMISSIONS);
        if (permissions == null) {
            return Collections.emptyList();
        }
        return permissions.stream().map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p)).toList();
    }
}
