package com.openfinova.banking.customer.account.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.customer.account.api.dto.ValidationResult;
import com.openfinova.banking.customer.account.api.entity.LimitPeriod;
import com.openfinova.banking.customer.account.api.entity.LimitType;
import com.openfinova.banking.customer.account.entity.Account;
import com.openfinova.banking.customer.account.entity.AccountLimit;
import com.openfinova.banking.customer.account.entity.AccountTransaction;
import com.openfinova.banking.customer.account.repository.AccountLimitRepository;
import com.openfinova.banking.customer.account.repository.AccountRepository;
import com.openfinova.banking.customer.account.repository.AccountTransactionRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Implementation of AccountLimitService for managing account limits and constraints.
 *
 * This service provides functionality for:
 * - Adding and managing account limits (transaction and balance limits)
 * - Retrieving effective limits for accounts
 * - Validating transactions against configured limits
 * - Removing or expiring limits
 */
@Service
@Transactional
public class AccountLimitService {

    private static final Logger logger = LoggerFactory.getLogger(AccountLimitService.class);

    private final AccountLimitRepository accountLimitRepository;
    private final AccountRepository accountRepository;
    private final AccountTransactionRepository accountTransactionRepository;

    /**
     * Constructs a new AccountLimitService with required dependencies.
     *
     * @param accountLimitRepository the repository for managing account limit entities
     * @param accountRepository the repository for accessing account entities
     * @param accountTransactionRepository the repository for accessing transaction records
     */
    public AccountLimitService(AccountLimitRepository accountLimitRepository, AccountRepository accountRepository,
            AccountTransactionRepository accountTransactionRepository) {
        this.accountLimitRepository = accountLimitRepository;
        this.accountRepository = accountRepository;
        this.accountTransactionRepository = accountTransactionRepository;
    }

    /**
     * Adds a new limit constraint to an account.
     *
     * @param accountId the unique identifier of the account
     * @param limitType the type of limit to apply
     * @param limitPeriod the period over which the limit applies
     * @param maxAmount the maximum amount allowed (can be null if only count is limited)
     * @param maxCount the maximum number of transactions allowed (can be null if only amount is limited)
     * @param createdBy the user or system creating the limit
     * @return the newly created account limit entity
     * @throws EntityNotFoundException if the account is not found
     */
    public AccountLimit addLimit(UUID accountId, LimitType limitType, LimitPeriod limitPeriod, BigDecimal maxAmount,
            Integer maxCount, String createdBy) {
        logger.debug(
                "Adding limit for account {}: type={}, period={}, maxAmount={}, maxCount={}",
                accountId,
                limitType,
                limitPeriod,
                maxAmount,
                maxCount);

        // Validate account exists
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        // Create new limit
        AccountLimit limit = new AccountLimit(account, limitType, limitPeriod, createdBy);
        limit.setMaxAmount(maxAmount);
        limit.setMaxCount(maxCount);

        // Validate limit constraints
        limit.validateLimitConstraints();

        AccountLimit savedLimit = accountLimitRepository.save(limit);

        logger.info(
                "Created limit {} for account {}: type={}, period={}",
                savedLimit.getId(),
                accountId,
                limitType,
                limitPeriod);

        return savedLimit;
    }

    /**
     * Retrieves all active and effective limits currently applied to an account.
     *
     * @param accountId the unique identifier of the account
     * @return a list of active account limits
     * @throws EntityNotFoundException if the account is not found
     */
    @Transactional(readOnly = true)
    public List<AccountLimit> getEffectiveLimitsByAccount(UUID accountId) {
        logger.debug("Getting effective limits for account: {}", accountId);

        // Verify account exists
        accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        List<AccountLimit> limits = accountLimitRepository.findActiveEffectiveLimitsByAccount(accountId);

        logger.debug("Found {} effective limits for account {}", limits.size(), accountId);

        return limits;
    }

    /**
     * Validates a transaction amount against all applicable limits of a specific type for an account.
     *
     * @param accountId the unique identifier of the account
     * @param limitType the type of limit being checked
     * @param amount the transaction amount to validate
     * @return a ValidationResult indicating whether the transaction is allowed and detailing any errors or warnings
     * @throws EntityNotFoundException if the account is not found
     */
    @Transactional(readOnly = true)
    public ValidationResult checkLimit(UUID accountId, LimitType limitType, BigDecimal amount) {
        logger.debug("Checking limit for account {}: type={}, amount={}", accountId, limitType, amount);

        // Verify account exists
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        // Get all effective limits for the account
        List<AccountLimit> effectiveLimits = accountLimitRepository.findActiveEffectiveLimitsByAccount(accountId);

        // Filter limits by type
        List<AccountLimit> applicableLimits = effectiveLimits.stream()
                .filter(limit -> limit.getLimitType() == limitType).toList();

        if (applicableLimits.isEmpty()) {
            logger.debug("No applicable limits found for account {} and type {}", accountId, limitType);
            return ValidationResult.success();
        }

        ValidationResult result = new ValidationResult(true);

        // Check each applicable limit
        for (AccountLimit limit : applicableLimits) {
            ValidationResult limitCheck = checkSingleLimit(account, limit, amount);

            if (!limitCheck.isValid()) {
                result.setValid(false);
                result.getErrors().addAll(limitCheck.getErrors());
            }

            result.getWarnings().addAll(limitCheck.getWarnings());
            result.getApplicableLimits().add(formatLimitDescription(limit));
        }

        if (!result.isValid()) {
            logger.warn("Limit check failed for account {}: {}", accountId, result.getErrorMessage());
        }

        return result;
    }

    /**
     * Removes an active limit by expiring it.
     *
     * @param limitId the unique identifier of the limit
     * @param removedBy the user or system removing the limit
     * @throws EntityNotFoundException if the limit is not found
     */
    public void removeLimit(UUID limitId, String removedBy) {
        logger.debug("Removing limit {}, removedBy: {}", limitId, removedBy);

        AccountLimit limit = accountLimitRepository.findById(limitId)
                .orElseThrow(() -> new EntityNotFoundException("Limit not found: " + limitId));

        // Expire the limit
        limit.expire(removedBy);
        accountLimitRepository.save(limit);

        logger.info(
                "Removed limit {} for account {}, removedBy: {}",
                limitId,
                limit.getCustomerAccount().getId(),
                removedBy);
    }

    /**
     * Checks a single limit against a transaction amount.
     */
    private ValidationResult checkSingleLimit(Account account, AccountLimit limit, BigDecimal amount) {
        ValidationResult result = new ValidationResult(true);

        // Check balance limits
        if (limit.getLimitType().isBalanceLimit()) {
            return checkBalanceLimit(account, limit, amount);
        }

        // Check transaction limits
        if (limit.getLimitType().isTransactionLimit()) {
            return checkTransactionLimit(account, limit, amount);
        }

        return result;
    }

    /**
     * Checks balance-based limits (minimum balance, maximum balance, overdraft).
     */
    private ValidationResult checkBalanceLimit(Account account, AccountLimit limit, BigDecimal amount) {
        ValidationResult result = new ValidationResult(true);
        BigDecimal currentBalance = account.getAvailableBalance();
        BigDecimal projectedBalance = currentBalance.subtract(amount);

        switch (limit.getLimitType()) {
            case MINIMUM_BALANCE:
                if (limit.getMinAmount() != null && projectedBalance.compareTo(limit.getMinAmount()) < 0) {
                    result.addError(
                            String.format(
                                    "Transaction would violate minimum balance requirement of %s. Current: %s, Projected: %s",
                                    limit.getMinAmount(),
                                    currentBalance,
                                    projectedBalance));
                }
                break;

            case MAXIMUM_BALANCE:
                BigDecimal projectedBalanceCredit = currentBalance.add(amount);
                if (limit.getMaxAmount() != null && projectedBalanceCredit.compareTo(limit.getMaxAmount()) > 0) {
                    result.addError(
                            String.format(
                                    "Transaction would exceed maximum balance limit of %s. Current: %s, Projected: %s",
                                    limit.getMaxAmount(),
                                    currentBalance,
                                    projectedBalanceCredit));
                }
                break;

            case OVERDRAFT_LIMIT:
                if (projectedBalance.compareTo(BigDecimal.ZERO) < 0) {
                    BigDecimal overdraftAmount = projectedBalance.abs();
                    if (limit.getMaxAmount() != null && overdraftAmount.compareTo(limit.getMaxAmount()) > 0) {
                        result.addError(
                                String.format(
                                        "Transaction would exceed overdraft limit of %s. Overdraft amount: %s",
                                        limit.getMaxAmount(),
                                        overdraftAmount));
                    }
                }
                break;

            default:
                break;
        }

        return result;
    }

    /**
     * Checks transaction-based limits (daily, weekly, monthly transaction limits).
     */
    private ValidationResult checkTransactionLimit(Account account, AccountLimit limit, BigDecimal amount) {
        ValidationResult result = new ValidationResult(true);

        // Get the period boundaries
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = limit.getLimitPeriod().getPeriodStart(now);
        LocalDateTime periodEnd = limit.getLimitPeriod().getPeriodEnd(now);

        // Calculate current usage in the period
        BigDecimal periodUsage = calculatePeriodUsage(account.getId(), limit.getLimitType(), periodStart, periodEnd);
        BigDecimal projectedUsage = periodUsage.add(amount);

        // Check amount limit
        if (limit.getMaxAmount() != null && projectedUsage.compareTo(limit.getMaxAmount()) > 0) {
            result.addError(
                    String.format(
                            "Transaction would exceed %s limit of %s. Current usage: %s, Transaction: %s, Projected: %s",
                            limit.getLimitPeriod().getDescription().toLowerCase(),
                            limit.getMaxAmount(),
                            periodUsage,
                            amount,
                            projectedUsage));
        }

        // Check count limit
        if (limit.getMaxCount() != null) {
            int periodCount = calculatePeriodTransactionCount(
                    account.getId(),
                    limit.getLimitType(),
                    periodStart,
                    periodEnd);
            if (periodCount + 1 > limit.getMaxCount()) {
                result.addError(
                        String.format(
                                "Transaction would exceed %s transaction count limit of %d. Current count: %d",
                                limit.getLimitPeriod().getDescription().toLowerCase(),
                                limit.getMaxCount(),
                                periodCount));
            }
        }

        // Add warning if approaching limit (80% threshold)
        if (limit.getMaxAmount() != null && result.isValid()) {
            BigDecimal threshold = limit.getMaxAmount().multiply(new BigDecimal("0.80"));
            if (projectedUsage.compareTo(threshold) >= 0) {
                result.addWarning(
                        String.format(
                                "Approaching %s limit: %s of %s used (%.1f%%)",
                                limit.getLimitPeriod().getDescription().toLowerCase(),
                                projectedUsage,
                                limit.getMaxAmount(),
                                projectedUsage.divide(limit.getMaxAmount(), 4, java.math.RoundingMode.HALF_UP)
                                        .multiply(new BigDecimal("100")).doubleValue()));
            }
        }

        return result;
    }

    /**
     * Calculates the total transaction amount for a specific limit type within a period.
     */
    private BigDecimal calculatePeriodUsage(UUID accountId, LimitType limitType, LocalDateTime periodStart,
            LocalDateTime periodEnd) {
        try (Stream<AccountTransaction> stream = accountTransactionRepository
                .streamByAccountAndDateRange(accountId, periodStart, periodEnd)) {
            return stream.filter(tx -> isTransactionMatchingLimitType(tx, limitType))
                    .filter(tx -> "POSTED".equals(tx.getStatus()) || "COMPLETED".equals(tx.getStatus()))
                    .map(AccountTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /**
     * Calculates the transaction count for a specific limit type within a period.
     */
    private int calculatePeriodTransactionCount(UUID accountId, LimitType limitType, LocalDateTime periodStart,
            LocalDateTime periodEnd) {
        try (Stream<AccountTransaction> stream = accountTransactionRepository
                .streamByAccountAndDateRange(accountId, periodStart, periodEnd)) {
            return (int) stream.filter(tx -> isTransactionMatchingLimitType(tx, limitType))
                    .filter(tx -> "POSTED".equals(tx.getStatus()) || "COMPLETED".equals(tx.getStatus())).count();
        }
    }

    /**
     * Determines if a transaction matches a specific limit type.
     */
    private boolean isTransactionMatchingLimitType(AccountTransaction transaction, LimitType limitType) {
        return switch (limitType) {
            case WITHDRAWAL_LIMIT -> transaction.getTransactionType().name().contains("WITHDRAWAL");
            case TRANSFER_LIMIT -> transaction.getTransactionType().name().contains("TRANSFER");
            case DAILY_TRANSACTION, WEEKLY_TRANSACTION, MONTHLY_TRANSACTION, ANNUAL_TRANSACTION, VELOCITY_LIMIT -> true; // All transactions count
            default -> false;
        };
    }

    /**
     * Formats a limit description for display in validation results.
     */
    private String formatLimitDescription(AccountLimit limit) {
        StringBuilder sb = new StringBuilder();
        sb.append(limit.getLimitType().getDescription());
        sb.append(" (").append(limit.getLimitPeriod().getDescription()).append(")");

        if (limit.getMaxAmount() != null) {
            sb.append(": max amount ").append(limit.getMaxAmount());
        }

        if (limit.getMaxCount() != null) {
            if (limit.getMaxAmount() != null) {
                sb.append(", ");
            } else {
                sb.append(": ");
            }
            sb.append("max count ").append(limit.getMaxCount());
        }

        return sb.toString();
    }
}
