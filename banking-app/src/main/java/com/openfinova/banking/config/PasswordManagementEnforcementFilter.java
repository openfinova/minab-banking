package com.openfinova.banking.config;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.openfinova.banking.identity.api.IdentityService;
import com.openfinova.banking.identity.api.principal.BankingPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * When the JWT carries {@link BankingPrincipal#CLAIM_FORCE_PASSWORD_CHANGE}, blocks all banking API
 * use except reading own profile and changing password until the flag is cleared in the database.
 */
@Component
public class PasswordManagementEnforcementFilter extends OncePerRequestFilter {

    private final IdentityService identityService;

    public PasswordManagementEnforcementFilter(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            filterChain.doFilter(request, response);
            return;
        }
        Jwt jwt = jwtAuth.getToken();
        if (!isForcePasswordChangeStillRequired(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (isAllowedWhileForcedPasswordChange(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"title\":\"Password change required\",\"detail\":\"Change your password before using this API.\"}");
    }

    private boolean isForcePasswordChangeStillRequired(Jwt jwt) {
        if (!Boolean.TRUE.equals(jwt.getClaim(BankingPrincipal.CLAIM_FORCE_PASSWORD_CHANGE))) {
            return false;
        }
        Optional<UUID> userId = resolveUserId(jwt);
        if (userId.isEmpty()) {
            return true;
        }
        return identityService.isForcePasswordChangeRequired(userId.get());
    }

    private Optional<UUID> resolveUserId(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub != null && !sub.isBlank()) {
            try {
                return Optional.of(UUID.fromString(sub));
            } catch (IllegalArgumentException ignored) {
                // Older tokens may still have username in sub.
            }
        }
        String username = jwt.getClaimAsString("preferred_username");
        if (username != null && !username.isBlank()) {
            return identityService.getUserIdByUsername(username);
        }
        return Optional.empty();
    }

    private static boolean isAllowedWhileForcedPasswordChange(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return ("GET".equalsIgnoreCase(method) && "/api/v1/identity/me".equals(path))
                || ("PATCH".equalsIgnoreCase(method) && "/api/v1/identity/me/password".equals(path));
    }
}
