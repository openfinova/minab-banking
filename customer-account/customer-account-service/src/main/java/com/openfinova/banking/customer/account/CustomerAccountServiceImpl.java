package com.openfinova.banking.customer.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.customer.account.api.CustomerAccountService;
import com.openfinova.banking.customer.account.api.entity.AccountTransactionType;
import com.openfinova.banking.customer.account.api.entity.GLAccountMappingType;
import com.openfinova.banking.customer.account.service.AccountBalanceService;
import com.openfinova.banking.customer.account.service.AccountHoldService;
import com.openfinova.banking.customer.account.service.AccountService;
import com.openfinova.banking.customer.account.service.AccountTransactionService;
import com.openfinova.banking.customer.account.service.GLAccountMappingService;

/**
 * Facade implementation for Customer Account operations consumed by other modules.
 * Delegates to internal services while exposing only API contracts to other modules.
 */
@Service
@Transactional(readOnly = true)
public class CustomerAccountServiceImpl implements CustomerAccountService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerAccountServiceImpl.class);

    private final AccountService accountService;
    private final AccountBalanceService accountBalanceService;
    private final GLAccountMappingService glAccountMappingService;
    private final AccountTransactionService accountTransactionService;
    private final AccountHoldService accountHoldService;

    public CustomerAccountServiceImpl(AccountService accountService, AccountBalanceService accountBalanceService,
            GLAccountMappingService glAccountMappingService, AccountTransactionService accountTransactionService,
            AccountHoldService accountHoldService) {
        this.accountService = accountService;
        this.accountBalanceService = accountBalanceService;
        this.glAccountMappingService = glAccountMappingService;
        this.accountTransactionService = accountTransactionService;
        this.accountHoldService = accountHoldService;
    }

    @Override
    public Optional<UUID> getPrimaryUserProfileIdForAccount(UUID accountId) {
        return accountService.getPrimaryUserProfileIdForAccount(accountId);
    }

    @Override
    public boolean accountExists(UUID accountId) {
        logger.debug("Checking account existence: {}", accountId);
        return accountService.accountExists(accountId);
    }

    @Override
    public boolean isAccountActive(UUID accountId) {
        logger.debug("Checking account active status: {}", accountId);
        return accountService.isAccountActive(accountId);
    }

    @Override
    public boolean isAccountEligibleForTransaction(UUID accountId, Object transactionType) {
        logger.debug("Checking account eligibility for transaction: {} type: {}", accountId, transactionType);
        return accountService.isAccountEligibleForTransaction(accountId);
    }

    @Override
    public boolean hasSufficientBalance(UUID accountId, BigDecimal amount) {
        logger.debug("Checking sufficient balance for account: {} amount: {}", accountId, amount);
        return accountBalanceService.hasSufficientBalance(accountId, amount);
    }

    @Override
    public boolean hasSufficientBalanceUnderLock(UUID accountId, BigDecimal amount) {
        logger.debug("Checking sufficient balance under lock for account: {} amount: {}", accountId, amount);
        return accountBalanceService.hasSufficientBalanceUnderLock(accountId, amount);
    }

    @Override
    public UUID getGLAccountIdForType(UUID accountId, GLAccountMappingType mappingType) {
        logger.debug("Resolving GL account for account: {} type: {}", accountId, mappingType);
        return glAccountMappingService.getGLAccountIdForType(accountId, mappingType);
    }

    @Override
    @Transactional
    public UUID recordAccountTransaction(UUID accountId, String transactionType, BigDecimal amount, String currency,
            LocalDateTime transactionDate, String description, String referenceId) {
        logger.info(
                "Recording account transaction for account: {} type: {} amount: {}",
                accountId,
                transactionType,
                amount);

        AccountTransactionType type = AccountTransactionType.valueOf(transactionType);
        var transaction = accountTransactionService
                .recordTransaction(accountId, type, amount, currency, transactionDate, description, referenceId);

        logger.info("Account transaction recorded successfully: {}", transaction.getId());
        return transaction.getId();
    }

    @Override
    @Transactional
    public void linkAccountTransactionToGL(UUID accountTransactionId, UUID glTransactionId) {
        logger.info("Linking account transaction: {} to GL transaction: {}", accountTransactionId, glTransactionId);
        accountTransactionService.updateGLTransactionLink(accountTransactionId, glTransactionId);
        logger.info("Account transaction linked to GL successfully");
    }

    @Override
    @Transactional
    public UUID recordAndLinkAccountTransaction(UUID accountId, String transactionType, BigDecimal amount,
            String currency, LocalDateTime transactionDate, String description, String referenceId,
            UUID glTransactionId) {
        logger.info(
                "Recording and linking account transaction for account: {} type: {} amount: {} GL: {}",
                accountId,
                transactionType,
                amount,
                glTransactionId);

        UUID accountTransactionId = recordAccountTransaction(
                accountId,
                transactionType,
                amount,
                currency,
                transactionDate,
                description,
                referenceId);
        linkAccountTransactionToGL(accountTransactionId, glTransactionId);

        logger.info("Account transaction recorded and linked successfully: {}", accountTransactionId);
        return accountTransactionId;
    }

    @Override
    @Transactional
    public UUID placeComplianceInvestigationHold(UUID accountId, BigDecimal amount, String currency, String reason,
            String externalReferenceId) {
        return accountHoldService.placeHold(accountId, amount, currency, reason, null, externalReferenceId).getId();
    }

    @Override
    @Transactional
    public void syncTransactionReservedAmount(UUID accountId, BigDecimal reservedAmount) {
        accountBalanceService.syncTransactionReservedAmount(accountId, reservedAmount);
    }
}
