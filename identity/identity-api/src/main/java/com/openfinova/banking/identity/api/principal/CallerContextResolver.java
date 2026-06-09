package com.openfinova.banking.identity.api.principal;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility to build a {@link CallerContext} from the Spring Security
 * {@link Authentication} and the current {@link HttpServletRequest}.
 *
 * <p>Lives in {@code identity-api} so every module can use it without
 * depending on {@code identity-service}. Only requires types already
 * available in this module ({@link BankingPrincipal}, {@link UserType})
 * plus the Servlet API (provided scope).
 */
public final class CallerContextResolver {

    private CallerContextResolver() {
    }

    /**
     * Full resolution: identity from JWT + IP/UA from HTTP request.
     */
    public static CallerContext resolve(Authentication auth, HttpServletRequest request) {
        BankingPrincipal principal = BankingPrincipal.from(auth);
        return new CallerContext(
                principal.userId(),
                principal.username(),
                principal.userType(),
                principal.branchCode(),
                principal.customerPartyId(),
                principal.kycStatus(),
                resolveIpAddress(request),
                resolveUserAgent(request));
    }

    /**
     * Extract just the username (for simple audit fields).
     */
    public static String resolveUsername(Authentication auth) {
        return BankingPrincipal.from(auth).username();
    }

    /**
     * Returns the JWT {@code sub} claim as a user profile id for the current request.
     * Intended for self-service service methods that must not trust a caller-supplied user id.
     *
     * @throws AccessDeniedException if there is no authenticated principal or {@code sub} is missing
     */
    public static UUID requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        UUID userId = BankingPrincipal.from(authentication).userId();
        if (userId == null) {
            throw new AccessDeniedException("Cannot resolve user from token");
        }
        return userId;
    }

    /**
     * Resolve client IP, respecting {@code X-Forwarded-For} behind reverse proxies.
     * Returns the first (leftmost, i.e. original client) address.
     */
    public static String resolveIpAddress(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Resolve User-Agent header from the request.
     */
    public static String resolveUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "unknown";
    }
}
