package com.openfinova.banking.gl.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.gl.api.entity.GLApprovalRole;
import com.openfinova.banking.gl.api.entity.GLTransactionSource;
import com.openfinova.banking.gl.entity.GLAuthorizationLimit;
import com.openfinova.banking.gl.repository.GLAuthorizationLimitRepository;

/**
 * Cached read accessors for authorization limits (used by approval queue and eligibility checks).
 * Mutations occur via Liquibase / direct DB — rely on TTL for freshness.
 */
@Service
@Transactional(readOnly = true)
public class GLAuthorizationLimitQueryService {

    /** Must match banking-app {@code BankingCacheNames.GL_AUTH_LIMITS}. */
    public static final String CACHE_NAME = "glAuthLimits";

    private final GLAuthorizationLimitRepository authLimitRepository;

    public GLAuthorizationLimitQueryService(GLAuthorizationLimitRepository authLimitRepository) {
        this.authLimitRepository = authLimitRepository;
    }

    @Cacheable(value = CACHE_NAME, key = "#role.name() + '_' + #currency + '_' + (#source != null ? #source.name() : 'null')")
    public List<GLAuthorizationLimit> findByRoleCurrencyAndSource(GLApprovalRole role, String currency,
            GLTransactionSource source) {
        return authLimitRepository.findByRoleCurrencyAndSource(role, currency, source);
    }

    @Cacheable(value = CACHE_NAME, key = "#role.name() + '_byRole'")
    public List<GLAuthorizationLimit> findByRole(GLApprovalRole role) {
        return authLimitRepository.findByRole(role);
    }
}
