package com.openfinova.banking.identity.config;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.openfinova.banking.identity.audit.AuditEventDetail;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.security.ClientIpResolver;
import com.openfinova.banking.identity.service.SecurityAuditService;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Audits and logs resource-server authentication/authorization outcomes for the banking API (2.4).
 * {@code 401} responses are logged only (avoids DB noise from unauthenticated crawlers);
 * {@code 403} responses are also written to the security audit trail.
 */
@Configuration
public class BankingResourceServerAuditConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BankingResourceServerAuditConfiguration.class);

    /** Jackson 3 ({@code tools.jackson}) — same pattern as {@link com.openfinova.banking.identity.converter.AuditEventDetailConverter}. */
    private static final ObjectMapper JSON = JsonMapper.builder().build();

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
        return (request, response, accessDeniedException) -> {
            String path = request.getRequestURI();
            String ip = ClientIpResolver.resolve(request);
            String ua = request.getHeader("User-Agent");
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : null;
            UUID userId = resolveUserId(auth);
            String auditDetail = "HTTP 403 path=" + path
                    + (accessDeniedException.getMessage() != null ? (": " + accessDeniedException.getMessage()) : "");
            log.warn("API access denied: user={} path={}", username, path);
            auditService.record(
                    SecurityAuditEventType.API_ACCESS_DENIED,
                    userId,
                    username,
                    ip,
                    ua,
                    auditDetail,
                    AuditEventDetail.accessDenied(request.getMethod(), path));
            writeJsonForbidden(response, accessDeniedException.getMessage());
        };
    }

    /**
     * Writes a JSON {@link ProblemDetail} 403 response directly.
     *
     * Delegating to {@link org.springframework.security.web.access.AccessDeniedHandlerImpl} uses
     * {@link HttpServletResponse#sendError}, which hands control to Spring Boot's error controller.
     * Because {@code spring.web.error.include-message=never} is set, the exception message — including
     * the step-up requirement detail — is stripped from the response body. Writing directly keeps the
     * detail visible to the BFF, which needs it to distinguish a step-up 403 from a plain 403.
     */
    private static void writeJsonForbidden(HttpServletResponse response, String detail) throws IOException {
        ProblemDetail pd = ProblemDetail
                .forStatusAndDetail(HttpStatus.FORBIDDEN, detail != null ? detail : "Access denied");
        pd.setTitle("Access Denied");
        pd.setType(URI.create("/errors/access-denied"));
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/problem+json;charset=UTF-8");
        JSON.writeValue(response.getWriter(), pd);
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
