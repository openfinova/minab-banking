package com.openfinova.banking.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
            PasswordManagementEnforcementFilter passwordManagementFilter, StepUpAcrFilter stepUpAcrFilter,
            InternalApiTokenFilter internalApiTokenFilter,
            AuthenticationEntryPoint bankingBearerAuthenticationEntryPoint,
            AccessDeniedHandler bankingApiAccessDeniedHandler) throws Exception {
        http.cors(Customizer.withDefaults()).csrf(AbstractHttpConfigurer::disable).exceptionHandling(
                ex -> ex.authenticationEntryPoint(bankingBearerAuthenticationEntryPoint)
                        .accessDeniedHandler(bankingApiAccessDeniedHandler))
                .authorizeHttpRequests(
                        auth -> auth
                                // Health/metrics for orchestrators and load balancers — no customer data
                                .requestMatchers("/actuator/**").permitAll()
                                // OpenAPI/Swagger UI for local and CI API exploration
                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                                // Internal machine-to-machine endpoints — guarded by the shared-secret
                                // InternalApiTokenFilter (no end-user bearer token), not by JWT auth.
                                .requestMatchers("/internal/**").permitAll()
                                // Login/MFA/logout and OIDC protocol endpoints now live in Keycloak;
                                // only the generic error/favicon paths remain server-rendered here.
                                .requestMatchers("/favicon.ico", "/error").permitAll()
                                // TAN enrollment bootstrap: short-lived signed enrollment JWT is the credential
                                .requestMatchers(HttpMethod.GET, "/api/v1/tan/devices/attestation-nonce").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/v1/tan/devices").permitAll().anyRequest()
                                .authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(bankingJwtAuthenticationConverter())))
                .addFilterBefore(internalApiTokenFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(passwordManagementFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(stepUpAcrFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Resource-server JWT decoder for Keycloak-issued access tokens.
     *
     * JWKS is fetched in-network ({@code banking.oauth2.jwk-set-uri}) so the container never has to
     * reach the host IP, while the {@code iss} claim is validated against the browser/device-facing
     * LAN-IP realm URL ({@code banking.oauth2.issuer}) together with the default timestamp checks.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${banking.oauth2.jwk-set-uri}") String jwkSetUri,
            @Value("${banking.oauth2.issuer}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(validator);
        return decoder;
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

    /**
     * CORS for browser SPAs (e.g. management portal on {@code http://localhost:3000}) calling this API
     * and OAuth2 endpoints on {@code http://localhost:8080}.
     *
     * <p>
     * Without this, post-login {@code fetch} to {@code /oauth2/token} and authenticated API calls fail
     * in the browser with a generic network error ("Failed to fetch").
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(
                List.of(
                        "http://localhost:3000",
                        "http://127.0.0.1:3000",
                        "http://localhost:3001",
                        "http://127.0.0.1:3001"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("WWW-Authenticate"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
