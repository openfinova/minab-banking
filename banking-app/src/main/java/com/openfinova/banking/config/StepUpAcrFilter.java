package com.openfinova.banking.config;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.openfinova.banking.identity.api.principal.BankingPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Enforces step-up authentication ({@link BankingPrincipal#ACR_GOLD}) on GL approve/reject.
 *
 * Scope intentionally narrow: identity/role write operations (user creation, locking, password
 * reset, etc.) are covered by the gold ACR that the staff portal requests at initial login —
 * no mid-task interruption is needed there. Step-up is reserved for GL approve/reject because
 * those are irreversible financial entries that warrant a second explicit authentication challenge
 * even within an already-gold session (defence-in-depth for the highest-risk operation the staff
 * portal exposes).
 */
@Component
public class StepUpAcrFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (requiresGoldAcr(request) && !hasGoldAcr()) {
            throw new AccessDeniedException(
                    "Step-up authentication required (acr=" + BankingPrincipal.ACR_GOLD
                            + "). Re-authenticate with MFA.");
        }
        filterChain.doFilter(request, response);
    }

    private static boolean requiresGoldAcr(HttpServletRequest request) {
        if (!HttpMethod.POST.name().equals(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/gl/approvals") && (path.endsWith("/approve") || path.endsWith("/reject"));
    }

    private static boolean hasGoldAcr() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return true;
        }
        Jwt jwt = jwtAuth.getToken();
        return BankingPrincipal.ACR_GOLD.equals(jwt.getClaimAsString(BankingPrincipal.CLAIM_ACR));
    }
}
