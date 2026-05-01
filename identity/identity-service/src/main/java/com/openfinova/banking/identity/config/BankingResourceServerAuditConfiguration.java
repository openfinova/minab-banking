package com.openfinova.banking.identity.config;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import com.openfinova.banking.identity.audit.AuditEventDetail;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.security.ClientIpResolver;
import com.openfinova.banking.identity.service.SecurityAuditService;

/**
 * Audits and logs resource-server authentication/authorization outcomes for the banking API (2.4).
 * {@code 401} responses are logged only (avoids DB noise from unauthenticated crawlers);
 * {@code 403} responses are also written to the security audit trail.
 */
@Configuration
public class BankingResourceServerAuditConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BankingResourceServerAuditConfiguration.class);

    @Bean
    public AuthenticationEntryPoint bankingBearerAuthenticationEntryPoint() {
        BearerTokenAuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();
        return (request, response, authException) -> {
            log.warn(
                    "API authentication failed: method={} path={} reason={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    authException.getMessage());
            delegate.commence(request, response, authException);
        };
    }

    @Bean
    public AccessDeniedHandler bankingApiAccessDeniedHandler(SecurityAuditService auditService) {
        AccessDeniedHandlerImpl delegate = new AccessDeniedHandlerImpl();
        return (request, response, accessDeniedException) -> {
            String path = request.getRequestURI();
            String ip = ClientIpResolver.resolve(request);
            String ua = request.getHeader("User-Agent");
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : null;
            UUID userId = resolveUserId(auth);
            String detail = "HTTP 403 path=" + path
                    + (accessDeniedException.getMessage() != null ? (": " + accessDeniedException.getMessage()) : "");
            log.warn("API access denied: user={} path={}", username, path);
            auditService.record(
                    SecurityAuditEventType.API_ACCESS_DENIED,
                    userId,
                    username,
                    ip,
                    ua,
                    detail,
                    AuditEventDetail.accessDenied(request.getMethod(), path));
            delegate.handle(request, response, accessDeniedException);
        };
    }

    private static UUID resolveUserId(Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken jwt)) {
            return null;
        }
        try {
            return UUID.fromString(jwt.getToken().getSubject());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
