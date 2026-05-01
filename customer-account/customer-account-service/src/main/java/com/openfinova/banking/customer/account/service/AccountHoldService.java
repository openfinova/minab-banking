package com.openfinova.banking.customer.account.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.openfinova.banking.customer.account.api.entity.HoldStatus;
import com.openfinova.banking.customer.account.entity.Account;
import com.openfinova.banking.customer.account.entity.AccountHold;
import com.openfinova.banking.customer.account.repository.AccountHoldRepository;
import com.openfinova.banking.customer.account.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

/**
 * Implementation of AccountHoldService for managing administrative holds on customer accounts.
 *
 * ARCHITECTURAL DECISION:
 * This service manages AccountHold entities which represent long-lived administrative holds
 * (court orders, fraud investigations, etc.) as opposed to short-lived transaction reservations
 * managed by BalanceReservationService in the TP module.
 *
 * IMPORTANT: This service does NOT directly manipulate account balances.
 * It only manages hold records. Available balance calculations are centralized
 * in AccountBalanceService which queries this service via getTotalHoldAmount().
 *
 * This service provides functionality for:
 * - Placing administrative holds on account funds
 * - Releasing holds when no longer needed
 * - Settling holds when transactions are posted
 * - Retrieving active holds for an account
 * - Processing expired holds automatically
 * - Calculating total held amounts for balance calculations
 */
@Service
@Transactional
public class AccountHoldService {

    private static final Logger logger = LoggerFactory.getLogger(AccountHoldService.class);

    private final AccountHoldRepository accountHoldRepository;
    private final AccountRepository accountRepository;

    /**
     * Constructs a new AccountHoldService with required dependencies.
     *
     * @param accountHoldRepository the repository for managing account hold entities
     * @param accountRepository the repository for accessing account entities
     */
    public AccountHoldService(AccountHoldRepository accountHoldRepository, AccountRepository accountRepository) {
        this.accountHoldRepository = accountHoldRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Places a new administrative hold on an account for a specified amount.
     *
     * @param accountId the unique identifier of the account
     * @param amount the amount to hold
     * @param currency the currency of the hold amount
     * @param reason the reason for placing the hold
     * @param expiresAt the date and time when the hold should expire
     * @return the newly created account hold entity
     * @throws EntityNotFoundException if the account is not found
     * @throws IllegalArgumentException if the amount is not positive or currency mismatches
     */
    public AccountHold placeHold(UUID accountId, BigDecimal amount, String currency, String reason,
            LocalDateTime expiresAt) {
        logger.debug(
                "Placing hold on account {}: amount={}, currency={}, reason={}, expiresAt={}",
                accountId,
                amount,
                currency,
                reason,
                expiresAt);

        // Validate account exists
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        // Validate amount is positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Hold amount must be positive");
        }

        // Validate currency matches account currency
        if (!currency.equals(account.getCurrency())) {
            throw new IllegalArgumentException(
                    String.format(
                            "Currency mismatch: hold currency %s does not match account currency %s",
                            currency,
                            account.getCurrency()));
        }

        // Note: We do NOT validate available balance here or manipulate account balances.
        // Balance validation should be done by the caller if needed.
        // Available balance calculation is centralized in AccountBalanceService.

        // Create the hold
        AccountHold hold = new AccountHold(account, amount, currency, reason);
        hold.setExpiresAt(expiresAt);
        hold.setStatus(HoldStatus.ACTIVE);

        AccountHold savedHold = accountHoldRepository.save(hold);

        logger.info(
                "Created hold {} on account {}: amount={}, reason={}",
                savedHold.getId(),
                accountId,
                amount,
                reason);

        return savedHold;
    }

    /**
     * Releases an active administrative hold, marking it as released.
     *
     * @param holdId the unique identifier of the hold to release
     * @throws EntityNotFoundException if the hold is not found
     * @throws IllegalStateException if the hold is not currently active
     */
    public void releaseHold(UUID holdId) {
        logger.debug("Releasing hold: {}", holdId);

        AccountHold hold = accountHoldRepository.findById(holdId)
                .orElseThrow(() -> new EntityNotFoundException("Hold not found: " + holdId));

        // Validate hold is active
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new IllegalStateException(
                    String.format("Cannot release hold %s: current status is %s", holdId, hold.getStatus()));
        }

        // Update hold status
        hold.setStatus(HoldStatus.RELEASED);
        accountHoldRepository.save(hold);

        logger.info(
                "Released hold {} on account {}: amount={}",
                holdId,
                hold.getCustomerAccount().getId(),
                hold.getAmount());

        // Note: We do NOT directly update account balances.
        // Available balance will be automatically recalculated by AccountBalanceService
        // when it queries getTotalHoldAmount() which will exclude this released hold.
    }

    /**
     * Settles an active administrative hold, marking it as consumed by a posted transaction.
     *
     * @param holdId the unique identifier of the hold to settle
     * @throws EntityNotFoundException if the hold is not found
     * @throws IllegalStateException if the hold is not currently active
     */
    public void settleHold(UUID holdId) {
        logger.debug("Settling hold: {}", holdId);

        AccountHold hold = accountHoldRepository.findById(holdId)
                .orElseThrow(() -> new EntityNotFoundException("Hold not found: " + holdId));

        // Validate hold is active
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new IllegalStateException(
                    String.format("Cannot settle hold %s: current status is %s", holdId, hold.getStatus()));
        }

        // Update hold status
        hold.setStatus(HoldStatus.SETTLED);
        accountHoldRepository.save(hold);

        logger.info(
                "Settled hold {} on account {}: amount={}",
                holdId,
                hold.getCustomerAccount().getId(),
                hold.getAmount());

        // Note: Available balance was already reduced when hold was placed (via calculation).
        // Ledger balance will be updated when the transaction is posted to GL.
        // This method only marks the hold as consumed.
    }

    /**
     * Retrieves all active administrative holds for a specific account.
     *
     * @param accountId the unique identifier of the account
     * @return a list of active account holds
     * @throws EntityNotFoundException if the account is not found
     */
    @Transactional(readOnly = true)
    public List<AccountHold> getActiveHoldsByAccount(UUID accountId) {
        logger.debug("Getting active holds for account: {}", accountId);

        // Verify account exists
        accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        List<AccountHold> holds = accountHoldRepository.findActiveHoldsByAccount(accountId);

        logger.debug("Found {} active holds for account {}", holds.size(), accountId);

        return holds;
    }

    /**
     * Calculates the total amount currently held across all active holds for an account.
     *
     * @param accountId the unique identifier of the account
     * @return the sum of all active hold amounts
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalHoldAmount(UUID accountId) {
        logger.debug("Calculating total hold amount for account: {}", accountId);

        List<AccountHold> activeHolds = accountHoldRepository.findActiveHoldsByAccount(accountId);

        BigDecimal totalHeld = activeHolds.stream().filter(AccountHold::isActive).map(AccountHold::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        logger.debug("Total hold amount for account {}: {}", accountId, totalHeld);

        return totalHeld;
    }

    /**
     * Identifies and marks all expired administrative holds across the system.
     *
     * @return the number of holds successfully marked as expired
     */
    public int processExpiredHolds() {
        logger.debug("Processing expired holds");

        LocalDateTime now = LocalDateTime.now();
        List<AccountHold> expiredHolds = accountHoldRepository.findExpiredHolds(now);

        int expiredCount = 0;

        for (AccountHold hold : expiredHolds) {
            try {
                // Update hold status
                hold.setStatus(HoldStatus.EXPIRED);
                accountHoldRepository.save(hold);

                logger.info(
                        "Expired hold {} on account {}: amount={}",
                        hold.getId(),
                        hold.getCustomerAccount().getId(),
                        hold.getAmount());

                expiredCount++;

                // Note: We do NOT directly update account balances.
                // Available balance will be automatically recalculated by AccountBalanceService
                // when it queries getTotalHoldAmount() which will exclude expired holds.
            } catch (Exception e) {
                logger.error("Error processing expired hold {}: {}", hold.getId(), e.getMessage(), e);
            }
        }

        logger.info("Processed {} expired holds", expiredCount);

        return expiredCount;
    }
}
