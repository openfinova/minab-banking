package com.openfinova.banking.identity.event;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.identity.api.exception.PasswordPolicyViolationException;
import com.openfinova.banking.identity.audit.AuditEventDetail;
import com.openfinova.banking.identity.config.LockoutProperties;
import com.openfinova.banking.identity.config.PasswordPolicyProperties;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.security.BankingUserDetails;
import com.openfinova.banking.identity.security.BankingUserDetailsService;
import com.openfinova.banking.identity.security.ClientIpResolver;
import com.openfinova.banking.identity.service.PasswordPolicyService;
import com.openfinova.banking.identity.service.SecurityAuditService;
import com.openfinova.banking.setup.api.DateTimeService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles login success and failure events:
 * <ul>
 *   <li>On success: resets failed-login counter, records last login time/IP, logs audit event.</li>
 *   <li>On failure: increments counter, auto-locks after threshold, logs audit event.</li>
 * </ul>
 *
 * Wired into the authorization server's login form chain in {@link AuthorizationServerConfig}.
 */
@Component
public class LoginEventHandlers {

    private static final Logger log = LoggerFactory.getLogger(LoginEventHandlers.class);

    private final UserRepository userRepository;
    private final SecurityAuditService auditService;
    private final LockoutProperties lockoutProps;
    private final PasswordPolicyProperties passwordPolicyProps;
    private final PasswordPolicyService passwordPolicyService;
    private final BankingUserDetailsService userDetailsService;
    private final DateTimeService dateTimeService;
    private final RequestCache requestCache;

    public LoginEventHandlers(UserRepository userRepository, SecurityAuditService auditService,
            LockoutProperties lockoutProps, PasswordPolicyProperties passwordPolicyProps,
            PasswordPolicyService passwordPolicyService, BankingUserDetailsService userDetailsService,
            DateTimeService dateTimeService, RequestCache requestCache) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.lockoutProps = lockoutProps;
        this.passwordPolicyProps = passwordPolicyProps;
        this.passwordPolicyService = passwordPolicyService;
        this.userDetailsService = userDetailsService;
        this.dateTimeService = dateTimeService;
        this.requestCache = requestCache;
    }

    public AuthenticationSuccessHandler successHandler() {
        SuccessHandler handler = new SuccessHandler();
        handler.setRequestCache(requestCache);
        // When /login is opened directly (no pending OAuth authorize), avoid redirecting to "/" (404).
        handler.setDefaultTargetUrl("/logged-out");
        return handler;
    }

    public AuthenticationFailureHandler failureHandler() {
        return new FailureHandler();
    }

    private class SuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

        @Override
        @Transactional
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                Authentication authentication) throws IOException, ServletException {

            String username = authentication.getName();
            String ip = ClientIpResolver.resolve(request);
            String ua = request.getHeader("User-Agent");

            userRepository.findByUsername(username).ifPresent(user -> {
                user.setFailedLoginAttempts(0);
                user.setFailedLoginLockedUntil(null);
                user.setLastLoginAt(dateTimeService.now());
                user.setLastLoginIp(ip);
                userRepository.save(user);
            });

            Authentication authForContext = applyLoginPasswordPolicy(request, authentication, username);

            auditService.record(
                    SecurityAuditEventType.LOGIN_SUCCESS,
                    null,
                    username,
                    ip,
                    ua,
                    null,
                    AuditEventDetail.loginSuccess());
            super.onAuthenticationSuccess(request, response, authForContext);
        }
    }

    private class FailureHandler implements AuthenticationFailureHandler {

        @Override
        @Transactional
        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                AuthenticationException exception) throws IOException, ServletException {

            String username = request.getParameter("username");
            String ip = ClientIpResolver.resolve(request);
            String ua = request.getHeader("User-Agent");
            UUID userId = null;

            if (username != null && !username.isBlank()) {
                Optional<BankingUser> optUser = userRepository.findByUsername(username);
                if (optUser.isPresent()) {
                    BankingUser user = optUser.get();
                    userId = user.getId();
                    int attempts = user.getFailedLoginAttempts() + 1;
                    user.setFailedLoginAttempts(attempts);

                    if (attempts >= lockoutProps.getMaxAttempts()) {
                        LocalDateTime lockUntil = dateTimeService.now()
                                .plusMinutes(lockoutProps.getLockoutDurationMinutes());
                        user.setFailedLoginLockedUntil(lockUntil);
                        auditService.record(
                                SecurityAuditEventType.ACCOUNT_LOCKED_AUTO,
                                user.getId(),
                                username,
                                ip,
                                ua,
                                "Auto-locked after " + attempts + " failed attempts until " + lockUntil,
                                AuditEventDetail.accountAutoLocked(attempts, lockUntil));
                        log.warn("User {} auto-locked after {} failed login attempts", userId, attempts);
                    }
                    userRepository.save(user);
                }
            }

            log.debug("Login failure for userId={}", userId, exception);
            auditService.record(
                    SecurityAuditEventType.LOGIN_FAILURE,
                    null,
                    username,
                    ip,
                    ua,
                    "Authentication failed",
                    AuditEventDetail.loginFailure(exception.getMessage()));

            response.sendRedirect("/login?error");
        }
    }

    /**
     * Re-validates submitted password against current complexity rules; on failure marks the account
     * for forced change and refreshes the security context so OAuth2 token issuance sees the update.
     */
    private Authentication applyLoginPasswordPolicy(HttpServletRequest request, Authentication authentication,
            String username) {
        if (!passwordPolicyProps.isRevalidateOnLogin()
                || !(authentication.getPrincipal() instanceof BankingUserDetails details)) {
            return authentication;
        }
        String rawPassword = request.getParameter("password");
        if (rawPassword == null || rawPassword.isBlank()) {
            return authentication;
        }
        try {
            passwordPolicyService.validate(rawPassword);
            return authentication;
        } catch (PasswordPolicyViolationException ignored) {
            userRepository.findById(details.getUserId()).ifPresent(user -> {
                user.setForcePasswordChange(true);
                userRepository.save(user);
            });
            UserDetails reloaded = userDetailsService.loadUserByUsername(username);
            Set<GrantedAuthority> mergedAuthorities = new LinkedHashSet<>(reloaded.getAuthorities());
            for (GrantedAuthority ga : authentication.getAuthorities()) {
                // Preserve FactorGrantedAuthority added by DaoAuthenticationProvider; SAS 7's
                // JwtGenerator.getAuthenticationTime reads it to populate the OIDC auth_time claim
                // and asserts non-null, so dropping it here breaks ID token issuance.
                if (ga instanceof FactorGrantedAuthority) {
                    mergedAuthorities.add(ga);
                }
            }
            UsernamePasswordAuthenticationToken refreshed = UsernamePasswordAuthenticationToken
                    .authenticated(reloaded, null, mergedAuthorities);
            refreshed.setDetails(authentication.getDetails());
            SecurityContextHolder.getContext().setAuthentication(refreshed);
            return refreshed;
        }
    }
}
