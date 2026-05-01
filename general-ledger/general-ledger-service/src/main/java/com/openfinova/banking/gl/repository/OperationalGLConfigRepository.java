package com.openfinova.banking.gl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;
import com.openfinova.banking.gl.entity.OperationalGLConfig;

/**
 * Repository for OperationalGLConfig entity.
 */
@Repository
public interface OperationalGLConfigRepository extends JpaRepository<OperationalGLConfig, UUID> {

    /**
     * Finds an active operational GL configuration by type.
     * 
     * @param configType the operational account type
     * @return Optional containing the configuration if found
     */
    Optional<OperationalGLConfig> findByConfigTypeAndIsActiveTrue(OperationalGLAccountType configType);

    /**
     * Checks if a configuration exists for the given type.
     * 
     * @param configType the operational account type
     * @return true if configuration exists
     */
    boolean existsByConfigType(OperationalGLAccountType configType);

    /**
     * Finds all active configurations.
     * 
     * @return list of all active configurations
     */
    List<OperationalGLConfig> findByIsActiveTrue();

    /**
     * Finds all configurations that reference a specific GL account.
     * 
     * @param glAccountId the GL account ID
     * @return list of configurations using this GL account
     */
    List<OperationalGLConfig> findByGlAccountId(UUID glAccountId);

    /**
     * Counts active configurations for a specific type.
     * 
     * @param configType the operational account type
     * @return count of active configurations
     */
    @Query("SELECT COUNT(c) FROM OperationalGLConfig c WHERE c.configType = :configType AND c.isActive = true")
    long countActiveByConfigType(@Param("configType") OperationalGLAccountType configType);
}
