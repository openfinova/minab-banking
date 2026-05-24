package com.openfinova.banking.gl.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.gl.api.dto.CreateGLAccountRequest;
import com.openfinova.banking.gl.api.dto.DailyBalanceSnapshot;
import com.openfinova.banking.gl.api.dto.ValidationResult;
import com.openfinova.banking.gl.api.entity.GLAccountStatus;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import com.openfinova.banking.gl.api.entity.GLAuditAction;
import com.openfinova.banking.gl.api.entity.GLEntityType;
import com.openfinova.banking.gl.dto.AccountExportData;
import com.openfinova.banking.gl.dto.AccountUsageStatistics;
import com.openfinova.banking.gl.dto.AccountsHierarchyValidationResult;
import com.openfinova.banking.gl.dto.ChartOfAccountsExport;
import com.openfinova.banking.gl.dto.ChartOfAccountsImport;
import com.openfinova.banking.gl.dto.ChartOfAccountsImportResult;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.entity.GLDailyBalance;
import com.openfinova.banking.gl.entity.OperationalGLConfig;
import com.openfinova.banking.gl.repository.GLAccountRepository;
import com.openfinova.banking.gl.repository.GLJournalEntryRepository;
import com.openfinova.banking.gl.repository.OperationalGLConfigRepository;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Implementation of GLAccountService providing comprehensive General Ledger account management.
 * This service handles the complete lifecycle of GL accounts including creation, maintenance,
 * balance inquiries, trial balance generation, and chart of accounts management.
 * Key responsibilities:
 * - GL account creation and maintenance
 * - Account hierarchy management and validation
 * - Balance calculations and trial balance generation
 * - Fiscal period integration and posting controls
 * - Chart of accounts import/export functionality
 * - Account usage statistics and archival
 */
@Service
@Transactional
public class GLAccountService {

    private static final Logger logger = LoggerFactory.getLogger(GLAccountService.class);

    @Value("${gl.account.hierarchy.max-depth:10}")
    private int maxHierarchyDepth;

    private final GLAccountRepository glAccountRepository;
    private final BalanceService balanceService;
    private final DateTimeService dateTimeService;
    private final AuditService auditService;
    private final OperationalGLConfigRepository operationalGLConfigRepository;
    private final GLJournalEntryRepository journalEntryRepository;

    /**
     * Constructor for dependency injection.
     *
     * @param glAccountRepository the GL account repository
     * @param balanceService the balance service for all balance-related operations
     * @param dateTimeService the date/time service for business date operations
     * @param auditService the audit service for regulatory compliance audit logging
     * @param journalEntryRepository the journal entry repository for posted-history checks
     */
    public GLAccountService(GLAccountRepository glAccountRepository, BalanceService balanceService,
            DateTimeService dateTimeService, AuditService auditService,
            OperationalGLConfigRepository operationalGLConfigRepository,
            GLJournalEntryRepository journalEntryRepository) {
        this.glAccountRepository = glAccountRepository;
        this.balanceService = balanceService;
        this.dateTimeService = dateTimeService;
        this.auditService = auditService;
        this.operationalGLConfigRepository = operationalGLConfigRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    /**
     * Creates a new GL account with validation and hierarchy management.
     *
     * @param glAccountRequest the account creation request containing all necessary details
     * @return the newly created and persisted GL account
     * @throws IllegalArgumentException if the account code already exists or validation fails
     * @throws IllegalStateException if the parent account is invalid for hierarchy rules
     */
    @CacheEvict(value = "glAccounts", allEntries = true)
    public GLAccount createAccount(CreateGLAccountRequest glAccountRequest) {
        logger.info("Creating GL account with request: {}", glAccountRequest);

        // Validate account code uniqueness
        if (glAccountRepository.existsByCode(glAccountRequest.getCode())) {
            throw new IllegalArgumentException("Account code already exists: " + glAccountRequest.getCode());
        }

        // Create new GL account
        GLAccount account = new GLAccount(
                glAccountRequest.getCode(),
                glAccountRequest.getName(),
                glAccountRequest.getType(),
                glAccountRequest.getCurrency(),
                glAccountRequest.getCreatedBy());

        // Set optional fields
        if (glAccountRequest.isContra()) {
            account.setContra(true); // flips normalBalance automatically
        }

        account.setDescription(glAccountRequest.getDescription());

        if (glAccountRequest.getMetadata() != null) {
            account.setMetadata(glAccountRequest.getMetadata());
        }

        // Set parent if specified
        if (glAccountRequest.getParentId() != null) {
            Optional<GLAccount> parent = glAccountRepository.findById(glAccountRequest.getParentId());
            if (parent.isEmpty()) {
                throw new ResourceNotFoundException("GLAccount", glAccountRequest.getParentId());
            }

            // Validate parent is active
            if (!parent.get().isActive()) {
                throw new IllegalStateException(
                        "Cannot create account under inactive parent: " + glAccountRequest.getParentId());
            }

            account.setParent(parent.get());
        }

        GLAccount savedAccount = glAccountRepository.save(account);
        logger.info("Successfully created GL account: {} with ID: {}", savedAccount.getCode(), savedAccount.getId());

        // Audit log: account creation
        Map<String, Object> newValues = Map.of(
                "code",
                savedAccount.getCode(),
                "name",
                savedAccount.getName(),
                "type",
                savedAccount.getType().toString(),
                "currency",
                savedAccount.getCurrency(),
                "status",
                savedAccount.getStatus().toString(),
                "parentId",
                savedAccount.getParent() != null ? savedAccount.getParent().getId().toString() : "none");
        auditService.logAudit(
                GLEntityType.GL_ACCOUNT,
                savedAccount.getId(),
                GLAuditAction.CREATE,
                glAccountRequest.getCreatedBy(),
                null, // no old values for CREATE
                newValues,
                "Account created");

        return savedAccount;
    }

    /**
     * Retrieves a GL account by its unique identifier.
     *
     * @param id the UUID of the account to retrieve
     * @return an Optional containing the account if found, empty otherwise
     */
    @Cacheable(value = "glAccounts", key = "#id")
    public Optional<GLAccount> getAccountById(UUID id) {
        logger.debug("Getting GL account by ID: {}", id);
        return glAccountRepository.findById(id);
    }

    /**
     * Finds a GL account by its internal code.
     *
     * @param code the account code to search for
     * @return an Optional containing the account if found, empty otherwise
     */
    @Cacheable(value = "glAccounts", key = "#code", unless = "#code == null || #code.isBlank()")
    public Optional<GLAccount> findByCode(String code) {
        logger.debug("Finding GL account by code: {}", code);
        return glAccountRepository.findByCode(code);
    }

    /**
     * Retrieves all direct child accounts for a given parent account.
     *
     * @param parentId the UUID of the parent account
     * @return a list of direct child accounts, empty list if no children exist
     */
    public List<GLAccount> getChildAccounts(UUID parentId) {
        logger.debug("Getting child accounts for parent: {}", parentId);
        return glAccountRepository.findByParentId(parentId);
    }

    /**
     * Deactivates a GL account by setting its status to INACTIVE with reason tracking.
     * Once inactivated, an account cannot be reactivated. Create a new account instead.
     *
     * @param id the UUID of the account to deactivate
     * @param reason the reason for inactivation (for audit trail)
     * @return the updated GL account with INACTIVE status
     * @throws IllegalArgumentException if the account is not found
     * @throws IllegalStateException if the account has active children
     */
    @CacheEvict(value = "glAccounts", allEntries = true)
    public GLAccount deactivateAccount(UUID id, String reason) {
        logger.info("Deactivating account: {} with reason: {}", id, reason);

        Optional<GLAccount> accountOpt = glAccountRepository.findById(id);
        if (accountOpt.isEmpty()) {
            throw new ResourceNotFoundException("GLAccount", id);
        }

        GLAccount account = accountOpt.get();

        // Validate no active children
        if (account.hasChildren()) {
            List<GLAccount> activeChildren = glAccountRepository.findByParentId(id).stream().filter(GLAccount::isActive)
                    .toList();

            if (!activeChildren.isEmpty()) {
                throw new IllegalStateException("Cannot deactivate account with active child accounts: " + id);
            }
        }

        // Validate not referenced by any active operational configuration
        List<OperationalGLConfig> activeConfigs = operationalGLConfigRepository.findByGlAccountId(id).stream()
                .filter(OperationalGLConfig::isActive).toList();
        if (!activeConfigs.isEmpty()) {
            throw new IllegalStateException(
                    String.format(
                            "Cannot deactivate GL account %s: it is still referenced by %d active "
                                    + "operational configuration(s): %s",
                            id,
                            activeConfigs.size(),
                            activeConfigs.stream().map(c -> c.getConfigType().toString()).toList()));
        }

        // Capture old status for audit
        GLAccountStatus oldStatus = account.getStatus();

        account.markInactive(reason);
        account.setUpdatedBy("system"); // TODO: replace with authenticated username
        GLAccount deactivatedAccount = glAccountRepository.save(account);

        logger.info("Successfully deactivated account ID: {} with reason: {}", id, reason);

        // Audit log: status change (mandatory reason)
        Map<String, Object> oldValues = Map
                .of("status", oldStatus.toString(), "accountCode", account.getCode(), "accountName", account.getName());
        Map<String, Object> newValues = Map.of(
                "status",
                deactivatedAccount.getStatus().toString(),
                "accountCode",
                deactivatedAccount.getCode(),
                "accountName",
                deactivatedAccount.getName());
        auditService.logAudit(
                GLEntityType.GL_ACCOUNT,
                deactivatedAccount.getId(),
                GLAuditAction.STATUS_CHANGE,
                "system", // TODO: pass username from request when available
                oldValues,
                newValues,
                reason // Mandatory reason for STATUS_CHANGE
        );

        return deactivatedAccount;
    }

    /**
     * Updates an existing GL account.
     *
     * @param account the account to update
     * @return the updated GL account
     * @throws IllegalArgumentException if the account is not found
     */
    @CacheEvict(value = "glAccounts", allEntries = true)
    public GLAccount updateAccount(GLAccount account) {
        logger.info("Updating GL account with ID: {}", account.getId());

        if (account.getId() == null) {
            throw new IllegalArgumentException("Account ID is required for update");
        }

        // Fetch existing state — used both for the audit trail and the contra-flip guard.
        GLAccount oldAccount = glAccountRepository.findById(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("GLAccount", account.getId()));

        // Guard: changing the contra flag rewrites normalBalance, which retroactively
        // misinterprets every posted journal entry for this account.  Block it once
        // any posted history exists — the account must be inactivated and a new one
        // created instead.
        if (account.isContra() != oldAccount.isContra()
                && journalEntryRepository.existsPostedEntryForAccount(account.getId())) {
            throw new IllegalStateException(
                    "Cannot change the contra flag on account '" + oldAccount.getCode()
                            + "' because it already has posted journal entries. "
                            + "Inactivate this account and create a new one with the correct contra setting.");
        }
        Map<String, Object> oldValues = Map.of(
                "code",
                oldAccount.getCode(),
                "name",
                oldAccount.getName(),
                "description",
                oldAccount.getDescription() != null ? oldAccount.getDescription() : "");

        // Validate code uniqueness if changed
        Optional<GLAccount> existingWithCode = glAccountRepository.findByCode(account.getCode());
        if (existingWithCode.isPresent() && !existingWithCode.get().getId().equals(account.getId())) {
            throw new IllegalArgumentException("Account code already exists: " + account.getCode());
        }

        GLAccount updatedAccount = glAccountRepository.save(account);
        logger.info("Successfully updated GL account: {}", updatedAccount.getCode());

        // Audit log: account update
        Map<String, Object> newValues = Map.of(
                "code",
                updatedAccount.getCode(),
                "name",
                updatedAccount.getName(),
                "description",
                updatedAccount.getDescription() != null ? updatedAccount.getDescription() : "");
        auditService.logAudit(
                GLEntityType.GL_ACCOUNT,
                updatedAccount.getId(),
                GLAuditAction.UPDATE,
                "system", // TODO: pass username from request when available
                oldValues,
                newValues,
                "Account updated");

        return updatedAccount;
    }

    /**
     * Gets all accounts with pagination support.
     *
     * @param pageable pagination parameters
     * @return page of GL accounts
     */
    public Page<GLAccount> getAllAccounts(Pageable pageable) {
        logger.debug("Getting all GL accounts - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return glAccountRepository.findAll(pageable);
    }

    /**
     * Retrieves the complete chart of accounts in hierarchical order.
     *
     * @return a list of all GL accounts ordered by account code
     */
    public List<GLAccount> getChartOfAccounts() {
        logger.debug("Getting complete chart of accounts");
        return glAccountRepository.findAll();
    }

    /**
     * Retrieves all accounts that can accept journal entries (leaf accounts without children).
     *
     * @return a list of postable accounts ordered by account code
     */
    public List<GLAccount> getPostableAccounts() {
        logger.debug("Getting all postable accounts");
        return glAccountRepository.findAllPostableAccounts();
    }

    /**
     * Validates if a GL account is eligible for posting transactions.
     *
     * @param accountId the UUID of the account to validate
     * @throws IllegalStateException if the account is not available for posting
     */
    public void validateAccountForPosting(UUID accountId) {
        logger.debug("Validating account for posting: {}", accountId);
        if (!isAccountActiveForPosting(accountId)) {
            throw new IllegalStateException("Account is not available for posting: " + accountId);
        }
    }

    /**
     * Checks if a GL account is active and available for posting transactions.
     *
     * @param accountId the UUID of the account to check
     * @return true if the account is active and postable, false otherwise
     */
    public boolean isAccountActiveForPosting(UUID accountId) {
        logger.debug("Checking if account is active for posting: {}", accountId);
        Optional<GLAccount> account = getAccountById(accountId);
        return account.isPresent() && account.get().getStatus() == GLAccountStatus.ACTIVE
                && !account.get().hasChildren(); // Only leaf accounts can accept postings
    }

    /**
     * Retrieves the complete path from root to the specified account.
     *
     * @param accountId the UUID of the target account
     * @return a list of accounts from root to the specified account
     */
    public List<GLAccount> getAccountPath(UUID accountId) {
        logger.debug("Getting account path for: {}", accountId);

        Optional<GLAccount> accountOpt = glAccountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return Collections.emptyList();
        }

        List<GLAccount> path = new ArrayList<>();
        GLAccount current = accountOpt.get();

        // Build path from target to root
        while (current != null) {
            path.add(current);
            current = current.getParent();
        }
        // reverse to get path from root to target
        Collections.reverse(path);

        return path;
    }

    /**
     * Retrieves all descendant accounts (children, grandchildren, etc.) of a parent account.
     *
     * @param parentId the UUID of the parent account
     * @return a list of all descendant accounts
     */
    public List<GLAccount> getAllDescendants(UUID parentId) {
        logger.debug("Getting all descendants for parent: {}", parentId);
        return glAccountRepository.findAllDescendants(parentId.toString());
    }

    /**
     * Retrieves all leaf accounts (accounts with no children) under a parent.
     *
     * @param parentId the UUID of the parent account (null for all leaf accounts)
     * @return a list of leaf accounts
     */
    public List<GLAccount> getLeafAccounts(UUID parentId) {
        logger.debug("Getting leaf accounts for parent: {}", parentId);

        if (parentId == null) {
            // Return all leaf accounts in the system
            return glAccountRepository.findAllPostableAccounts();
        }

        // Get all descendants and filter for leaf accounts
        List<GLAccount> descendants = getAllDescendants(parentId);
        return descendants.stream().filter(account -> !account.hasChildren()).toList();
    }

    /**
     * Validates the entire account hierarchy for consistency and integrity.
     *
     * @return a validation result with any issues found
     */
    public AccountsHierarchyValidationResult validateAccountHierarchy() {
        logger.info("Validating account hierarchy");

        List<String> issues = new ArrayList<>();
        List<UUID> problematicAccounts = new ArrayList<>();

        List<GLAccount> allAccounts = glAccountRepository.findAll();

        for (GLAccount account : allAccounts) {
            // Check for circular references
            if (hasCircularReference(account)) {
                issues.add(String.format("Circular reference detected for account: %s", account.getCode()));
                problematicAccounts.add(account.getId());
            }

            // Check depth limits
            int depth = getAccountDepth(account.getId());
            if (depth > maxHierarchyDepth) {
                issues.add(
                        String.format(
                                "Account exceeds maximum depth (%d): %s (depth: %d)",
                                maxHierarchyDepth,
                                account.getCode(),
                                depth));
                problematicAccounts.add(account.getId());
            }

            // Check parent-child type consistency
            if (account.getParent() != null) {
                GLAccountType parentType = account.getParent().getType();
                GLAccountType childType = account.getType();

                if (!isValidParentChildTypeRelation(parentType, childType)) {
                    issues.add(
                            String.format(
                                    "Invalid parent-child type relation: %s (%s) -> %s (%s)",
                                    account.getParent().getCode(),
                                    parentType,
                                    account.getCode(),
                                    childType));
                    problematicAccounts.add(account.getId());
                }
            }
        }

        return new AccountsHierarchyValidationResult(issues.isEmpty(), issues, problematicAccounts);
    }

    /**
     * Moves an account to a new parent in the hierarchy.
     *
     * @param accountId the UUID of the account to move
     * @param newParentId the UUID of the new parent account (null for root level)
     * @param movedBy the user performing the move
     * @return the updated account
     * @throws IllegalArgumentException if validation fails
     */
    @CacheEvict(value = "glAccounts", allEntries = true)
    public GLAccount moveAccount(UUID accountId, UUID newParentId, String movedBy) {
        logger.info("Moving account: {} to parent: {} by {}", accountId, newParentId, movedBy);

        // Validate the move
        ValidationResult validation = validateAccountMove(accountId, newParentId);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(
                    "Account move validation failed: " + String.join(", ", validation.getErrors()));
        }

        Optional<GLAccount> accountOpt = glAccountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            throw new ResourceNotFoundException("GLAccount", accountId);
        }

        GLAccount account = accountOpt.get();

        // Set new parent
        if (newParentId == null) {
            account.setParent(null);
        } else {
            Optional<GLAccount> newParentOpt = glAccountRepository.findById(newParentId);
            if (newParentOpt.isEmpty()) {
                throw new ResourceNotFoundException("GLAccount", newParentId);
            }
            account.setParent(newParentOpt.get());
        }

        account.setUpdatedBy(movedBy);
        GLAccount updatedAccount = glAccountRepository.save(account);
        logger.info("Successfully moved account: {} to new parent: {}", accountId, newParentId);

        return updatedAccount;
    }

    /**
     * Validates if an account can be moved to a new parent.
     *
     * @param accountId the UUID of the account to move
     * @param newParentId the UUID of the new parent account (null for root level)
     * @return a validation result indicating if the move is allowed
     */
    public ValidationResult validateAccountMove(UUID accountId, UUID newParentId) {
        logger.debug("Validating account move for: {} to parent: {}", accountId, newParentId);

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Optional<GLAccount> accountOpt = glAccountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            errors.add(String.format("Account not found: %s", accountId));
            return new ValidationResult(false, errors, warnings);
        }

        GLAccount account = accountOpt.get();

        // If moving to root level
        if (newParentId == null) {
            return new ValidationResult(true, errors, warnings);
        }

        Optional<GLAccount> newParentOpt = glAccountRepository.findById(newParentId);
        if (newParentOpt.isEmpty()) {
            errors.add(String.format("New parent account not found: %s", newParentId));
            return new ValidationResult(false, errors, warnings);
        }

        GLAccount newParent = newParentOpt.get();

        // Check if new parent is active
        if (!newParent.isActive()) {
            errors.add(String.format("Cannot move account under inactive parent: %s", newParent.getCode()));
        }

        // Check if account is trying to move under one of its descendants
        List<GLAccount> descendants = getAllDescendants(accountId);
        if (descendants.stream().anyMatch(desc -> desc.getId().equals(newParentId))) {
            errors.add("Cannot move account under its own descendant");
        }

        // Check type compatibility
        if (!isValidParentChildTypeRelation(newParent.getType(), account.getType())) {
            warnings.add(
                    String.format(
                            "Account type %s may not be appropriate under parent type %s",
                            account.getType(),
                            newParent.getType()));
        }

        // Check depth after move
        int newDepth = getAccountDepth(newParentId) + 1;
        if (newDepth > maxHierarchyDepth) {
            errors.add(String.format("Move would exceed maximum hierarchy depth (%d)", maxHierarchyDepth));
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Calculates the depth level of an account in the hierarchy (root = 0).
     *
     * @param accountId the UUID of the account
     * @return the depth level of the account
     * @throws IllegalArgumentException if the account is not found
     */
    public int getAccountDepth(UUID accountId) {
        logger.debug("Getting account depth for: {}", accountId);

        Optional<GLAccount> accountOpt = glAccountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            throw new ResourceNotFoundException("GLAccount", accountId);
        }

        int depth = 0;
        GLAccount current = accountOpt.get();

        while (current.getParent() != null) {
            depth++;
            current = current.getParent();
        }

        return depth;
    }

    /**
     * Validates account hierarchy rules for a specific account.
     *
     * @param accountId the UUID of the account to validate
     * @return a validation result with any rule violations
     */
    public ValidationResult validateAccountHierarchyRules(UUID accountId) {
        logger.debug("Validating hierarchy rules for account: {}", accountId);

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Optional<GLAccount> accountOpt = glAccountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            errors.add(String.format("Account not found: %s", accountId));
            return new ValidationResult(false, errors, warnings);
        }

        GLAccount account = accountOpt.get();

        // Check maximum depth
        int depth = getAccountDepth(accountId);
        if (depth > maxHierarchyDepth) {
            errors.add(String.format("Account exceeds maximum depth (%d): %d", maxHierarchyDepth, depth));
        }

        // Check circular references
        if (hasCircularReference(account)) {
            errors.add("Circular reference detected in account hierarchy");
        }

        // Check parent-child type consistency
        if (account.getParent() != null) {
            if (!isValidParentChildTypeRelation(account.getParent().getType(), account.getType())) {
                warnings.add("Account type may not be appropriate for parent type");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Checks if an account has a circular reference in its hierarchy.
     *
     * @param account the account to check
     * @return true if a circular reference exists, false otherwise
     */
    private boolean hasCircularReference(GLAccount account) {
        List<UUID> visited = new ArrayList<>();
        GLAccount current = account;

        while (current != null) {
            if (visited.contains(current.getId())) {
                return true; // Circular reference found
            }
            visited.add(current.getId());
            current = current.getParent();
        }

        return false;
    }

    /**
     * Validates if a parent-child account type relationship is valid.
     *
     * This is a simplified validation that requires parent and child to have the same type.
     * In practice, many valid scenarios exist where types differ:
     *
     * Example scenarios where different types are valid:
     *
     * - Loan Portfolio: Parent "Loan Portfolio" (ASSET) may have children like
     *   "Loan Loss Provision" (CONTRA-ASSET/EXPENSE), "Interest Receivable" (ASSET),
     *   or "Loan Origination Fees" (REVENUE)
     *
     * - Customer Deposits: Parent "Customer Deposits" (LIABILITY) may have children like
     *   "Overdraft Fees" (REVENUE) or "Interest Expense on Deposits" (EXPENSE)
     *
     * - Investment Securities: Parent "Investment Securities" (ASSET) may have children like
     *   "Unrealized Gain/Loss" (EQUITY) or "Investment Income" (REVENUE)
     *
     * - Fixed Assets: Parent "Buildings" (ASSET) may have children like
     *   "Accumulated Depreciation" (CONTRA-ASSET) or "Depreciation Expense" (EXPENSE)
     *
     * A more sophisticated implementation would allow specific type combinations based on
     * accounting standards, such as CONTRA accounts under their primary type, or REVENUE/EXPENSE
     * children under ASSET/LIABILITY parents for related income and costs.
     *
     * @param parentType the parent account type
     * @param childType the child account type
     * @return true if the relationship is valid, false otherwise
     */
    private boolean isValidParentChildTypeRelation(GLAccountType parentType, GLAccountType childType) {
        return parentType == childType;
    }

    /**
     * Retrieves the complete chart of accounts organized by account type.
     *
     * @return a map of account type to list of accounts
     */
    public Map<GLAccountType, List<GLAccount>> getChartOfAccountsByType() {
        logger.debug("Getting chart of accounts by type");

        Map<GLAccountType, List<GLAccount>> chartByType = new HashMap<>();

        for (GLAccountType type : GLAccountType.values()) {
            List<GLAccount> accountsOfType = glAccountRepository.findByType(type);
            chartByType.put(type, accountsOfType);
        }

        return chartByType;
    }

    /**
     * Filters accounts for chart-of-accounts browsing. All parameters are optional.
     *
     * @param type       account type filter, {@code null} = any
     * @param status     account status filter, {@code null} = any
     * @param currency   currency code filter, {@code null} = any
     * @param searchTerm case-insensitive substring on name or code, {@code null} = any
     * @param pageable   pagination and sort
     * @return page of matching accounts
     */
    @Transactional(readOnly = true)
    public Page<GLAccount> filterAccounts(GLAccountType type, GLAccountStatus status, String currency,
            String searchTerm, Pageable pageable) {
        logger.debug(
                "Filtering accounts: type={}, status={}, currency={}, search='{}'",
                type,
                status,
                currency,
                searchTerm);

        String pattern = toLikePattern(searchTerm);

        // Fast path: no filters and no search — the dominant UI case (paginated chart-of-accounts
        // grid). Skip the optional-filter query and let Spring Data emit the plain paged SELECT,
        // which is simpler for Postgres to plan and benefits from second-level cache.
        if (type == null && status == null && currency == null && pattern == null) {
            return glAccountRepository.findAll(pageable);
        }

        return glAccountRepository.filterAccounts(type, status, currency, pattern, pageable);
    }

    /**
     * Normalises a free-text search term into an upper-cased LIKE pattern suitable for the
     * {@code UPPER(column) LIKE :pattern} queries on {@link GLAccountRepository}. Returns
     * {@code null} when there is nothing to search for, so the SQL clause short-circuits.
     */
    private static String toLikePattern(String searchTerm) {
        if (searchTerm == null) {
            return null;
        }
        String trimmed = searchTerm.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return "%" + trimmed.toUpperCase() + "%";
    }

    /**
     * Retrieves accounts by type with optional status filter.
     *
     * @param accountType the account type to filter by
     * @param status optional status filter
     * @return a list of accounts matching the criteria
     */
    public List<GLAccount> getAccountsByType(GLAccountType accountType, GLAccountStatus status) {
        logger.debug("Getting accounts by type: {} with status filter: {}", accountType, status);
        if (status != null) {
            return glAccountRepository.findByTypeAndStatus(accountType, status);
        } else {
            return glAccountRepository.findByType(accountType);
        }
    }

    /**
     * Searches accounts by name or code using case-insensitive matching.
     *
     * @param searchTerm the search term to match against account name or code
     * @return a list of matching accounts
     */
    public List<GLAccount> searchAccounts(String searchTerm) {
        logger.debug("Searching accounts with term: {}", searchTerm);

        String pattern = toLikePattern(searchTerm);
        if (pattern == null) {
            return Collections.emptyList();
        }

        return glAccountRepository.searchByNameOrCode(pattern, Pageable.unpaged()).getContent();
    }

    /**
     * Retrieves accounts filtered by currency.
     *
     * @param currency the currency code to filter by
     * @return a list of accounts in the specified currency
     */
    public List<GLAccount> getAccountsByCurrency(String currency) {
        logger.debug("Getting accounts by currency: {}", currency);
        return glAccountRepository.findByCurrencyOrderByCode(currency, Pageable.unpaged()).getContent();
    }

    /**
     * Creates a standard chart of accounts template for a given currency.
     * This method creates a basic set of accounts commonly used in banking.
     *
     * @param currency the base currency for the chart
     * @param createdBy the user creating the chart
     * @return the number of accounts created
     */
    public int createStandardChartOfAccounts(String currency, String createdBy) {
        logger.info("Creating standard chart of accounts for currency: {} by {}", currency, createdBy);

        // Get standard template and leverage import logic for persistence
        ChartOfAccountsImport chartImport = getStandardAccountsTemplate(currency);
        ChartOfAccountsImportResult result = importChartOfAccounts(chartImport, createdBy);

        logger.info("Created {} standard accounts for currency: {}", result.getSuccessfulImports(), currency);
        return result.getSuccessfulImports();
    }

    /**
     * Exports the chart of accounts to a structured format.
     *
     * @return a chart of accounts export containing all account data
     */
    public ChartOfAccountsExport exportChartOfAccounts() {
        logger.info("Exporting chart of accounts");

        List<GLAccount> allAccounts = glAccountRepository.findAll();
        List<AccountExportData> exportData = new ArrayList<>();

        for (GLAccount account : allAccounts) {
            AccountExportData data = new AccountExportData();
            data.setCode(account.getCode());
            data.setName(account.getName());
            data.setType(account.getType());
            data.setCurrency(account.getCurrency());
            data.setParentCode(account.getParent() != null ? account.getParent().getCode() : null);
            data.setStatus(account.getStatus());
            data.setNormalBalance(account.getNormalBalance());
            data.setDescription(account.getDescription());

            exportData.add(data);
        }

        ChartOfAccountsExport export = new ChartOfAccountsExport();
        export.setExportedAt(dateTimeService.now());
        export.setAccounts(exportData);
        export.setExportFormat("JSON");

        logger.info("Exported {} accounts to chart of accounts", exportData.size());
        return export;
    }

    /**
     * Imports accounts from a structured format with validation and error handling.
     *
     * @param chartImport the chart of accounts import data
     * @param importedBy the user performing the import
     * @return an import result with statistics and any errors
     */
    @CacheEvict(value = "glAccounts", allEntries = true)
    public ChartOfAccountsImportResult importChartOfAccounts(ChartOfAccountsImport chartImport, String importedBy) {
        logger.info("Importing chart of accounts by {}", importedBy);

        ChartOfAccountsImportResult result = new ChartOfAccountsImportResult();
        result.setImportedAt(dateTimeService.now());
        result.setTotalRecords(chartImport.getAccounts().size());
        result.setErrors(new ArrayList<>());

        int successful = 0;
        int failed = 0;

        for (AccountExportData accountData : chartImport.getAccounts()) {
            try {
                // Check if account exists
                boolean exists = glAccountRepository.existsByCode(accountData.getCode());

                if (exists && !chartImport.isOverwriteExisting()) {
                    result.getErrors()
                            .add("Account already exists and overwrite not allowed: " + accountData.getCode());
                    failed++;
                    continue;
                }

                GLAccount account;
                if (exists) {
                    // Update existing account
                    Optional<GLAccount> existingOpt = glAccountRepository.findByCode(accountData.getCode());
                    account = existingOpt.get();
                } else {
                    // Create new account
                    account = new GLAccount(
                            accountData.getCode(),
                            accountData.getName(),
                            accountData.getType(),
                            accountData.getCurrency(),
                            importedBy);
                }

                // Set account properties
                account.setName(accountData.getName());
                account.setType(accountData.getType());
                account.setCurrency(accountData.getCurrency());
                account.setStatus(accountData.getStatus());
                account.setDescription(accountData.getDescription());

                // Set parent if specified
                if (accountData.getParentCode() != null) {
                    Optional<GLAccount> parentOpt = glAccountRepository.findByCode(accountData.getParentCode());
                    if (parentOpt.isPresent()) {
                        account.setParent(parentOpt.get());
                    } else {
                        result.getErrors().add(
                                "Parent account not found for: " + accountData.getCode() + " (parent: "
                                        + accountData.getParentCode() + ")");
                        failed++;
                        continue;
                    }
                }

                glAccountRepository.save(account);
                successful++;

            } catch (Exception e) {
                result.getErrors().add("Error importing account " + accountData.getCode() + ": " + e.getMessage());
                failed++;
            }
        }

        result.setSuccessfulImports(successful);
        result.setFailedImports(failed);

        logger.info("Import completed: {} successful, {} failed", successful, failed);
        return result;
    }

    /**
     * Retrieves account usage statistics for a specified date range.
     *
     * @param fromDate the start date for statistics
     * @param toDate the end date for statistics
     * @return a map of account ID to usage statistics
     */
    public Map<UUID, AccountUsageStatistics> getAccountUsageStatistics(LocalDate fromDate, LocalDate toDate) {
        logger.debug("Getting account usage statistics from {} to {}", fromDate, toDate);

        Map<UUID, AccountUsageStatistics> statistics = new HashMap<>();
        List<GLAccount> allAccounts = glAccountRepository.findAll();

        for (GLAccount account : allAccounts) {
            AccountUsageStatistics stats = new AccountUsageStatistics();
            stats.setAccountId(account.getId());

            // Get balance history for the period
            List<DailyBalanceSnapshot> balances = balanceService.getBalanceHistory(account.getId(), fromDate, toDate);

            if (!balances.isEmpty()) {
                int totalTransactions = balances.stream().mapToInt(DailyBalanceSnapshot::getTransactionCount).sum();

                BigDecimal totalActivity = balances.stream()
                        .map(balance -> balance.getTotalDebits().add(balance.getTotalCredits()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                LocalDate lastActivity = balances.stream().filter(balance -> balance.getTransactionCount() > 0)
                        .map(DailyBalanceSnapshot::getBalanceDate).max(LocalDate::compareTo).orElse(null);

                BigDecimal averageBalance = balances.stream().map(DailyBalanceSnapshot::getClosingBalance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(balances.size()), RoundingMode.HALF_UP);

                stats.setTransactionCount(totalTransactions);
                stats.setTotalActivity(totalActivity);
                stats.setLastActivityDate(lastActivity);
                stats.setAverageDailyBalance(averageBalance);
            } else {
                // No activity in the period
                stats.setTransactionCount(0);
                stats.setTotalActivity(BigDecimal.ZERO);
                stats.setLastActivityDate(null);
                stats.setAverageDailyBalance(BigDecimal.ZERO);
            }

            statistics.put(account.getId(), stats);
        }

        return statistics;
    }

    /**
     * Archives inactive accounts that haven't been used for a specified period.
     *
     * @param inactiveMonths the number of months of inactivity required for archival
     * @param archivedBy the user performing the archival
     * @return the number of accounts archived
     */
    @CacheEvict(value = "glAccounts", allEntries = true)
    public int archiveInactiveAccounts(int inactiveMonths, String archivedBy) {
        logger.info("Archiving inactive accounts (inactive for {} months) by {}", inactiveMonths, archivedBy);

        LocalDate cutoffDate = dateTimeService.today().minusMonths(inactiveMonths);
        long cutoffDays = inactiveMonths * 30L; // Approximate days for simple comparison
        int archivedCount = 0;

        List<GLAccount> activeAccounts = glAccountRepository.findByStatus(GLAccountStatus.ACTIVE);

        for (GLAccount account : activeAccounts) {
            // Check if account has any recent activity
            Optional<GLDailyBalance> latestBalance = balanceService.getAccountLatestDailyBalance(account.getId());

            boolean shouldArchive = false;

            if (latestBalance.isEmpty()) {
                // No balance history - archive if account is old enough (simple day comparison)
                long daysSinceCreation = ChronoUnit.DAYS.between(account.getCreatedAt(), dateTimeService.now());
                if (daysSinceCreation >= cutoffDays) {
                    shouldArchive = true;
                }
            } else {
                // Check if last activity was before cutoff date
                GLDailyBalance balance = latestBalance.get();
                if (balance.getBalanceDate().isBefore(cutoffDate) && balance.getTransactionCount() == 0) {
                    shouldArchive = true;
                }
            }

            if (shouldArchive && !account.hasChildren()) {
                // Only archive leaf accounts without children
                account.setStatus(GLAccountStatus.INACTIVE);
                glAccountRepository.save(account);
                archivedCount++;
            }
        }

        logger.info("Archived {} inactive accounts", archivedCount);
        return archivedCount;
    }

    /**
     * Retrieves the standard chart of accounts template from the unified template definition.
     * This delegates to StandardBankTemplateDefinition which serves as the single source of truth
     * for all GL account definitions, including operational account mappings.
     *
     * @param currency the base currency for the chart
     * @return the standard chart of accounts for the specified currency
     */
    private ChartOfAccountsImport getStandardAccountsTemplate(String currency) {
        return StandardBankTemplateDefinition.getStandardTemplate(currency);
    }

}
