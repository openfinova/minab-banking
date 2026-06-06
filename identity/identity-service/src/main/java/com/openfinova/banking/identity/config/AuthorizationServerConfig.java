package com.openfinova.banking.identity.config;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
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
import com.openfinova.banking.identity.security.PromptLoginReauthenticationFilter;
import com.openfinova.banking.identity.service.MfaService;
import com.openfinova.banking.identity.service.SecurityAuditService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthorizationServerConfig.class);

    /** Custom OAuth2 scopes registered for dev clients (see {@link #registeredClientRepository}). */
    private static final String SCOPE_OFFLINE_ACCESS = "offline_access";
    private static final String SCOPE_BANKING_STAFF = "banking.staff";
    private static final String SCOPE_BANKING_CUSTOMER = "banking.customer";

    @Bean
    public RequestCache requestCache() {
        return new HttpSessionRequestCache();
    }

    @Bean
    public MfaChallengeFilter mfaChallengeFilter(MfaService mfaService, SecurityAuditService auditService,
            UserRepository userRepository, RequestCache requestCache) {
        return new MfaChallengeFilter(mfaService, auditService, userRepository, requestCache);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http, MfaChallengeFilter mfaChallengeFilter,
            RequestCache requestCache) throws Exception {
        LoginUrlAuthenticationEntryPoint loginEntryPoint = new LoginUrlAuthenticationEntryPoint("/login");

        http.cors(Customizer.withDefaults());
        // OAuth2 protocol endpoints include server-to-server POSTs (/oauth2/token, introspection, revoke)
        // that do not carry browser CSRF tokens. Enforce client auth instead of CSRF for this chain.
        http.csrf(csrf -> csrf.disable());
        http.requestCache(cache -> cache.requestCache(requestCache));
        http.oauth2AuthorizationServer(authServer -> {
            http.securityMatcher(authServer.getEndpointsMatcher());
            authServer.authorizationServerMetadataEndpoint(
                    metadata -> metadata.authorizationServerMetadataCustomizer(
                            customizer -> customizer.scope(SCOPE_OFFLINE_ACCESS).scope(SCOPE_BANKING_STAFF)
                                    .scope(SCOPE_BANKING_CUSTOMER)));
            authServer.oidc(oidc -> {
                oidc.providerConfigurationEndpoint(
                        provider -> provider.providerConfigurationCustomizer(
                                builder -> builder.scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE)
                                        .scope(OidcScopes.EMAIL).scope(SCOPE_OFFLINE_ACCESS).scope(SCOPE_BANKING_STAFF)
                                        .scope(SCOPE_BANKING_CUSTOMER)));
                oidc.userInfoEndpoint(Customizer.withDefaults());
                oidc.logoutEndpoint(
                        logout -> logout.errorResponseHandler(AuthorizationServerConfig::handleOidcLogoutFailure));
            });
        }).authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).exceptionHandling(
                ex -> ex.defaultAuthenticationEntryPointFor(
                        loginEntryPoint,
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
        // Runs before MFA so prompt=login forces a fresh login/MFA even when an SSO session exists.
        http.addFilterBefore(new PromptLoginReauthenticationFilter(), AuthorizationFilter.class);
        http.addFilterBefore(mfaChallengeFilter, AuthorizationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain loginFormFilterChain(HttpSecurity http, LoginEventHandlers loginEventHandlers,
            LoginRateLimitFilter loginRateLimitFilter, MfaChallengeFilter mfaChallengeFilter, RequestCache requestCache)
            throws Exception {
        http.cors(Customizer.withDefaults());
        http.requestCache(cache -> cache.requestCache(requestCache));
        http.authorizeHttpRequests(
                auth -> auth
                        // MFA challenge pages shown between password auth and token issuance
                        .requestMatchers("/mfa/challenge", "/mfa/verify").permitAll()
                        // Static assets for the server-rendered login portal
                        .requestMatchers("/css/**", "/js/**").permitAll()
                        // Form login, logout, and post-logout confirmation pages
                        .requestMatchers(
                                "/login",
                                "/login/**",
                                "/logout",
                                "/logout/**",
                                "/logged-out",
                                "/logged-out/**")
                        .permitAll().anyRequest().authenticated())
                .formLogin(
                        form -> form.loginPage("/login").successHandler(loginEventHandlers.successHandler())
                                .failureHandler(loginEventHandlers.failureHandler()))
                .logout(
                        logout -> logout.logoutRequestMatcher(
                                // GET enables SPAs to clear the IdP session when no id_token is available
                                // (Spring's /connect/logout requires id_token_hint). Prefer POST from HTML forms.
                                new OrRequestMatcher(
                                        PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/logout"),
                                        PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/logout")))
                                .logoutSuccessUrl("/logged-out") // redirect here after logout
                                .permitAll())
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(mfaChallengeFilter, AuthorizationFilter.class)
                // Narrow matcher avoids clashing with the resource-server chain (Order 3).
                .securityMatcher(
                        RequestMatchers.anyOf(
                                PathPatternRequestMatcher.pathPattern("/login"),
                                PathPatternRequestMatcher.pathPattern("/login/**"),
                                PathPatternRequestMatcher.pathPattern("/logout"),
                                PathPatternRequestMatcher.pathPattern("/logout/**"),
                                PathPatternRequestMatcher.pathPattern("/logged-out"),
                                PathPatternRequestMatcher.pathPattern("/logged-out/**"),
                                PathPatternRequestMatcher.pathPattern("/mfa/challenge"),
                                PathPatternRequestMatcher.pathPattern("/mfa/verify"),
                                PathPatternRequestMatcher.pathPattern("/css/**"),
                                PathPatternRequestMatcher.pathPattern("/js/**")));
        return http.build();
    }

    /**
     * In-memory OAuth2 clients for dev/test.
     *
     * <ul>
     * <li>{@code staff-app} — Swagger UI on {@code localhost:8080} only (longer TTL for API exploration).</li>
     * <li>{@code staff-portal} — staff Next.js BFF ({@code localhost:3000}); confidential, no refresh grant.</li>
     * <li>{@code customer-portal} — customer Next.js BFF ({@code localhost:3001}); confidential, refresh + rotation.</li>
     * </ul>
     *
     * Future {@code mobile-app} is documented in {@code docs/security/oauth-session-policy.md} but not registered here.
     *
     * Replace with a DB-backed {@link RegisteredClientRepository} for production.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder,
            OAuth2TokenPolicyProperties tokenPolicy) {
        ClientSettings pkceRequired = ClientSettings.builder().requireAuthorizationConsent(false).requireProofKey(true)
                .build();

        RegisteredClient staffApp = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(OAuth2TokenPolicyProperties.CLIENT_STAFF_APP)
                .clientSecret(passwordEncoder.encode("staff-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:8080/login/oauth2/code/staff-app")
                .redirectUri("http://localhost:8080/swagger-ui/oauth2-redirect.html")
                .postLogoutRedirectUri("http://localhost:8080/").scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL).scope(SCOPE_BANKING_STAFF).clientSettings(pkceRequired)
                .tokenSettings(tokenPolicy.toTokenSettings(OAuth2TokenPolicyProperties.CLIENT_STAFF_APP)).build();

        RegisteredClient staffPortal = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(OAuth2TokenPolicyProperties.CLIENT_STAFF_PORTAL)
                .clientSecret(passwordEncoder.encode("staff-portal-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:3000/api/auth/callback")
                .redirectUri("http://127.0.0.1:3000/api/auth/callback")
                .postLogoutRedirectUri("http://localhost:3000/login").postLogoutRedirectUri("http://localhost:3000/")
                .postLogoutRedirectUri("http://127.0.0.1:3000/login").scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL).scope(SCOPE_BANKING_STAFF).clientSettings(pkceRequired)
                .tokenSettings(tokenPolicy.toTokenSettings(OAuth2TokenPolicyProperties.CLIENT_STAFF_PORTAL)).build();

        RegisteredClient customerPortal = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(OAuth2TokenPolicyProperties.CLIENT_CUSTOMER_PORTAL)
                .clientSecret(passwordEncoder.encode("customer-portal-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:3001/api/auth/callback")
                .redirectUri("http://127.0.0.1:3001/api/auth/callback")
                .postLogoutRedirectUri("http://localhost:3001/login").postLogoutRedirectUri("http://localhost:3001/")
                .postLogoutRedirectUri("http://127.0.0.1:3001/login").scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL).scope(SCOPE_OFFLINE_ACCESS).scope(SCOPE_BANKING_CUSTOMER)
                .clientSettings(pkceRequired)
                .tokenSettings(tokenPolicy.toTokenSettings(OAuth2TokenPolicyProperties.CLIENT_CUSTOMER_PORTAL)).build();

        return new InMemoryRegisteredClientRepository(staffApp, staffPortal, customerPortal);
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
    public AuthorizationServerSettings authorizationServerSettings(
            @Value("${spring.security.oauth2.authorizationserver.issuer}") String issuer) {
        // Fixed issuer for all endpoints (including token calls from internal Docker hostnames).
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Logs any failure while processing {@code /connect/logout}; does not change whether logout
     * succeeds (that is determined by token/session validation). The default SAS handler passes a
     * long {@code OAuth2Error} string into {@link HttpServletResponse#sendError(int, String)}, which
     * some servlet containers handle poorly; we use the single-arg {@code sendError(400)} instead
     * and put detail in logs (including stack trace).
     */
    private static void handleOidcLogoutFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        String oauthSummary;
        if (exception instanceof OAuth2AuthenticationException oae && oae.getError() != null) {
            oauthSummary = oae.getError().toString();
        } else {
            oauthSummary = exception.getClass().getSimpleName();
        }
        String idTokenHint = request.getParameter("id_token_hint");
        LOG.warn(
                "OIDC RP-initiated logout failed: method={} uri={} summary={} client_id={} id_token_hint_length={} post_logout_redirect_uri={}",
                request.getMethod(),
                request.getRequestURI(),
                oauthSummary,
                request.getParameter("client_id"),
                idTokenHint != null ? idTokenHint.length() : 0,
                request.getParameter("post_logout_redirect_uri"),
                exception);
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }
}
