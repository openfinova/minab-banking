package com.openfinova.banking.identity.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.filter.OncePerRequestFilter;

import com.openfinova.banking.identity.audit.AuditEventDetail;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.service.MfaService;
import com.openfinova.banking.identity.service.SecurityAuditService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Intercepts authenticated requests from users who have MFA enabled but
 * have not yet completed the TOTP challenge in the current session.
 *
 * Workflow:
 * - After form login succeeds, this filter checks if the user has MFA enabled.
 * - If yes and the session does not contain an MFA_VERIFIED flag,
 *   redirects to {@code /mfa/challenge}.
 * - The challenge page submits a code to {@code POST /mfa/verify}: a 6-digit TOTP
 *   or an 8-digit recovery code (same form field). On success, the handler sets the
 *   session flag and redirects to the saved request.
 * - Subsequent requests pass through because the session flag is present.
 *
 * Interactive login uses the OAuth2 authorization_code flow after password verification; that step
 * is served by {@code /oauth2/authorize} on the authorization-server chain, so MFA must run there —
 * successful form-login does not invoke filters registered after {@code UsernamePasswordAuthenticationFilter}.
 */
public class MfaChallengeFilter extends OncePerRequestFilter {

    public static final String MFA_VERIFIED_ATTR = "MFA_VERIFIED";
    private static final String MFA_CHALLENGE_URL = "/mfa/challenge";
    private static final String MFA_VERIFY_URL = "/mfa/verify";

    private final MfaService mfaService;
    private final SecurityAuditService auditService;
    private final UserRepository userRepository;
    private final RequestCache requestCache;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public MfaChallengeFilter(MfaService mfaService, SecurityAuditService auditService, UserRepository userRepository,
            RequestCache requestCache) {
        this.mfaService = mfaService;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.requestCache = requestCache;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.equals(MFA_CHALLENGE_URL) || path.startsWith("/css/") || path.startsWith("/js/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // OIDC RP-initiated logout must remain reachable even before MFA completes.
        if (path.startsWith("/connect/logout")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof BankingUserDetails userDetails)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!userDetails.isMfaEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute(MFA_VERIFIED_ATTR))) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.equals(MFA_VERIFY_URL) && "POST".equalsIgnoreCase(request.getMethod())) {
            handleMfaVerify(request, response, userDetails);
            return;
        }

        // Persist the intercepted URL so we can resume the OAuth authorize flow after MFA.
        requestCache.saveRequest(request, response);
        response.sendRedirect(MFA_CHALLENGE_URL);
    }

    private void handleMfaVerify(HttpServletRequest request, HttpServletResponse response,
            BankingUserDetails userDetails) throws IOException {
        String raw = request.getParameter("code");
        String code = raw != null ? raw.trim() : null;
        String ip = resolveIp(request);
        String ua = request.getHeader("User-Agent");

        BankingUser user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        if (user == null) {
            auditService.record(
                    SecurityAuditEventType.MFA_FAILURE,
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    ip,
                    ua,
                    "MFA verify: user record missing for authenticated principal",
                    AuditEventDetail.mfaFailure("user record missing"));
            response.sendRedirect(MFA_CHALLENGE_URL + "?error");
            return;
        }

        if (code == null || code.isEmpty()) {
            auditService.record(
                    SecurityAuditEventType.MFA_FAILURE,
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    ip,
                    ua,
                    "Missing MFA code",
                    AuditEventDetail.mfaFailure("Missing MFA code"));
            response.sendRedirect(MFA_CHALLENGE_URL + "?error");
            return;
        }

        if (mfaService.isRecoveryCodeFormat(code)) {
            if (mfaService.consumeRecoveryCode(user, code)) {
                userRepository.save(user);
                request.getSession(true).setAttribute(MFA_VERIFIED_ATTR, Boolean.TRUE);
                int remaining = user.getMfaRecoveryCodes().size();
                auditService.record(
                        SecurityAuditEventType.MFA_RECOVERY_CODE_USED,
                        userDetails.getUserId(),
                        userDetails.getUsername(),
                        ip,
                        ua,
                        "Recovery code consumed; " + remaining + " remaining",
                        AuditEventDetail.mfaRecoveryCodeUsed(remaining));
                redirectAfterSuccessfulMfa(request, response);
            } else {
                auditService.record(
                        SecurityAuditEventType.MFA_FAILURE,
                        userDetails.getUserId(),
                        userDetails.getUsername(),
                        ip,
                        ua,
                        "Invalid recovery code",
                        AuditEventDetail.mfaFailure("Invalid recovery code"));
                response.sendRedirect(MFA_CHALLENGE_URL + "?error");
            }
            return;
        }

        String mfaSecret = user.getMfaSecret();
        if (mfaSecret != null && mfaService.verifyCode(mfaSecret, code)) {
            request.getSession(true).setAttribute(MFA_VERIFIED_ATTR, Boolean.TRUE);
            auditService.record(
                    SecurityAuditEventType.MFA_SUCCESS,
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    ip,
                    ua,
                    null,
                    AuditEventDetail.mfaSuccess("TOTP"));
            redirectAfterSuccessfulMfa(request, response);
        } else if (code.length() == 6 && code.chars().allMatch(Character::isDigit)) {
            auditService.record(
                    SecurityAuditEventType.MFA_FAILURE,
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    ip,
                    ua,
                    "Invalid TOTP code",
                    AuditEventDetail.mfaFailure("Invalid TOTP code"));
            response.sendRedirect(MFA_CHALLENGE_URL + "?error");
        } else {
            auditService.record(
                    SecurityAuditEventType.MFA_FAILURE,
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    ip,
                    ua,
                    "Invalid MFA code format (expect 6-digit TOTP or 8-digit recovery)",
                    AuditEventDetail.mfaFailure("Invalid MFA code format"));
            response.sendRedirect(MFA_CHALLENGE_URL + "?error");
        }
    }

    private void redirectAfterSuccessfulMfa(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest == null) {
            redirectStrategy.sendRedirect(request, response, "/logged-out");
            return;
        }
        String redirectUrl = savedRequest.getRedirectUrl();
        requestCache.removeRequest(request, response);
        redirectStrategy.sendRedirect(request, response, redirectUrl);
    }

    private static String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
