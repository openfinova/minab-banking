package com.openfinova.banking.customer.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.customer.account.api.CustomerAccountService;
import com.openfinova.banking.customer.account.api.entity.AccountTransactionType;
import com.openfinova.banking.customer.account.api.entity.GLAccountMappingType;
import com.openfinova.banking.customer.account.api.entity.LimitType;
import com.openfinova.banking.customer.account.entity.Account;
import com.openfinova.banking.customer.account.entity.AccountTransaction;
import com.openfinova.banking.customer.account.repository.AccountLimitRepository;
import com.openfinova.banking.customer.account.repository.AccountRepository;
import com.openfinova.banking.customer.account.service.AccountTransactionService;
import com.openfinova.banking.customer.account.service.GLAccountMappingService;

/**
 * Facade implementation for Customer Account operations consumed by other modules.
 * Delegates to internal services while keeping the dependency graph acyclic:
 * this class depends only on {@link AccountRepository}, {@link AccountLimitRepository},
 * and {@link GLAccountMappingService}, never on {@link com.openfinova.banking.customer.account.service.AccountBalanceService}.
 */
@Service
@Transactional(readOnly = true)
public class CustomerAccountServiceImpl implements CustomerAccountService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerAccountServiceImpl.class);

    private final AccountRepository accountRepository;
    private final AccountLimitRepository accountLimitRepository;
    private final GLAccountMappingService glAccountMappingService;
    private final AccountTransactionService accountTransactionService;

    public CustomerAccountServiceImpl(AccountRepository accountRepository,
            AccountLimitRepository accountLimitRepository, GLAccountMappingService glAccountMappingService,
            AccountTransactionService accountTransactionService) {
        this.accountRepository = accountRepository;
        this.accountLimitRepository = accountLimitRepository;
        this.glAccountMappingService = glAccountMappingService;
        this.accountTransactionService = accountTransactionService;
    }

    @Override
    @PreAuthorize("hasAuthority('service:account:read')")
    public Optional<UUID> getPrimaryUserProfileIdForAccount(UUID accountId) {
        return accountRepository.findById(accountId).map(Account::getPrimaryUserProfileId);
    }

    @Override
    @PreAuthorize("hasAuthority('service:account:read')")
    public boolean accountExists(UUID accountId) {
        logger.debug("Checking account existence: {}", accountId);
        return accountRepository.existsById(accountId);
    }

    @Override
    @PreAuthorize("hasAuthority('service:account:read')")
    public boolean isAccountActive(UUID accountId) {
        logger.debug("Checking account active status: {}", accountId);
        return accountRepository.findById(accountId).map(Account::isActive).orElse(false);
    }

    @Override
    @PreAuthorize("hasAuthority('service:account:read')")
    public boolean isAccountEligibleForTransaction(UUID accountId, Object transactionType) {
        logger.debug("Checking account eligibility for transaction: {} type: {}", accountId, transactionType);
        return accountRepository.findById(accountId).map(Account::canTransact).orElse(false);
    }

    @Override
    @PreAuthorize("hasAuthority('service:account:read')")
    public boolean hasSufficientBalance(UUID accountId, BigDecimal amount) {
        logger.debug("Checking sufficient balance for account: {} amount: {}", accountId, amount);
        return accountRepository.findById(accountId).map(account -> hasSufficientFunds(account, amount)).orElse(false);
    }

    @Override
    @PreAuthorize("hasAuthority('service:account:read')")
    public boolean hasSufficientBalanceUnderLock(UUID accountId, BigDecimal amount) {
        logger.debug("Checking sufficient balance under lock for account: {} amount: {}", accountId, amount);
        return accountRepository.findByIdWithLock(accountId).map(account -> hasSufficientFunds(account, amount))
                .orElse(false);
    }

    /**
     * Sufficient funds = available balance + effective overdraft limit >= amount.
     * Uses available balance (ledger minus holds/reservations). If the account has no
     * overdraft limit, only positive available balance can be debited.
     */
    private boolean hasSufficientFunds(Account account, BigDecimal amount) {
        BigDecimal available = account.getAvailableBalance();
        BigDecimal overdraftLimit = getOverdraftLimit(account.getId());
        BigDecimal effectiveLimit = available.add(overdraftLimit);
        return effectiveLimit.compareTo(amount) >= 0;
    }

    /**
     * Returns the effective overdraft limit for the account (max of active OVERDRAFT_LIMIT maxAmount).
     * Returns zero if the account has no overdraft facility.
     */
    private BigDecimal getOverdraftLimit(UUID accountId) {
        return accountLimitRepository.findActiveEffectiveLimitsByAccount(accountId).stream()
                .filter(l -> l.getLimitType() == LimitType.OVERDRAFT_LIMIT)
                .map(l -> l.getMaxAmount() != null ? l.getMaxAmount() : BigDecimal.ZERO).max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @PreAuthorize("hasAuthority('service:account:read')")
    public UUID getGLAccountIdForType(UUID accountId, GLAccountMappingType mappingType) {
        logger.debug("Resolving GL account for account: {} type: {}", accountId, mappingType);
        return glAccountMappingService.getGLAccountIdForType(accountId, mappingType);
    }

    // ── Account Transaction Operations (Customer-Facing Memo Posts) ──────────

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('service:account:write')")
    public UUID recordAccountTransaction(UUID accountId, String transactionType, BigDecimal amount, String currency,
            LocalDateTime transactionDate, String description, String referenceId) {
        logger.info(
                "Recording account transaction for account: {} type: {} amount: {}",
                accountId,
                transactionType,
                amount);

        // Convert string to enum
        AccountTransactionType type = AccountTransactionType.valueOf(transactionType);

        // Delegate to internal service
        AccountTransaction transaction = accountTransactionService
                .recordTransaction(accountId, type, amount, currency, transactionDate, description, referenceId);

        logger.info("Account transaction recorded successfully: {}", transaction.getId());
        return transaction.getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('service:account:write')")
    public void linkAccountTransactionToGL(UUID accountTransactionId, UUID glTransactionId) {
        logger.info("Linking account transaction: {} to GL transaction: {}", accountTransactionId, glTransactionId);

        // Delegate to internal service
        accountTransactionService.updateGLTransactionLink(accountTransactionId, glTransactionId);

        logger.info("Account transaction linked to GL successfully");
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('service:account:write')")
    public UUID recordAndLinkAccountTransaction(UUID accountId, String transactionType, BigDecimal amount,
            String currency, LocalDateTime transactionDate, String description, String referenceId,
            UUID glTransactionId) {
        logger.info(
                "Recording and linking account transaction for account: {} type: {} amount: {} GL: {}",
                accountId,
                transactionType,
                amount,
                glTransactionId);

        // Step 1: Record the transaction
        UUID accountTransactionId = recordAccountTransaction(
                accountId,
                transactionType,
                amount,
                currency,
                transactionDate,
                description,
                referenceId);

        // Step 2: Link to GL
        linkAccountTransactionToGL(accountTransactionId, glTransactionId);

        logger.info("Account transaction recorded and linked successfully: {}", accountTransactionId);
        return accountTransactionId;
    }
}
