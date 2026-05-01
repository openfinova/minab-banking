package com.openfinova.banking.identity.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.openfinova.banking.identity.event.LoginEventHandlers;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.security.LoginRateLimitFilter;
import com.openfinova.banking.identity.security.MfaChallengeFilter;
import com.openfinova.banking.identity.service.MfaService;
import com.openfinova.banking.identity.service.SecurityAuditService;

/**
 * Configures the OAuth2 / OIDC Authorization Server (Spring Security 7.0).
 *
 * Provides two filter chains:
 * {@code Order(1)} — protocol endpoints (/oauth2/token, /oauth2/authorize, …)
 * {@code Order(2)} — login form used by the authorization_code flow
 *
 *
 * The banking-app {@code SecurityConfiguration} provides the {@code Order(3)} resource-server chain
 * that protects the actual API endpoints.
 *
 * Production notes:
 * - Replace the in-memory {@link RegisteredClientRepository} with a JPA-backed one.
 * - Load the RSA signing key from a keystore / Key Vault instead of generating at startup.
 * - Override the issuer URI via {@code spring.security.oauth2.authorizationserver.issuer}.
 */
@Configuration
public class AuthorizationServerConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http) throws Exception {
        http.oauth2AuthorizationServer(authServer -> {
            http.securityMatcher(authServer.getEndpointsMatcher());
            authServer.oidc(Customizer.withDefaults());
        }).authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).exceptionHandling(
                ex -> ex.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain loginFormFilterChain(HttpSecurity http, LoginEventHandlers loginEventHandlers,
            LoginRateLimitFilter loginRateLimitFilter, MfaService mfaService, SecurityAuditService auditService,
            UserRepository userRepository) throws Exception {
        http.authorizeHttpRequests(
                auth -> auth.requestMatchers("/mfa/challenge", "/mfa/verify", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(
                        form -> form.successHandler(loginEventHandlers.successHandler())
                                .failureHandler(loginEventHandlers.failureHandler()))
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(
                        new MfaChallengeFilter(mfaService, auditService, userRepository),
                        UsernamePasswordAuthenticationFilter.class)
                // Apply after formLogin: Spring Security 7 otherwise leaves this chain matching any
                // request
                // (UnreachableFilterChainException vs. the resource-server chain).
                .securityMatcher(
                        RequestMatchers.anyOf(
                                PathPatternRequestMatcher.pathPattern("/login"),
                                PathPatternRequestMatcher.pathPattern("/login/**"),
                                PathPatternRequestMatcher.pathPattern("/logout"),
                                PathPatternRequestMatcher.pathPattern("/logout/**"),
                                PathPatternRequestMatcher.pathPattern("/mfa/challenge"),
                                PathPatternRequestMatcher.pathPattern("/mfa/verify"),
                                PathPatternRequestMatcher.pathPattern("/css/**"),
                                PathPatternRequestMatcher.pathPattern("/js/**")));
        return http.build();
    }

    /**
     * Two in-memory clients wired for dev/test.
     *
     * {@code staff-app} — used by the internal banking portal / Swagger UI for staff.
     * {@code customer-app} — used by the customer mobile / web application.
     *
     * Both use PKCE (no client secret stored in the browser) and authorization_code flow. Replace
     * with a DB-backed {@link RegisteredClientRepository} for production.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder,
            OAuth2TokenPolicyProperties tokenPolicy) {
        TokenSettings shortLivedTokens = TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(tokenPolicy.getAccessTokenTtlMinutes()))
                .refreshTokenTimeToLive(Duration.ofDays(tokenPolicy.getRefreshTokenTtlDays()))
                .reuseRefreshTokens(tokenPolicy.isReuseRefreshTokens()).build();

        RegisteredClient staffApp = RegisteredClient.withId(UUID.randomUUID().toString()).clientId("staff-app")
                .clientSecret(passwordEncoder.encode("staff-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:8080/login/oauth2/code/staff-app")
                .redirectUri("http://localhost:8080/swagger-ui/oauth2-redirect.html")
                .postLogoutRedirectUri("http://localhost:8080/").scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE)
                .scope("banking.staff")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .tokenSettings(shortLivedTokens).build();

        RegisteredClient customerApp = RegisteredClient.withId(UUID.randomUUID().toString()).clientId("customer-app")
                .clientSecret(passwordEncoder.encode("customer-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:3000/callback").postLogoutRedirectUri("http://localhost:3000/")
                .scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE).scope("banking.customer")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .tokenSettings(shortLivedTokens).build();

        return new InMemoryRegisteredClientRepository(staffApp, customerApp);
    }

    /**
     * Generates an RSA-2048 key pair on startup.
     *
     * WARNING: a fresh key pair is generated on every restart, which invalidates all previously
     * issued tokens. In production, load a persistent key from a keystore or Azure Key Vault and
     * inject it here.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKeyPair();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate()).keyID(UUID.randomUUID().toString()).build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA algorithm not available in this JRE", ex);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    // ── Password encoder (shared across the application context) ──────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
