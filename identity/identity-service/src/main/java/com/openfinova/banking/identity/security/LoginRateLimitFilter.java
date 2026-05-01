package com.openfinova.banking.identity.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.openfinova.banking.identity.config.LoginRateLimitProperties;

/**
 * Limits {@code POST /login} attempts per IP to complement account lockout (2.4).
 * On limit breach, redirects to the same URL as a failed login so callers cannot distinguish rate limit
 * from bad credentials.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private final LoginIpRateLimiter limiter;
    private final LoginRateLimitProperties properties;

    public LoginRateLimitFilter(LoginIpRateLimiter limiter, LoginRateLimitProperties properties) {
        this.limiter = limiter;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.isEnabled() || !HttpMethod.POST.matches(request.getMethod())
                || !"/login".equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        String ip = ClientIpResolver.resolve(request);
        if (!limiter.tryConsume(ip)) {
            log.warn("Login rate limit exceeded for ip={}", ip);
            response.sendRedirect("/login?error");
            return;
        }
        chain.doFilter(request, response);
    }
}
