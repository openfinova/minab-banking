package com.openfinova.banking.customer.account.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.openfinova.banking.customer.account.entity.Account;
import com.openfinova.banking.customer.account.entity.InterestRate;
import com.openfinova.banking.customer.account.repository.AccountRepository;
import com.openfinova.banking.customer.account.repository.InterestRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

/**
 * Implementation of InterestRateService for managing interest rates on customer accounts.
 *
 * This service provides functionality for:
 * - Configuring interest rates (credit and debit) for accounts
 * - Retrieving currently effective rates
 * - Managing rate expiration and lifecycle
 * - Supporting time-based rate queries
 */
@Service
@Transactional
public class InterestRateService {

    private static final Logger logger = LoggerFactory.getLogger(InterestRateService.class);

    private final InterestRateRepository interestRateRepository;
    private final AccountRepository accountRepository;

    public InterestRateService(InterestRateRepository interestRateRepository, AccountRepository accountRepository) {
        this.interestRateRepository = interestRateRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Adds a new interest rate configuration to an account.
     *
     * @param accountId the unique identifier of the account
     * @param rateType the type of interest rate (e.g., credit, debit)
     * @param annualPercentageRate the annual percentage rate to apply
     * @param effectiveFrom the date and time when the rate becomes effective
     * @return the newly created interest rate entity
     * @throws EntityNotFoundException if the account is not found
     * @throws IllegalArgumentException if the rate is negative
     */
    public InterestRate addInterestRate(UUID accountId, InterestRate.RateType rateType, BigDecimal annualPercentageRate,
            LocalDateTime effectiveFrom) {
        logger.debug(
                "Adding interest rate for account {}: type={}, rate={}, effectiveFrom={}",
                accountId,
                rateType,
                annualPercentageRate,
                effectiveFrom);

        // Validate account exists
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        // Validate rate is non-negative
        if (annualPercentageRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate must be non-negative");
        }

        // Create new interest rate
        InterestRate interestRate = new InterestRate();
        interestRate.setCustomerAccount(account);
        interestRate.setRateType(rateType);
        interestRate.setAnnualPercentageRate(annualPercentageRate);
        interestRate.setEffectiveFrom(effectiveFrom);

        InterestRate savedRate = interestRateRepository.save(interestRate);

        logger.info(
                "Created interest rate {} for account {}: type={}, rate={}",
                savedRate.getId(),
                accountId,
                rateType,
                annualPercentageRate);

        return savedRate;
    }

    /**
     * Retrieves all currently active interest rates for an account.
     *
     * @param accountId the unique identifier of the account
     * @return a list of active interest rates
     * @throws EntityNotFoundException if the account is not found
     */
    @Transactional(readOnly = true)
    public List<InterestRate> getCurrentRates(UUID accountId) {
        logger.debug("Getting current interest rates for account: {}", accountId);

        // Verify account exists
        accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        LocalDateTime now = LocalDateTime.now();

        // Use repository query to efficiently fetch only current rates
        List<InterestRate> currentRates = interestRateRepository.findCurrentRatesByAccount(accountId, now);

        logger.debug("Found {} current interest rates for account {}", currentRates.size(), accountId);

        return currentRates;
    }

    /**
     * Retrieves the most recent effective interest rate of a specific type for an account.
     *
     * @param accountId the unique identifier of the account
     * @param rateType the specific type of rate to retrieve
     * @return the effective interest rate, or null if none is found
     * @throws EntityNotFoundException if the account is not found
     */
    @Transactional(readOnly = true)
    public InterestRate getEffectiveRateByType(UUID accountId, InterestRate.RateType rateType) {
        logger.debug("Getting effective interest rate for account {}, type: {}", accountId, rateType);

        // Verify account exists
        accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        LocalDateTime now = LocalDateTime.now();

        // Use repository query to find the most recent effective rate of the specified type
        return interestRateRepository.findCurrentRateByAccountAndType(accountId, rateType, now).orElse(null);
    }

    /**
     * Expires an active interest rate by setting its effective-until date.
     *
     * @param rateId the unique identifier of the interest rate
     * @param effectiveUntil the date and time when the rate should expire
     * @throws EntityNotFoundException if the interest rate is not found
     * @throws IllegalArgumentException if the effective-until date is before the effective-from date
     */
    public void expireRate(UUID rateId, LocalDateTime effectiveUntil) {
        logger.debug("Expiring interest rate {}, effectiveUntil: {}", rateId, effectiveUntil);

        InterestRate interestRate = interestRateRepository.findById(rateId)
                .orElseThrow(() -> new EntityNotFoundException("Interest rate not found: " + rateId));

        // Validate that effectiveUntil is after effectiveFrom
        if (effectiveUntil.isBefore(interestRate.getEffectiveFrom())) {
            throw new IllegalArgumentException("Effective until date must be after effective from date");
        }

        interestRate.setEffectiveUntil(effectiveUntil);
        interestRateRepository.save(interestRate);

        logger.info(
                "Expired interest rate {} for account {}, effectiveUntil: {}",
                rateId,
                interestRate.getCustomerAccount().getId(),
                effectiveUntil);
    }
}
