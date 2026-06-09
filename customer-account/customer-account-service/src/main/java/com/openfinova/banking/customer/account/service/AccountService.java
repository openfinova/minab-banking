package com.openfinova.banking.customer.account.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.customer.account.api.dto.AccountBalanceView;
import com.openfinova.banking.customer.account.api.dto.AccountStatement;
import com.openfinova.banking.customer.account.api.dto.AddBeneficiaryRequest;
import com.openfinova.banking.customer.account.api.dto.BalanceUpdate;
import com.openfinova.banking.customer.account.api.dto.StatementPeriod;
import com.openfinova.banking.customer.account.api.dto.StatementSummary;
import com.openfinova.banking.customer.account.api.dto.ValidationResult;
import com.openfinova.banking.customer.account.api.entity.AccountPermission;
import com.openfinova.banking.customer.account.api.entity.AccountProductType;
import com.openfinova.banking.customer.account.api.entity.AccountStatus;
import com.openfinova.banking.customer.account.api.entity.AccountTransactionType;
import com.openfinova.banking.customer.account.api.entity.LimitType;
import com.openfinova.banking.customer.account.api.entity.RelationshipStatus;
import com.openfinova.banking.customer.account.api.entity.RelationshipType;
import com.openfinova.banking.customer.account.entity.Account;
import com.openfinova.banking.customer.account.entity.AccountHold;
import com.openfinova.banking.customer.account.entity.AccountLimit;
import com.openfinova.banking.customer.account.entity.AccountRelationship;
import com.openfinova.banking.customer.account.entity.AccountSearchCriteria;
import com.openfinova.banking.customer.account.entity.AccountTransaction;
import com.openfinova.banking.customer.account.entity.InterestRate;
import com.openfinova.banking.customer.account.repository.AccountLimitRepository;
import com.openfinova.banking.customer.account.repository.AccountRelationshipRepository;
import com.openfinova.banking.customer.account.repository.AccountRepository;
import com.openfinova.banking.customer.account.repository.AccountTransactionRepository;
import com.openfinova.banking.customer.account.repository.InterestRateRepository;
import com.openfinova.banking.identity.api.principal.CallerContextResolver;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Enhanced AccountService implementation with comprehensive business rules
 * for account closure validation, dormancy detection, and hold/limit management.
 */
@Service
@Transactional
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository accountTransactionRepository;
    private final AccountLimitRepository accountLimitRepository;
    private final AccountRelationshipRepository accountRelationshipRepository;
    private final InterestRateRepository interestRateRepository;
    private final AccountHoldService accountHoldService;
    private final AccountLimitService accountLimitService;
    private final DateTimeService dateTimeService;

    /**
     * Constructs a new AccountService with required dependencies.
     *
     * @param accountRepository the repository for managing account entities
     * @param accountTransactionRepository the repository for transaction entities
     * @param accountLimitRepository the repository for limit entities
     * @param accountRelationshipRepository the repository for relationship entities
     * @param interestRateRepository the repository for interest rate entities
     * @param accountHoldService the service for managing account holds
     * @param accountLimitService the service for managing account limits
     * @param dateTimeService centralized clock and calendar operations
     */
    public AccountService(AccountRepository accountRepository,
            AccountTransactionRepository accountTransactionRepository, AccountLimitRepository accountLimitRepository,
            AccountRelationshipRepository accountRelationshipRepository, InterestRateRepository interestRateRepository,
            AccountHoldService accountHoldService, AccountLimitService accountLimitService,
            DateTimeService dateTimeService) {
        this.accountRepository = accountRepository;
        this.accountTransactionRepository = accountTransactionRepository;
        this.accountLimitRepository = accountLimitRepository;
        this.accountRelationshipRepository = accountRelationshipRepository;
        this.interestRateRepository = interestRateRepository;
        this.accountHoldService = accountHoldService;
        this.accountLimitService = accountLimitService;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Creates a new customer account and a {@link RelationshipType#PRIMARY_HOLDER} relationship for
     * {@code primaryUserProfileId} so holder lists stay aligned with the account record.
     *
     * @param primaryUserProfileId the unique identifier of the primary user
     * @param productType the product type of the account
     * @param currency the currency of the account
     * @param accountNumber the unique account number assigned by the caller
     * @param createdBy the user or system creating the account
     * @return the newly created account entity
     * @throws IllegalArgumentException if {@code accountNumber} is already in use
     */
    public Account createAccount(UUID primaryUserProfileId, AccountProductType productType, String currency,
            String accountNumber, String createdBy) {
        logger.info(
                "Creating account for user: {} with product type: {} and number: {}",
                primaryUserProfileId,
                productType,
                accountNumber);

        if (accountNumber != null && accountRepository.findByAccountNumber(accountNumber).isPresent()) {
            throw new IllegalArgumentException("Account number already in use: " + accountNumber);
        }

        Account account = new Account(primaryUserProfileId, productType, createdBy);
        account.setCurrency(currency);
        account.setDisplayName(productType.getDisplayName() + " Account");
        account.setAccountNumber(accountNumber);

        Account savedAccount = accountRepository.save(account);
        logger.info("Account created successfully: {}", savedAccount.getAccountNumber());

        AccountRelationship primaryHolder = new AccountRelationship(
                savedAccount,
                primaryUserProfileId,
                RelationshipType.PRIMARY_HOLDER,
                dateTimeService.now());
        primaryHolder.setStatus(RelationshipStatus.ACTIVE);
        accountRelationshipRepository.save(primaryHolder);
        logger.info(
                "Created PRIMARY_HOLDER relationship for account {} and user profile {}",
                savedAccount.getId(),
                primaryUserProfileId);

        return savedAccount;
    }

    /**
     * Retrieves an account by its unique identifier.
     *
     * @param id the unique identifier of the account
     * @return an Optional containing the account, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<Account> getAccountById(UUID id) {
        return accountRepository.findById(id);
    }

    /**
     * Retrieves an account by its IBAN.
     *
     * @param iban the IBAN of the account
     * @return an Optional containing the account, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<Account> getAccountByIban(String iban) {
        return accountRepository.findByIban(iban);
    }

    /**
     * Retrieves an account by its account number.
     *
     * @param accountNumber the account number
     * @return an Optional containing the account, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<Account> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    /**
     * Retrieves all accounts where the authenticated user is the primary holder.
     * The user profile id is taken from the JWT subject — never from caller input.
     */
    @PreAuthorize("hasAuthority('account:read:own')")
    @Transactional(readOnly = true)
    public List<Account> getMyAccounts() {
        UUID userProfileId = CallerContextResolver.requireCurrentUserId();
        return accountRepository.findByPrimaryUserProfileId(userProfileId);
    }

    /**
     * Updates the status of an existing account.
     *
     * @param id the unique identifier of the account
     * @param newStatus the new status to apply
     * @param reason the reason for the status change
     * @param changedBy the user or system initiating the change
     * @throws IllegalArgumentException if the account is not found
     */
    public void updateAccountStatus(UUID id, AccountStatus newStatus, String reason, String changedBy) {
        logger.info("Updating account status for account: {} to: {}", id, newStatus);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));

        account.changeStatus(newStatus, reason, changedBy, dateTimeService.now());
        accountRepository.save(account);

        logger.info("Account status updated successfully for account: {}", id);
    }

    /**
     * Updates the ledger and available balances of an account.
     *
     * @param id the unique identifier of the account
     * @param ledgerDelta the amount to add to the ledger balance
     * @param availableDelta the amount to add to the available balance
     * @throws IllegalArgumentException if the account is not found
     */
    public void updateBalances(UUID id, BigDecimal ledgerDelta, BigDecimal availableDelta) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));

        account.setLedgerBalance(account.getLedgerBalance().add(ledgerDelta));
        account.setAvailableBalance(account.getAvailableBalance().add(availableDelta));

        accountRepository.save(account);
    }

    /**
     * Closes an active account after validating business rules.
     *
     * @param id the unique identifier of the account
     * @param reason the reason for closure
     * @throws ResourceNotFoundException if the account is not found
     * @throws IllegalStateException if the account cannot be closed
     */
    public void closeAccount(UUID id, String reason) throws IllegalStateException {
        logger.info("Closing account: {} with reason: {}", id, reason);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id.toString()));

        // Perform comprehensive closure validation
        ValidationResult validationResult = validateForClosure(id);
        if (!validationResult.isValid()) {
            throw new IllegalStateException("Account cannot be closed: " + validationResult.getErrorMessage());
        }

        // Close the account
        account.changeStatus(AccountStatus.CLOSED, reason, "SYSTEM", dateTimeService.now());

        // Release any active holds
        releaseAllActiveHolds(account);

        // Expire all active limits
        expireAllActiveLimits(account);

        accountRepository.save(account);
        logger.info("Account closed successfully: {}", id);
    }

    /**
     * Validates whether an account can accept a transaction of a specific amount.
     *
     * @param accountId the unique identifier of the account
     * @param amount the transaction amount
     * @return a ValidationResult indicating whether the transaction is allowed
     */
    @Transactional(readOnly = true)
    public ValidationResult validateAccountForTransaction(UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId).orElse(null);

        if (account == null) {
            return ValidationResult.invalid("Account not found");
        }

        if (!account.canTransact()) {
            return ValidationResult.invalid("Account status does not allow transactions: " + account.getStatus());
        }

        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            if (account.getAvailableBalance().compareTo(amount) < 0) {
                return ValidationResult.invalid("Insufficient available balance");
            }

            // Check account limits
            ValidationResult limitValidation = validateAgainstLimits(account, amount);
            if (!limitValidation.isValid()) {
                return limitValidation;
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Checks if an account is currently active.
     *
     * @param accountId the unique identifier of the account
     * @return true if active, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isAccountActive(UUID accountId) {
        return accountRepository.findById(accountId).map(Account::isActive).orElse(false);
    }

    /**
     * Validates an account against all rules required for closure.
     *
     * @param accountId the unique identifier of the account
     * @return a ValidationResult indicating whether the account can be closed
     */
    @Transactional(readOnly = true)
    public ValidationResult validateForClosure(UUID accountId) {
        Account account = accountRepository.findById(accountId).orElse(null);

        if (account == null) {
            return ValidationResult.invalid("Account not found");
        }

        List<String> validationErrors = new ArrayList<>();

        // Check if account is already closed
        if (account.getStatus() == AccountStatus.CLOSED) {
            validationErrors.add("Account is already closed");
        }

        // Check for non-zero balance
        if (account.getLedgerBalance().compareTo(BigDecimal.ZERO) != 0) {
            validationErrors.add("Account has non-zero balance: " + account.getLedgerBalance());
        }

        // Check for active holds
        List<AccountHold> activeHolds = accountHoldService.getActiveHoldsByAccount(accountId);
        if (!activeHolds.isEmpty()) {
            validationErrors.add("Account has " + activeHolds.size() + " active holds");
        }

        // Check for pending transactions
        long pendingTransactionCount = accountTransactionRepository.countPendingTransactionsByAccount(accountId);
        if (pendingTransactionCount > 0) {
            validationErrors.add("Account has " + pendingTransactionCount + " pending transactions");
        }

        // Check for active relationships (other than primary owner)
        List<AccountRelationship> activeRelationships = accountRelationshipRepository
                .findActiveRelationshipsByAccount(accountId);
        long nonOwnerRelationships = activeRelationships.stream()
                .filter(rel -> rel.getRelationshipType() != RelationshipType.PRIMARY_HOLDER).count();

        if (nonOwnerRelationships > 0) {
            validationErrors.add("Account has " + nonOwnerRelationships + " active non-owner relationships");
        }

        if (validationErrors.isEmpty()) {
            return ValidationResult.valid();
        } else {
            return ValidationResult.invalid(String.join("; ", validationErrors));
        }
    }

    /**
     * Detects and marks accounts as dormant based on inactivity.
     *
     * @param inactivityMonths the number of months of inactivity before dormancy
     * @return the number of accounts marked as dormant
     */
    public int processDormancyDetection(int inactivityMonths) {
        logger.info("Processing dormancy detection for accounts inactive for {} months", inactivityMonths);

        Instant cutoffInstant = dateTimeService.nowZoned().minusMonths(inactivityMonths).toInstant();

        // Find accounts that are active but haven't had transactions since cutoff date
        List<Account> candidateAccounts = accountRepository.findAccountsForDormancyCheck(cutoffInstant);

        int dormantCount = 0;

        for (Account account : candidateAccounts) {
            // Additional checks for dormancy
            if (shouldMarkAsDormant(account, cutoffInstant)) {
                try {
                    account.changeStatus(
                            AccountStatus.DORMANT,
                            "Account marked as dormant due to " + inactivityMonths + " months of inactivity",
                            "SYSTEM",
                            dateTimeService.now());
                    accountRepository.save(account);
                    dormantCount++;

                    logger.info("Marked account as dormant: {}", account.getAccountNumber());
                } catch (Exception e) {
                    logger.error("Failed to mark account as dormant: {}", account.getAccountNumber(), e);
                }
            }
        }

        logger.info("Processed dormancy detection: {} accounts marked as dormant", dormantCount);
        return dormantCount;
    }

    /**
     * Retrieves accounts filtered by product type.
     *
     * @param productType the product type
     * @return a list of matching accounts
     */
    @Transactional(readOnly = true)
    public List<Account> getAccountsOfType(AccountProductType productType) {
        return accountRepository.findByProductType(productType);
    }

    /**
     * Retrieves accounts filtered by status.
     *
     * @param status the account status
     * @return a list of matching accounts
     */
    @Transactional(readOnly = true)
    public List<Account> getAccountsByStatus(AccountStatus status) {
        return accountRepository.findByStatus(status);
    }

    /**
     * Retrieves accounts for a user filtered by product type.
     *
     * @param primaryUserProfileId the unique identifier of the primary user
     * @param productType the product type
     * @return a list of matching accounts
     */
    @Transactional(readOnly = true)
    public List<Account> getCustomerAccountsOfProductType(UUID primaryUserProfileId, AccountProductType productType) {
        return accountRepository.findByPrimaryUserProfileIdAndProductType(primaryUserProfileId, productType);
    }

    /**
     * Retrieves accounts for a user filtered by status.
     *
     * @param primaryUserProfileId the unique identifier of the primary user
     * @param status the account status
     * @return a list of matching accounts
     */
    @Transactional(readOnly = true)
    public List<Account> getCustomerAccountsByStatus(UUID primaryUserProfileId, AccountStatus status) {
        return accountRepository.findByPrimaryUserProfileIdAndStatus(primaryUserProfileId, status);
    }

    /**
     * Searches for accounts using complex criteria and pagination.
     *
     * @param criteria the search criteria
     * @param pageable the pagination parameters
     * @return a paginated result of accounts
     */
    @Transactional(readOnly = true)
    public Page<Account> findAccountsWithFilters(AccountSearchCriteria criteria, Pageable pageable) {
        return accountRepository.findAccountsWithCriteria(criteria, pageable);
    }

    /**
     * Processes a batch of account status updates.
     *
     * @param accountIds the list of account identifiers
     * @param newStatus the new status to apply
     * @param reason the reason for the update
     * @param changedBy the user or system making the change
     * @return a map of account IDs to operation results
     */
    public Map<UUID, String> batchUpdateAccountStatus(List<UUID> accountIds, AccountStatus newStatus, String reason,
            String changedBy) {
        logger.info("Batch updating status for {} accounts to: {}", accountIds.size(), newStatus);

        Map<UUID, String> results = new HashMap<>();

        for (UUID accountId : accountIds) {
            try {
                updateAccountStatus(accountId, newStatus, reason, changedBy);
                results.put(accountId, "SUCCESS");
            } catch (Exception e) {
                logger.error("Failed to update status for account: {}", accountId, e);
                results.put(accountId, "FAILED: " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Processes a batch of account closures.
     *
     * @param accountIds the list of account identifiers
     * @param reason the reason for closure
     * @param closedBy the user or system making the change
     * @return a map of account IDs to operation results
     */
    public Map<UUID, String> batchCloseAccounts(List<UUID> accountIds, String reason, String closedBy) {
        logger.info("Batch closing {} accounts", accountIds.size());

        Map<UUID, String> results = new HashMap<>();

        for (UUID accountId : accountIds) {
            try {
                closeAccount(accountId, reason);
                results.put(accountId, "SUCCESS");
            } catch (Exception e) {
                logger.error("Failed to close account: {}", accountId, e);
                results.put(accountId, "FAILED: " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Processes a batch of balance updates.
     *
     * @param balanceUpdates a map of account IDs to balance update details
     * @return a map of account IDs to operation results
     */
    public Map<UUID, String> batchUpdateBalances(Map<UUID, BalanceUpdate> balanceUpdates) {
        logger.info("Batch updating balances for {} accounts", balanceUpdates.size());

        Map<UUID, String> results = new HashMap<>();

        for (Map.Entry<UUID, BalanceUpdate> entry : balanceUpdates.entrySet()) {
            UUID accountId = entry.getKey();
            BalanceUpdate update = entry.getValue();

            try {
                updateBalances(accountId, update.getLedgerDelta(), update.getAvailableDelta());
                results.put(accountId, "SUCCESS");
            } catch (Exception e) {
                logger.error("Failed to update balance for account: {}", accountId, e);
                results.put(accountId, "FAILED: " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Adds a new relationship between an account and a user profile.
     *
     * @param accountId the unique identifier of the account
     * @param userProfileId the unique identifier of the user profile
     * @param relationshipType the type of relationship
     * @param createdBy the user or system making the change
     * @return the new relationship entity
     * @throws IllegalArgumentException if the account is not found
     */
    public AccountRelationship addAccountRelationship(UUID accountId, UUID userProfileId,
            RelationshipType relationshipType, String createdBy) {
        logger.info(
                "Adding relationship for account: {} user: {} type: {}",
                accountId,
                userProfileId,
                relationshipType);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        AccountRelationship relationship = new AccountRelationship();
        relationship.setCustomerAccount(account);
        relationship.setUserProfileId(userProfileId);
        relationship.setRelationshipType(relationshipType);
        relationship.setStatus(RelationshipStatus.ACTIVE);
        relationship.setEffectiveFrom(dateTimeService.now());

        AccountRelationship savedRelationship = accountRelationshipRepository.save(relationship);
        logger.info("Account relationship created: {}", savedRelationship.getId());
        return savedRelationship;
    }

    /**
     * Removes an active relationship between an account and a user profile.
     *
     * @param accountId the unique identifier of the account
     * @param userProfileId the unique identifier of the user profile
     * @param removedBy the user or system making the change
     */
    public void removeAccountRelationship(UUID accountId, UUID userProfileId, String removedBy) {
        logger.info("Removing relationship for account: {} user: {}", accountId, userProfileId);

        Optional<AccountRelationship> relationship = accountRelationshipRepository
                .findActiveRelationshipByAccountAndUser(accountId, userProfileId);

        if (relationship.isPresent()) {
            AccountRelationship rel = relationship.get();
            rel.setStatus(RelationshipStatus.INACTIVE);
            accountRelationshipRepository.save(rel);
            logger.info("Account relationship removed: {}", rel.getId());
        } else {
            logger.warn("No active relationship found for account: {} user: {}", accountId, userProfileId);
        }
    }

    /**
     * Updates the permissions for a specific relationship.
     *
     * @param relationshipId the unique identifier of the relationship
     * @param permissions the new set of permissions
     * @param updatedBy the user or system making the change
     * @return the updated relationship entity
     * @throws IllegalArgumentException if the relationship is not found
     */
    public AccountRelationship updateRelationshipPermissions(UUID relationshipId, Set<AccountPermission> permissions,
            String updatedBy) {
        AccountRelationship relationship = accountRelationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new IllegalArgumentException("Relationship not found: " + relationshipId));

        relationship.setPermissions(permissions);

        return accountRelationshipRepository.save(relationship);
    }

    /**
     * Adds a beneficiary to an account.
     *
     * @param accountId the unique identifier of the account
     * @param request the details of the beneficiary
     * @param createdBy the user or system creating the beneficiary relationship
     * @return the newly created relationship entity
     */
    public AccountRelationship addBeneficiary(UUID accountId, AddBeneficiaryRequest request, String createdBy) {
        return addAccountRelationship(accountId, request.getUserProfileId(), RelationshipType.BENEFICIARY, createdBy);
    }

    /**
     * Removes a beneficiary from an account.
     *
     * @param accountId the unique identifier of the account
     * @param userProfileId the unique identifier of the user profile
     * @param removedBy the user or system making the change
     */
    public void removeBeneficiary(UUID accountId, UUID userProfileId, String removedBy) {
        removeAccountRelationship(accountId, userProfileId, removedBy);
    }

    /**
     * Retrieves all active relationships for an account.
     *
     * @param accountId the unique identifier of the account
     * @return a list of relationships
     */
    @Transactional(readOnly = true)
    public List<AccountRelationship> getAccountRelationships(UUID accountId) {
        return accountRelationshipRepository.findActiveRelationshipsByAccount(accountId);
    }

    /**
     * Retrieves all accounts associated with a user profile.
     *
     * @param userProfileId the unique identifier of the user profile
     * @return a list of accounts
     */
    @Transactional(readOnly = true)
    public List<Account> getAccountsForUser(UUID userProfileId) {
        return accountRepository.findAccountsForUser(userProfileId);
    }

    /**
     * Checks if a user profile has a specific permission on an account.
     *
     * @param accountId the unique identifier of the account
     * @param userProfileId the unique identifier of the user profile
     * @param permission the permission to check
     * @return true if the permission is granted, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasAccountPermission(UUID accountId, UUID userProfileId, AccountPermission permission) {
        return accountRelationshipRepository.hasPermission(accountId, userProfileId, permission.name());
    }

    // Interest Calculation and Accrual Implementation

    /**
     * Sets a new interest rate for an account.
     *
     * @param accountId the unique identifier of the account
     * @param rateType the type of rate (credit or debit)
     * @param annualPercentageRate the annual percentage rate
     * @param effectiveFrom the date and time the rate becomes effective
     * @param setBy the user or system setting the rate
     * @return the new interest rate entity
     * @throws IllegalArgumentException if the account is not found
     */
    public InterestRate setInterestRate(UUID accountId, InterestRate.RateType rateType, BigDecimal annualPercentageRate,
            LocalDateTime effectiveFrom, String setBy) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        InterestRate interestRate = new InterestRate();
        interestRate.setCustomerAccount(account);
        interestRate.setRateType(rateType);
        interestRate.setAnnualPercentageRate(annualPercentageRate);
        interestRate.setEffectiveFrom(effectiveFrom);

        return interestRateRepository.save(interestRate);
    }

    /**
     * Retrieves the current active interest rate of a specific type for an account.
     *
     * @param accountId the unique identifier of the account
     * @param rateType the type of rate
     * @return an Optional containing the rate if found
     */
    @Transactional(readOnly = true)
    public Optional<InterestRate> getCurrentInterestRate(UUID accountId, InterestRate.RateType rateType) {
        return interestRateRepository.findCurrentRateByAccountAndType(accountId, rateType, dateTimeService.now());
    }

    /**
     * Calculates the accrued interest for an account over a date range.
     *
     * @param accountId the unique identifier of the account
     * @param fromDate the start date (inclusive)
     * @param toDate the end date (exclusive)
     * @return the calculated interest amount
     * @throws IllegalArgumentException if the account is not found
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateAccruedInterest(UUID accountId, LocalDate fromDate, LocalDate toDate) {
        // Implementation would calculate daily interest accrual
        // This is a simplified version
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        Optional<InterestRate> currentRate = getCurrentInterestRate(accountId, InterestRate.RateType.CREDIT);
        if (currentRate.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Simple daily interest calculation
        BigDecimal dailyRate = currentRate.get().getAnnualPercentageRate()
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
        long days = toDate.toEpochDay() - fromDate.toEpochDay();

        return account.getLedgerBalance().multiply(dailyRate).multiply(BigDecimal.valueOf(days))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Posts accrued interest to an account's balance.
     *
     * @param accountId the unique identifier of the account
     * @param interestAmount the interest amount to post
     * @param postingDate the date of posting
     * @param postedBy the user or system initiating the posting
     */
    public void postAccruedInterest(UUID accountId, BigDecimal interestAmount, LocalDate postingDate, String postedBy) {
        // This would integrate with GL posting
        updateBalances(accountId, interestAmount, interestAmount);
        logger.info("Posted accrued interest: {} for account: {}", interestAmount, accountId);
    }

    /**
     * Processes daily interest accrual for all eligible accounts.
     *
     * @param accrualDate the date for which to process accrual
     * @return the number of accounts successfully processed
     */
    public int processInterestAccrual(LocalDate accrualDate) {
        List<Account> eligibleAccounts = accountRepository.findAccountsEligibleForInterest();
        int processedCount = 0;

        for (Account account : eligibleAccounts) {
            try {
                BigDecimal interest = calculateAccruedInterest(account.getId(), accrualDate.minusDays(1), accrualDate);

                if (interest.compareTo(BigDecimal.ZERO) > 0) {
                    postAccruedInterest(account.getId(), interest, accrualDate, "SYSTEM");
                    processedCount++;
                }
            } catch (Exception e) {
                logger.error("Failed to process interest for account: {}", account.getId(), e);
            }
        }

        return processedCount;
    }

    // Statement Generation Implementation (simplified)

    /**
     * Generates a comprehensive statement for an account over a specific date range.
     *
     * @param accountId the unique identifier of the account
     * @param fromDate the start date of the statement
     * @param toDate the end date of the statement
     * @return the generated account statement
     * @throws IllegalArgumentException if the account is not found
     */
    @Transactional(readOnly = true)
    public AccountStatement generateAccountStatement(UUID accountId, LocalDate fromDate, LocalDate toDate) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        AccountStatement statement = new AccountStatement();
        statement.setAccountId(accountId);
        statement.setAccountNumber(account.getAccountNumber());
        statement.setFromDate(fromDate);
        statement.setToDate(toDate);

        List<AccountTransaction> transactions;
        try (Stream<AccountTransaction> stream = accountTransactionRepository
                .streamByAccountAndDateRange(accountId, fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay())) {
            transactions = stream.toList();
        }
        statement.setTransactions(new ArrayList<>(transactions));

        StatementSummary summary = new StatementSummary();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        Set<AccountTransactionType> debitTypes = EnumSet.of(
                AccountTransactionType.WITHDRAWAL,
                AccountTransactionType.TRANSFER_OUT,
                AccountTransactionType.FEE,
                AccountTransactionType.INTEREST_CHARGE);
        for (AccountTransaction tx : transactions) {
            if (tx.getTransactionType() != null && debitTypes.contains(tx.getTransactionType())) {
                totalDebits = totalDebits.add(tx.getAmount());
            } else {
                totalCredits = totalCredits.add(tx.getAmount());
            }
        }
        summary.setTotalDebits(totalDebits);
        summary.setTotalCredits(totalCredits);
        summary.setOpeningBalance(account.getLedgerBalance().subtract(totalCredits).add(totalDebits));
        summary.setClosingBalance(account.getLedgerBalance());
        statement.setSummary(summary);

        return statement;
    }

    /**
     * Retrieves a detailed balance view for an account.
     *
     * @param accountId the unique identifier of the account
     * @return the detailed balance view
     * @throws IllegalArgumentException if the account is not found
     */
    @Transactional(readOnly = true)
    public AccountBalanceView getDetailedBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        AccountBalanceView balanceView = new AccountBalanceView();
        balanceView.setAccountId(accountId);
        balanceView.setLedgerBalance(account.getLedgerBalance());
        balanceView.setAvailableBalance(account.getAvailableBalance());

        return balanceView;
    }

    /**
     * Generates a monthly statement for an account.
     *
     * @param accountId the unique identifier of the account
     * @param year the year of the statement
     * @param month the month of the statement (1-12)
     * @return the generated account statement
     */
    @Transactional(readOnly = true)
    public AccountStatement generateMonthlyStatement(UUID accountId, int year, int month) {
        LocalDate fromDate = LocalDate.of(year, month, 1);
        LocalDate toDate = fromDate.plusMonths(1).minusDays(1);
        return generateAccountStatement(accountId, fromDate, toDate);
    }

    /**
     * Retrieves all available statement periods for an account since its creation.
     *
     * @param accountId the unique identifier of the account
     * @return a list of statement periods
     * @throws IllegalArgumentException if the account is not found
     */
    @Transactional(readOnly = true)
    public List<StatementPeriod> getAvailableStatementPeriods(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        List<StatementPeriod> periods = new ArrayList<>();
        LocalDate start = account.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                .withDayOfMonth(1);
        LocalDate now = dateTimeService.today().withDayOfMonth(1);
        LocalDate cursor = start;
        while (!cursor.isAfter(now)) {
            periods.add(new StatementPeriod(cursor.getYear(), cursor.getMonthValue()));
            cursor = cursor.plusMonths(1);
        }
        return periods;
    }

    // Private helper methods

    private ValidationResult validateAgainstLimits(Account account, BigDecimal amount) {
        // Use the service to check daily transaction limits
        ValidationResult limitCheck = accountLimitService
                .checkLimit(account.getId(), LimitType.DAILY_TRANSACTION, amount);
        if (!limitCheck.isValid()) {
            return limitCheck;
        }

        return ValidationResult.valid();
    }

    private boolean shouldMarkAsDormant(Account account, Instant cutoffInstant) {
        // Additional business rules for dormancy
        if (account.getStatus() != AccountStatus.ACTIVE) {
            return false;
        }

        // AccountTransaction.transactionDate is LocalDateTime — same zone as DateTimeService / Clock bean
        LocalDateTime cutoffForTransactions = LocalDateTime.ofInstant(cutoffInstant, dateTimeService.clock().getZone());
        boolean hasRecentActivity = accountTransactionRepository
                .hasTransactionsSince(account.getId(), cutoffForTransactions);
        if (hasRecentActivity) {
            return false;
        }

        // Check if account has significant balance (might not want to mark high-balance accounts as dormant)
        BigDecimal significantBalanceThreshold = new BigDecimal("1000.00");
        if (account.getLedgerBalance().abs().compareTo(significantBalanceThreshold) > 0) {
            return false;
        }

        return true;
    }

    private void releaseAllActiveHolds(Account account) {
        List<AccountHold> activeHolds = accountHoldService.getActiveHoldsByAccount(account.getId());
        for (AccountHold hold : activeHolds) {
            try {
                accountHoldService.releaseHold(hold.getId());
            } catch (Exception e) {
                logger.error("Failed to release hold: {} for account: {}", hold.getId(), account.getId(), e);
            }
        }
    }

    private void expireAllActiveLimits(Account account) {
        List<AccountLimit> activeLimits = accountLimitRepository
                .findActiveEffectiveLimitsByAccount(account.getId(), dateTimeService.instant());
        for (AccountLimit limit : activeLimits) {
            try {
                limit.expire("SYSTEM");
                accountLimitRepository.save(limit);
            } catch (Exception e) {
                logger.error("Failed to expire limit: {} for account: {}", limit.getId(), account.getId(), e);
            }
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('account:read', 'service:account:read')")
    public Optional<UUID> getPrimaryUserProfileIdForAccount(UUID accountId) {
        return accountRepository.findById(accountId).map(Account::getPrimaryUserProfileId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('account:read', 'service:account:read')")
    public boolean accountExists(UUID accountId) {
        return accountRepository.existsById(accountId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('account:read', 'service:account:read')")
    public boolean isAccountEligibleForTransaction(UUID accountId) {
        return accountRepository.findById(accountId).map(Account::canTransact).orElse(false);
    }

}