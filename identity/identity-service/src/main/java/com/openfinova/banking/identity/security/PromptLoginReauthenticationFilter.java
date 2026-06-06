package com.openfinova.banking.identity.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
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
 * authorization-server session is invalidated and the security context cleared, then the browser is
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

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        response.sendRedirect(authorizeUrlWithoutPrompt(request));
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
     * Rebuilds the authorize URL preserving the original (already-encoded) query string but dropping
     * the {@code prompt} parameter, so the post-login replay does not trigger this filter again.
     */
    private static String authorizeUrlWithoutPrompt(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null || query.isEmpty()) {
            return request.getRequestURI();
        }
        String filtered = Arrays.stream(query.split("&"))
                .filter(pair -> !pair.equals(PROMPT_PARAM) && !pair.startsWith(PROMPT_PARAM + "="))
                .collect(Collectors.joining("&"));
        return filtered.isEmpty() ? request.getRequestURI() : request.getRequestURI() + "?" + filtered;
    }
}
