package com.openfinova.banking.identity.security;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Enforces the OpenID Connect {@code prompt=login} parameter on the authorization endpoint.
 *
 * Spring Authorization Server does not natively honour {@code prompt=login}; without this filter an
 * existing authenticated session is silently reused (single sign-on). That is unacceptable here
 * because the staff and customer portals run on the same host ({@code localhost}, different ports) and
 * therefore share one authorization-server session cookie. Silent SSO would let a staff session leak
 * into the customer channel (and vice versa) and would defeat the policy requirement that staff
 * re-authenticate rather than ride an existing session (see docs/security/oauth-session-policy.md).
 *
 * Behaviour
 * When a {@code GET /oauth2/authorize} request carries {@code prompt=login}, the current
 * authorization-server authentication is cleared (without invalidating the HTTP session, so the
 * Spring Security {@code RequestCache} survives for the OAuth redirect chain), then the browser is
 * redirected back to the same authorize request with the {@code prompt} parameter removed. The
 * unauthenticated replay drives a fresh form login (and MFA) before a code is issued.
 *
 * Removing {@code prompt} on the redirect is what prevents an infinite re-authentication loop: the
 * request that is saved and replayed after login no longer asks to be re-prompted.
 */
public class PromptLoginReauthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZE_ENDPOINT = "/oauth2/authorize";
    private static final String PROMPT_PARAM = "prompt";
    private static final String PROMPT_LOGIN = "login";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!requiresFreshLogin(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Clear authentication only — do not invalidate the HTTP session. Session invalidation
        // destroys RequestCache entries that the authorize → login → MFA chain relies on; when MFA
        // completes with no saved request the user is sent to /logged-out and cannot finish OAuth.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            session.removeAttribute(MfaChallengeFilter.MFA_VERIFIED_ATTR);
        }
        SecurityContextHolder.clearContext();
        redirectToAuthorizeWithoutPrompt(request, response);
    }

    /**
     * Redirects to the authorize endpoint with {@code prompt} removed. The target is rebuilt from a
     * fixed path plus decoded query parameters (never the raw query string) and validated as a
     * relative URI before {@link HttpServletResponse#sendRedirect(String)} to satisfy open-redirect
     * checks (CodeQL {@code java/unvalidated-url-redirection}).
     */
    private static void redirectToAuthorizeWithoutPrompt(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String target = authorizeUrlWithoutPrompt(request);
        try {
            URI uri = new URI(target);
            if (!uri.isAbsolute() && !target.startsWith("//") && AUTHORIZE_ENDPOINT.equals(uri.getPath())) {
                response.sendRedirect(uri.toString());
                return;
            }
        } catch (URISyntaxException ignored) {
            // Fall through to the fixed authorize path.
        }
        response.sendRedirect(AUTHORIZE_ENDPOINT);
    }

    private static boolean requiresFreshLogin(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        if (!AUTHORIZE_ENDPOINT.equals(request.getRequestURI())) {
            return false;
        }
        String prompt = request.getParameter(PROMPT_PARAM);
        if (prompt == null) {
            return false;
        }
        // The OIDC prompt parameter is a space-delimited list; act if "login" is present.
        return Arrays.stream(prompt.trim().split("\\s+")).anyMatch(PROMPT_LOGIN::equalsIgnoreCase);
    }

    /**
     * Rebuilds the authorize URL from a fixed path and decoded parameters, dropping {@code prompt}
     * so the post-login replay does not trigger this filter again.
     */
    private static String authorizeUrlWithoutPrompt(HttpServletRequest request) {
        String query = request.getParameterMap().entrySet().stream()
                .filter(entry -> !PROMPT_PARAM.equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> Arrays.stream(entry.getValue()).map(value -> encodeQueryParam(entry.getKey(), value)))
                .collect(Collectors.joining("&"));
        return query.isEmpty() ? AUTHORIZE_ENDPOINT : AUTHORIZE_ENDPOINT + "?" + query;
    }

    private static String encodeQueryParam(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
