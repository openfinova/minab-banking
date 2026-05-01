package com.openfinova.banking.identity.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the real client IP address from an HTTP request, accounting for reverse proxies.
 *
 * The problem this solves:
 * When a request passes through a reverse proxy (load balancer, API gateway, CDN), the value
 * returned by HttpServletRequest.getRemoteAddr() is the proxy's IP, not the original client's IP.
 * Proxies typically preserve the originating address by appending it to the X-Forwarded-For header:
 *
 *   X-Forwarded-For: 203.0.113.42, 10.0.0.1, 10.0.0.2
 *
 * The format is a comma-separated chain where the first entry is the original client and each
 * subsequent entry is an intermediate proxy that appended its own address.
 *
 * Security note:
 * X-Forwarded-For is trivially spoofable — a client can send any value they like in this header.
 * This is acceptable for audit logging, where the goal is to record what was claimed. However,
 * if the resolved IP were ever used for an access control decision (such as IP allowlisting), the
 * header must only be trusted when it originates from known infrastructure, typically by reading
 * the last IP added by your own proxies rather than the first entry in the chain.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    /**
     * Returns the client IP address from the given HTTP request.
     *
     * If the X-Forwarded-For header is present and non-blank, the first (leftmost) value in the
     * comma-separated list is returned. This is the IP of the original client before any proxies
     * forwarded the request. Leading and trailing whitespace is trimmed.
     *
     * If the header is absent, the direct remote address from the socket connection is returned
     * via HttpServletRequest.getRemoteAddr(). This is the correct value when the application
     * receives requests directly without any intermediate proxy.
     *
     * Returns null if the request itself is null, which happens when this method is called outside
     * of an active HTTP request context (for example from a background or async thread).
     *
     * @param request the current HTTP request, or null if no request context is available
     * @return the resolved client IP address string, or null if request is null
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}