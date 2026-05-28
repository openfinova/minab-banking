package com.openfinova.banking.gl.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.gl.api.entity.GLAuditAction;
import com.openfinova.banking.gl.api.entity.GLEntityType;
import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;
import com.openfinova.banking.gl.dto.ChartOfAccountsImport;
import com.openfinova.banking.gl.dto.OperationalAccountValidationResult;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.entity.OperationalGLConfig;
import com.openfinova.banking.gl.repository.GLAccountRepository;
import com.openfinova.banking.gl.repository.OperationalGLConfigRepository;

/**
 * Implementation of OperationalGLAccountService for managing operational GL account configurations.
 */
@Service
@Transactional
public class OperationalGLAccountService {

    private static final Logger logger = LoggerFactory.getLogger(OperationalGLAccountService.class);

    private final OperationalGLConfigRepository operationalGLConfigRepository;
    private final GLAccountRepository glAccountRepository;
    private final AuditService auditService;
    private final OperationalGLConfigReadService operationalGLConfigReadService;

    public OperationalGLAccountService(OperationalGLConfigRepository operationalGLConfigRepository,
            GLAccountRepository glAccountRepository, AuditService auditService,
            OperationalGLConfigReadService operationalGLConfigReadService) {
        this.operationalGLConfigRepository = operationalGLConfigRepository;
        this.glAccountRepository = glAccountRepository;
        this.auditService = auditService;
        this.operationalGLConfigReadService = operationalGLConfigReadService;
    }

    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")
    @Transactional(readOnly = true)
    public UUID getOperationalGLAccount(OperationalGLAccountType type) {
        logger.debug("Getting operational GL account for type: {}", type);

        Optional<OperationalGLConfig> config = operationalGLConfigReadService.resolveActiveOperationalConfig(type);

        if (config.isEmpty()) {
            throw new IllegalStateException(
                    String.format(
                            "No operational GL account configured for type: %s. "
                                    + "Please configure operational accounts using createStandardOperationalAccounts() "
                                    + "or configureOperationalAccount().",
                            type));
        }

        UUID glAccountId = config.get().getGlAccountId();
        logger.debug("Resolved operational GL account: {} for type: {}", glAccountId, type);

        return glAccountId;
    }

    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")
    @Transactional(readOnly = true)
    public UUID getOperationalGLAccount(String operationalGLAccountType) {
        try {
            return getOperationalGLAccount(OperationalGLAccountType.valueOf(operationalGLAccountType));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid operational GL account type: " + operationalGLAccountType);
        }
    }

    @PreAuthorize("hasAuthority('gl:read')")
    @Transactional(readOnly = true)
    public UUID getOperationalGLAccountOrNull(OperationalGLAccountType type) {
        logger.debug("Getting operational GL account (nullable) for type: {}", type);

        Optional<OperationalGLConfig> config = operationalGLConfigReadService.resolveActiveOperationalConfig(type);

        return config.map(OperationalGLConfig::getGlAccountId).orElse(null);
    }

    @CacheEvict(value = "operationalGlConfig", allEntries = true)
    @PreAuthorize("hasAuthority('gl:approve')")
    public OperationalGLConfig configureOperationalAccount(OperationalGLAccountType type, UUID glAccountId,
            String createdBy) {
        logger.info("Configuring operational account - type: {}, glAccountId: {}", type, glAccountId);

        // Validate that the GL account exists
        GLAccount glAccount = glAccountRepository.findById(glAccountId)
                .orElseThrow(() -> new IllegalArgumentException("GL account not found: " + glAccountId));

        // Check if configuration already exists
        Optional<OperationalGLConfig> existingConfig = operationalGLConfigRepository
                .findByConfigTypeAndIsActiveTrue(type);

        if (existingConfig.isPresent()) {
            // Update existing configuration
            OperationalGLConfig config = existingConfig.get();
            UUID oldGLAccountId = config.getGlAccountId();

            config.updateGLAccount(glAccount, createdBy);
            OperationalGLConfig savedConfig = operationalGLConfigRepository.save(config);
            logger.info("Updated existing operational account configuration: {}", savedConfig.getId());

            // Audit log: configuration update
            Map<String, Object> oldValues = Map
                    .of("configType", type.toString(), "glAccountId", oldGLAccountId.toString());
            Map<String, Object> newValues = Map
                    .of("configType", type.toString(), "glAccountId", glAccountId.toString());
            auditService.logAudit(
                    GLEntityType.OPERATIONAL_CONFIG,
                    savedConfig.getId(),
                    GLAuditAction.CONFIG_CHANGE,
                    createdBy,
                    oldValues,
                    newValues,
                    "Operational account configuration updated");

            return savedConfig;
        } else {
            // Create new configuration
            OperationalGLConfig config = new OperationalGLConfig(type, glAccount, createdBy);
            config.setDescription(type.getDescription());
            OperationalGLConfig savedConfig = operationalGLConfigRepository.save(config);
            logger.info("Created new operational account configuration: {}", savedConfig.getId());

            // Audit log: configuration creation
            Map<String, Object> newValues = Map.of(
                    "configType",
                    type.toString(),
                    "glAccountId",
                    glAccountId.toString(),
                    "description",
                    type.getDescription());
            auditService.logAudit(
                    GLEntityType.OPERATIONAL_CONFIG,
                    savedConfig.getId(),
                    GLAuditAction.CONFIG_CHANGE,
                    createdBy,
                    null, // no old values for creation
                    newValues,
                    "Operational account configuration created");

            return savedConfig;
        }
    }

    @PreAuthorize("hasAuthority('gl:read')")
    @Transactional(readOnly = true)
    public boolean isConfigured(OperationalGLAccountType type) {
        return operationalGLConfigRepository.existsByConfigType(type);
    }

    @PreAuthorize("hasAuthority('gl:read')")
    @Transactional(readOnly = true)
    public Optional<OperationalGLConfig> getConfiguration(OperationalGLAccountType type) {
        return operationalGLConfigReadService.resolveActiveOperationalConfig(type);
    }

    @PreAuthorize("hasAuthority('gl:read')")
    @Transactional(readOnly = true)
    public List<OperationalGLConfig> getAllActiveConfigurations() {
        return operationalGLConfigReadService.findAllActive();
    }

    @CacheEvict(value = "operationalGlConfig", allEntries = true)
    @PreAuthorize("hasAuthority('gl:approve')")
    public void deactivateConfiguration(UUID configId, String deactivatedBy) {
        logger.info("Deactivating operational GL configuration: {}", configId);

        OperationalGLConfig config = operationalGLConfigRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + configId));

        config.deactivate(deactivatedBy);
        operationalGLConfigRepository.save(config);

        logger.info("Deactivated operational GL configuration: {}", configId);
    }

    @CacheEvict(value = "operationalGlConfig", allEntries = true)
    @PreAuthorize("hasAuthority('gl:approve')")
    public void activateConfiguration(UUID configId, String activatedBy) {
        logger.info("Activating operational GL configuration: {}", configId);

        OperationalGLConfig config = operationalGLConfigRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + configId));

        config.activate(activatedBy);
        operationalGLConfigRepository.save(config);

        logger.info("Activated operational GL configuration: {}", configId);
    }

    @CacheEvict(value = "operationalGlConfig", allEntries = true)
    @PreAuthorize("hasAuthority('gl:approve')")
    public int createStandardOperationalAccounts(String createdBy) {
        logger.info("Creating standard operational accounts by {}", createdBy);

        int accountsCreated = 0;

        // Get the standard operational accounts template
        Map<OperationalGLAccountType, String> standardAccounts = getStandardOperationalAccountsTemplate();

        for (Map.Entry<OperationalGLAccountType, String> entry : standardAccounts.entrySet()) {
            OperationalGLAccountType type = entry.getKey();
            String glAccountCode = entry.getValue();

            // Check if configuration already exists
            if (operationalGLConfigRepository.existsByConfigType(type)) {
                logger.debug("Operational account already configured: {}", type);
                continue;
            }

            // Find the GL account by code
            Optional<GLAccount> glAccount = glAccountRepository.findByCode(glAccountCode);

            if (glAccount.isEmpty()) {
                logger.warn("GL account not found for code: {}. Skipping operational account: {}", glAccountCode, type);
                continue;
            }

            // Create the configuration
            try {
                OperationalGLConfig config = new OperationalGLConfig(type, glAccount.get(), createdBy);
                config.setDescription(type.getDescription());
                operationalGLConfigRepository.save(config);
                accountsCreated++;
                logger.info("Created operational account configuration: {} -> {} ", type, glAccountCode);
            } catch (Exception e) {
                logger.error("Failed to create operational account configuration for {}: {}", type, e.getMessage());
            }
        }

        logger.info("Created {} standard operational accounts", accountsCreated);
        return accountsCreated;
    }

    @PreAuthorize("hasAuthority('gl:read')")
    @Transactional(readOnly = true)
    public OperationalAccountValidationResult validateOperationalAccounts() {
        logger.debug("Validating operational accounts");

        // Define required operational account types
        List<OperationalGLAccountType> requiredTypes = Arrays.asList(
                OperationalGLAccountType.FEE_INCOME,
                OperationalGLAccountType.CASH_VAULT,
                OperationalGLAccountType.SUSPENSE);

        List<OperationalGLAccountType> missingTypes = new ArrayList<>();

        for (OperationalGLAccountType type : requiredTypes) {
            if (!isConfigured(type)) {
                missingTypes.add(type);
            }
        }

        boolean isValid = missingTypes.isEmpty();
        return new OperationalAccountValidationResult(isValid, missingTypes);
    }

    /**
     * Returns a template mapping of operational account types to GL account codes.
     * This extracts operational accounts from the unified StandardBankTemplateDefinition
     * which serves as the single source of truth for all GL account definitions.
     *
     * The GL account codes are automatically derived from the standard chart of accounts
     * where operational accounts are marked with OperationalGLAccountType metadata.
     *
     * @return map of operational account type to GL account code
     */
    private Map<OperationalGLAccountType, String> getStandardOperationalAccountsTemplate() {
        Map<OperationalGLAccountType, String> template = new HashMap<>();

        // Get the unified template definition and extract operational accounts
        ChartOfAccountsImport standardTemplate = StandardBankTemplateDefinition.getStandardTemplate("USD");

        // Extract all operational accounts from the template
        standardTemplate.getAccounts().forEach(account -> {
            if (account.getOperationalAccountType() != null) {
                template.put(account.getOperationalAccountType(), account.getCode());
            }
        });

        return template;
    }
}
