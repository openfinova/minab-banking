package com.openfinova.banking.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Guards the internal machine-to-machine endpoints ({@code /internal/**}) with a shared secret.
 *
 * The Keycloak SPI calls these endpoints with no end-user token, so the usual bearer-token
 * authentication does not apply. Each request must present the {@code X-Internal-Token} header
 * matching {@code banking.internal.token}; otherwise the request is rejected with 401 before it
 * reaches any controller. The endpoints are additionally network-restricted to the container
 * network in deployment.
 */
@Component
public class InternalApiTokenFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH_PREFIX = "/internal/";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final String expectedToken;

    public InternalApiTokenFilter(@Value("${banking.internal.token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String provided = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (expectedToken.isBlank() || provided == null || !constantTimeEquals(provided, expectedToken)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
