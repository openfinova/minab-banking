package com.openfinova.banking.config;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.openfinova.banking.identity.api.principal.BankingPrincipal;

/**
 * Supplies the current auditor username for JPA {@code @CreatedBy} / {@code @LastModifiedBy} fields.
 * Falls back to {@code SYSTEM} when no authenticated principal is present (batch jobs, sync tasks).
 */
@Component("bankingAuditorAware")
public class BankingAuditorAware implements AuditorAware<String> {

    static final String SYSTEM = "SYSTEM";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of(SYSTEM);
        }
        if ("anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.of(SYSTEM);
        }
        try {
            return Optional.of(BankingPrincipal.from(authentication).username());
        } catch (IllegalArgumentException ex) {
            return Optional.of(SYSTEM);
        }
    }
}
