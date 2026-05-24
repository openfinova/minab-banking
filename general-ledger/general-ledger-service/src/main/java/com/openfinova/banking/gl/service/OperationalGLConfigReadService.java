package com.openfinova.banking.gl.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;
import com.openfinova.banking.gl.entity.OperationalGLConfig;
import com.openfinova.banking.gl.repository.OperationalGLConfigRepository;

/**
 * Read-through cache wrapper for operational GL configuration lookups.
 */
@Service
@Transactional(readOnly = true)
public class OperationalGLConfigReadService {

    private final OperationalGLConfigRepository operationalGLConfigRepository;

    public OperationalGLConfigReadService(OperationalGLConfigRepository operationalGLConfigRepository) {
        this.operationalGLConfigRepository = operationalGLConfigRepository;
    }

    @Cacheable(value = "operationalGlConfig", key = "#type.name()")
    public Optional<OperationalGLConfig> resolveActiveOperationalConfig(OperationalGLAccountType type) {
        return operationalGLConfigRepository.findByConfigTypeAndIsActiveTrue(type);
    }

    @Cacheable(value = "operationalGlConfig", key = "'allActive'")
    public List<OperationalGLConfig> findAllActive() {
        return operationalGLConfigRepository.findByIsActiveTrue();
    }
}
