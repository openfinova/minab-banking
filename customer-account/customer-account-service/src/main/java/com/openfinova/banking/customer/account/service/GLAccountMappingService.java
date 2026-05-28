package com.openfinova.banking.customer.account.service;

import com.openfinova.banking.customer.account.api.entity.GLAccountMappingType;
import com.openfinova.banking.customer.account.entity.Account;
import com.openfinova.banking.customer.account.entity.GLAccountMapping;
import com.openfinova.banking.customer.account.repository.AccountRepository;
import com.openfinova.banking.customer.account.repository.GLAccountMappingRepository;
import com.openfinova.banking.setup.api.DateTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/*
 * Implementation of GLAccountMappingService for managing the relationship between
 * customer accounts and General Ledger accounts.
 *
 * PURPOSE AND SCOPE
 *
 * This service manages the mapping layer that connects customer-facing accounts to
 * the underlying General Ledger (GL) accounts. In a dual-ledger banking architecture,
 * customer accounts are logical representations that customers interact with, while
 * GL accounts are the authoritative accounting records that maintain the double-entry
 * bookkeeping system.
 *
 * ARCHITECTURAL CONTEXT
 *
 * In a banking system, there are two distinct account layers:
 *
 * - Customer Accounts: User-facing accounts with account numbers, balances, and
 *   transaction history that customers see in their banking interface.
 *
 * - GL Accounts: The underlying accounting structure that maintains the bank's
 *   books according to accounting principles. A single customer account may map
 *   to multiple GL accounts for different purposes.
 *
 * MAPPING TYPES AND USE CASES
 *
 * A customer account can have multiple GL account mappings, each serving a different
 * purpose:
 *
 * - PRIMARY_BALANCE: The main GL account holding the customer's balance
 * - INTEREST_ACCRUAL: GL account for tracking accrued interest
 * - FEE_COLLECTION: GL account for collecting account maintenance fees
 * - OVERDRAFT_FACILITY: GL account for overdraft protection
 * - ESCROW_HOLD: GL account for temporary holds (e.g., pending transactions)
 *
 * MAPPING LIFECYCLE
 *
 * 1. Creation: Mapping is created via addMapping() when a customer account is
 *    opened or when a new GL relationship is established.
 *
 * 2. Active Use: The mapping is used by transaction services to determine which
 *    GL accounts to post transactions to.
 *
 * 3. Deactivation: Mapping is deactivated (not deleted) via deactivateMapping()
 *    when the relationship is no longer needed. This preserves audit history.
 *
 * DESIGN PRINCIPLES
 *
 * - Soft Deletes: Mappings are never physically deleted, only deactivated with
 *   reason and audit trail for compliance and historical reporting.
 *
 * - Uniqueness: The combination of customer account, mapping type, and GL account
 *   must be unique to prevent duplicate mappings.
 *
 * - Validation: All operations validate that referenced accounts exist before
 *   creating or modifying mappings.
 *
 * FUTURE ENHANCEMENTS
 *
 * - Support for weighted mappings to distribute balances across multiple GL accounts
 * - Temporal mappings with effective dates for scheduled GL account changes
 * - Bulk mapping operations for account migrations
 * - Mapping validation rules (e.g., ensure PRIMARY_BALANCE always exists)
 * - Event publishing for mapping changes to support real-time reconciliation
 */
@Service
@Transactional
public class GLAccountMappingService {

    private static final Logger logger = LoggerFactory.getLogger(GLAccountMappingService.class);

    private final GLAccountMappingRepository glAccountMappingRepository;
    private final AccountRepository accountRepository;
    private final DateTimeService dateTimeService;

    public GLAccountMappingService(GLAccountMappingRepository glAccountMappingRepository,
            AccountRepository accountRepository, DateTimeService dateTimeService) {
        this.glAccountMappingRepository = glAccountMappingRepository;
        this.accountRepository = accountRepository;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Adds a new GL account mapping to a customer account.
     *
     * @param accountId the unique identifier of the customer account
     * @param glAccountId the unique identifier of the general ledger account
     * @param mappingType the type of mapping to establish
     * @param createdBy the user or system creating the mapping
     * @return the newly created GL account mapping entity
     * @throws IllegalArgumentException if required parameters are missing or invalid
     */
    public GLAccountMapping addMapping(UUID accountId, UUID glAccountId, GLAccountMappingType mappingType,
            String createdBy) {
        logger.info(
                "Adding GL account mapping for account: {} type: {} glAccount: {}",
                accountId,
                mappingType,
                glAccountId);

        // Validate customer account exists and load the entity for the foreign key relationship
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        // Validate required parameters
        if (glAccountId == null) {
            throw new IllegalArgumentException("GL account ID is required");
        }
        if (mappingType == null) {
            throw new IllegalArgumentException("Mapping type is required");
        }
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Created by is required");
        }

        // Create the mapping entity using the constructor for required fields
        GLAccountMapping mapping = new GLAccountMapping(account, glAccountId, mappingType);
        mapping.setIsActive(true); // Explicitly set active status

        // Persist the mapping - createdAt is set automatically
        GLAccountMapping savedMapping = glAccountMappingRepository.save(mapping);
        logger.info("GL account mapping created successfully: {}", savedMapping.getId());

        return savedMapping;
    }

    /**
     * Deactivates an existing GL account mapping.
     *
     * @param mappingId the unique identifier of the mapping
     * @param reason the reason for deactivation
     * @param deactivatedBy the user or system deactivating the mapping
     * @throws IllegalArgumentException if the mapping is not found or parameters are missing
     */
    public void deactivateMapping(UUID mappingId, String reason, String deactivatedBy) {
        logger.info("Deactivating GL account mapping: {} reason: {}", mappingId, reason);

        // Load the mapping - fail fast if it doesn't exist
        GLAccountMapping mapping = glAccountMappingRepository.findById(mappingId)
                .orElseThrow(() -> new IllegalArgumentException("GL account mapping not found: " + mappingId));

        // Validate required parameters
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Deactivation reason is required");
        }
        if (deactivatedBy == null || deactivatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Deactivated by is required");
        }

        // Use the entity's deactivate method which handles all the details
        mapping.deactivate(reason, deactivatedBy, dateTimeService.now());

        // Persist the changes - this is idempotent
        glAccountMappingRepository.save(mapping);
        logger.info("GL account mapping deactivated successfully");
    }

    /**
     * Retrieves all active mappings for a customer account.
     *
     * @param accountId the unique identifier of the customer account
     * @return a list of active GL account mappings
     * @throws IllegalArgumentException if the account is not found
     */
    @Transactional(readOnly = true)
    public List<GLAccountMapping> getMappingsByAccount(UUID accountId) {
        logger.debug("Retrieving active GL account mappings for account: {}", accountId);

        // Validate account exists
        if (!accountRepository.existsById(accountId)) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }

        // Query for active mappings only
        return glAccountMappingRepository.findByCustomerAccountIdAndIsActiveTrue(accountId);
    }

    /**
     * Retrieves the GL account ID for a specific mapping type on an account.
     *
     * @param accountId the unique identifier of the customer account
     * @param mappingType the mapping type to query
     * @return the GL account ID, or null if no mapping is found
     * @throws IllegalArgumentException if required parameters are missing
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('account:read', 'service:account:read')")
    public UUID getGLAccountIdForType(UUID accountId, GLAccountMappingType mappingType) {
        logger.debug("Retrieving GL account ID for account: {} type: {}", accountId, mappingType);

        // Validate parameters
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID is required");
        }
        if (mappingType == null) {
            throw new IllegalArgumentException("Mapping type is required");
        }

        // Query for the specific mapping type
        return glAccountMappingRepository.findByCustomerAccountIdAndMappingTypeAndIsActiveTrue(accountId, mappingType)
                .map(GLAccountMapping::getGlAccountId).orElse(null); // Return null if no mapping found, as per interface contract
    }
}
