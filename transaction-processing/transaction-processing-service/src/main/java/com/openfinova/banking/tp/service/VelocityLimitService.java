package com.openfinova.banking.tp.service;

import com.openfinova.banking.setup.api.DateTimeService;
import com.openfinova.banking.tp.api.dto.*;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.api.entity.VelocityLimitPeriod;
import com.openfinova.banking.tp.entity.*;
import com.openfinova.banking.tp.repository.VelocityLimitBreachRepository;
import com.openfinova.banking.tp.repository.VelocityLimitRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of VelocityLimitService providing comprehensive velocity limit management.
 */
@Service
@Transactional
public class VelocityLimitService {

    private final VelocityLimitRepository velocityLimitRepository;
    private final VelocityLimitBreachRepository breachRepository;
    private final DateTimeService dateTimeService;

    public VelocityLimitService(VelocityLimitRepository velocityLimitRepository,
            VelocityLimitBreachRepository breachRepository, DateTimeService dateTimeService) {
        this.velocityLimitRepository = velocityLimitRepository;
        this.breachRepository = breachRepository;
        this.dateTimeService = dateTimeService;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('transaction:read', 'service:transaction:read')")
    public boolean checkLimits(UUID accountId, TransactionType type, BigDecimal amount, String currency) {
        List<VelocityLimit> limits = velocityLimitRepository
                .findByAccountIdAndTransactionTypeAndIsActiveTrue(accountId, type);

        for (VelocityLimit limit : limits) {
            // Reset limit if period has expired
            if (!limit.isPeriodActive(dateTimeService.now())) {
                resetLimitPeriod(limit);
            }

            // Check if currency matches (null means all currencies)
            if (limit.getCurrency() != null && !limit.getCurrency().equals(currency)) {
                continue;
            }

            // Check count limit
            if (limit.wouldExceedCountLimit(1)) {
                recordLimitBreach(
                        accountId,
                        type,
                        amount,
                        "Transaction count would exceed limit: " + limit.getMaxCount());
                return false;
            }

            // Check amount limit
            if (limit.wouldExceedAmountLimit(amount)) {
                recordLimitBreach(
                        accountId,
                        type,
                        amount,
                        "Transaction amount would exceed limit: " + limit.getMaxAmount());
                return false;
            }
        }

        return true;
    }

    @PreAuthorize("hasAnyAuthority('transaction:write', 'service:transaction:write')")
    public void incrementUsage(UUID accountId, TransactionType type, BigDecimal amount, String currency) {
        List<VelocityLimit> limits = velocityLimitRepository
                .findByAccountIdAndTransactionTypeAndIsActiveTrue(accountId, type);

        for (VelocityLimit limit : limits) {
            // Reset limit if period has expired
            if (!limit.isPeriodActive(dateTimeService.now())) {
                resetLimitPeriod(limit);
            }

            // Check if currency matches (null means all currencies)
            if (limit.getCurrency() != null && !limit.getCurrency().equals(currency)) {
                continue;
            }

            // Increment usage
            limit.addTransaction(amount);
            velocityLimitRepository.save(limit);
        }
    }

    @Transactional(readOnly = true)
    public List<VelocityLimit> getRemainingLimits(UUID accountId, TransactionType type) {
        return velocityLimitRepository.findByAccountIdAndTransactionTypeAndIsActiveTrue(accountId, type);
    }

    public void resetExpiredLimits(UUID accountId) {
        List<VelocityLimit> limits = velocityLimitRepository.findByAccountIdAndIsActive(accountId, true);

        for (VelocityLimit limit : limits) {
            if (!limit.isPeriodActive(dateTimeService.now())) {
                resetLimitPeriod(limit);
            }
        }
    }

    // New limit configuration methods

    public VelocityLimit createVelocityLimit(VelocityLimit limit) {
        if (limit.getId() != null) {
            throw new IllegalArgumentException("Cannot create velocity limit with existing ID");
        }

        // Validate required fields
        validateVelocityLimit(limit);

        // Set default values
        if (limit.getCurrentCount() == null) {
            limit.setCurrentCount(0);
        }
        if (limit.getCurrentAmount() == null) {
            limit.setCurrentAmount(BigDecimal.ZERO);
        }

        // Set period boundaries
        LocalDateTime now = dateTimeService.now();
        limit.setPeriodStart(limit.getVelocityLimitPeriod().getPeriodStart(now));
        limit.setPeriodEnd(limit.getVelocityLimitPeriod().getPeriodEnd(now));

        return velocityLimitRepository.save(limit);
    }

    public VelocityLimit updateVelocityLimit(UUID limitId, VelocityLimit updatedLimit) {
        VelocityLimit existingLimit = velocityLimitRepository.findById(limitId)
                .orElseThrow(() -> new IllegalArgumentException("Velocity limit not found: " + limitId));

        // Validate updated limit
        validateVelocityLimit(updatedLimit);

        // Update fields (preserve ID and usage counters)
        existingLimit.setTransactionType(updatedLimit.getTransactionType());
        existingLimit.setVelocityLimitPeriod(updatedLimit.getVelocityLimitPeriod());
        existingLimit.setCustomerTier(updatedLimit.getCustomerTier());
        existingLimit.setCurrency(updatedLimit.getCurrency());
        existingLimit.setMaxCount(updatedLimit.getMaxCount());
        existingLimit.setMaxAmount(updatedLimit.getMaxAmount());
        existingLimit.setActive(updatedLimit.isActive());

        return velocityLimitRepository.save(existingLimit);
    }

    public void deleteVelocityLimit(UUID limitId) {
        if (!velocityLimitRepository.existsById(limitId)) {
            throw new IllegalArgumentException("Velocity limit not found: " + limitId);
        }
        velocityLimitRepository.deleteById(limitId);
    }

    @Transactional(readOnly = true)
    public List<VelocityLimit> getVelocityLimitsByAccount(UUID accountId) {
        return velocityLimitRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<VelocityLimit> getVelocityLimitsByType(TransactionType type) {
        return velocityLimitRepository.findByTransactionType(type);
    }

    // Real-time monitoring methods

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('transaction:read', 'service:transaction:read')")
    public VelocityLimitStatus getCurrentLimitStatus(UUID accountId, TransactionType type) {
        List<VelocityLimit> limits = velocityLimitRepository
                .findByAccountIdAndTransactionTypeAndIsActiveTrue(accountId, type);

        VelocityLimitStatus status = new VelocityLimitStatus(accountId, type, dateTimeService.now());

        Map<VelocityLimitPeriod, LimitUsage> currentUsage = new HashMap<>();
        Map<VelocityLimitPeriod, BigDecimal> remainingAmounts = new HashMap<>();
        Map<VelocityLimitPeriod, Integer> remainingCounts = new HashMap<>();

        for (VelocityLimit limit : limits) {
            VelocityLimitPeriod period = limit.getVelocityLimitPeriod();

            LimitUsage usage = new LimitUsage(
                    limit.getCurrentCount(),
                    limit.getCurrentAmount(),
                    limit.getMaxCount(),
                    limit.getMaxAmount(),
                    limit.getPeriodStart(),
                    limit.getPeriodEnd());

            currentUsage.put(period, usage);
            remainingAmounts.put(period, limit.getRemainingAmount());
            remainingCounts.put(period, limit.getRemainingCount());
        }

        status.setCurrentUsage(currentUsage);
        status.setRemainingAmounts(remainingAmounts);
        status.setRemainingCounts(remainingCounts);

        // Get recent breaches (last 24 hours)
        LocalDateTime since = dateTimeService.now().minusDays(1);
        List<VelocityLimitBreach> recentBreachEntities = breachRepository.findRecentBreaches(accountId, since);
        List<VelocityLimitBreachDTO> recentBreaches = recentBreachEntities.stream().map(this::convertToDto)
                .collect(Collectors.toList());
        status.setRecentBreaches(recentBreaches);

        return status;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('transaction:read', 'service:transaction:read')")
    public BigDecimal getRemainingLimit(UUID accountId, TransactionType type, VelocityLimitPeriod period) {
        List<VelocityLimit> limits = velocityLimitRepository
                .findByAccountIdAndTransactionTypeAndIsActiveTrue(accountId, type);

        return limits.stream().filter(limit -> limit.getVelocityLimitPeriod() == period)
                .map(VelocityLimit::getRemainingAmount).filter(Objects::nonNull).min(BigDecimal::compareTo)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Integer getRemainingTransactionCount(UUID accountId, TransactionType type, VelocityLimitPeriod period) {
        List<VelocityLimit> limits = velocityLimitRepository
                .findByAccountIdAndTransactionTypeAndIsActiveTrue(accountId, type);

        return limits.stream().filter(limit -> limit.getVelocityLimitPeriod() == period)
                .map(VelocityLimit::getRemainingCount).filter(Objects::nonNull).min(Integer::compareTo).orElse(null);
    }

    @Transactional(readOnly = true)
    public LocalDateTime getNextLimitReset(UUID accountId, TransactionType type, VelocityLimitPeriod period) {
        List<VelocityLimit> limits = velocityLimitRepository
                .findByAccountIdAndTransactionTypeAndIsActiveTrue(accountId, type);

        return limits.stream().filter(limit -> limit.getVelocityLimitPeriod() == period)
                .map(VelocityLimit::getPeriodEnd).min(LocalDateTime::compareTo).orElse(null);
    }

    // Limit breach handling methods

    @Transactional(readOnly = true)
    public List<VelocityLimitBreachDTO> getLimitBreaches(UUID accountId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<VelocityLimitBreach> breachEntities = breachRepository
                .findByAccountIdAndDateRange(accountId, startDateTime, endDateTime);

        return breachEntities.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public void recordLimitBreach(UUID accountId, TransactionType type, BigDecimal attemptedAmount, String reason) {
        // Find the most restrictive limit that was breached
        List<VelocityLimit> limits = velocityLimitRepository
                .findByAccountIdAndTransactionTypeAndIsActiveTrue(accountId, type);

        for (VelocityLimit limit : limits) {
            if (limit.wouldExceedAmountLimit(attemptedAmount) || limit.wouldExceedCountLimit(1)) {
                VelocityLimitBreach breach = new VelocityLimitBreach(
                        accountId,
                        type,
                        limit.getVelocityLimitPeriod(),
                        attemptedAmount,
                        reason);

                breach.setLimitAmount(limit.getMaxAmount());
                breach.setLimitCount(limit.getMaxCount());
                breach.setAttemptedCount(1);

                if (limit.wouldExceedAmountLimit(attemptedAmount)) {
                    breach.setBreachType("AMOUNT");
                } else {
                    breach.setBreachType("COUNT");
                }

                breachRepository.save(breach);
                break; // Record only the first breach found
            }
        }
    }

    // Batch operations

    @Transactional(readOnly = true)
    public Map<UUID, Boolean> checkLimitsBatch(List<LimitCheckRequest> requests) {
        Map<UUID, Boolean> results = new HashMap<>();

        for (LimitCheckRequest request : requests) {
            try {
                boolean result = checkLimits(
                        request.getAccountId(),
                        request.getTransactionType(),
                        request.getAmount(),
                        request.getCurrency());
                results.put(request.getAccountId(), result);
            } catch (Exception e) {
                results.put(request.getAccountId(), false);
            }
        }

        return results;
    }

    public void incrementUsageBatch(List<UsageIncrementRequest> requests) {
        for (UsageIncrementRequest request : requests) {
            try {
                incrementUsage(
                        request.getAccountId(),
                        request.getTransactionType(),
                        request.getAmount(),
                        request.getCurrency());
            } catch (Exception e) {
                // Log error but continue with other requests
                // In a real implementation, you might want to collect errors and return them
            }
        }
    }

    // Private helper methods

    private void resetLimitPeriod(VelocityLimit limit) {
        LocalDateTime now = dateTimeService.now();
        LocalDateTime newPeriodStart = limit.getVelocityLimitPeriod().getPeriodStart(now);
        limit.resetForNewPeriod(newPeriodStart, now);
        velocityLimitRepository.save(limit);
    }

    private void validateVelocityLimit(VelocityLimit limit) {
        if (limit.getAccountId() == null) {
            throw new IllegalArgumentException("Account ID is required");
        }
        if (limit.getTransactionType() == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }
        if (limit.getVelocityLimitPeriod() == null) {
            throw new IllegalArgumentException("Limit period is required");
        }
        if (limit.getCustomerTier() == null) {
            throw new IllegalArgumentException("Customer tier is required");
        }
        if (limit.getMaxCount() == null && limit.getMaxAmount() == null) {
            throw new IllegalArgumentException("At least one limit (count or amount) must be specified");
        }
        if (limit.getMaxCount() != null && limit.getMaxCount() <= 0) {
            throw new IllegalArgumentException("Max count must be positive");
        }
        if (limit.getMaxAmount() != null && limit.getMaxAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max amount must be positive");
        }
    }

    private VelocityLimitBreachDTO convertToDto(VelocityLimitBreach entity) {
        VelocityLimitBreachDTO dto = new VelocityLimitBreachDTO();
        dto.setId(entity.getId());
        dto.setAccountId(entity.getAccountId());
        dto.setTransactionType(entity.getTransactionType());
        dto.setVelocityLimitPeriod(entity.getVelocityLimitPeriod());
        dto.setAttemptedAmount(entity.getAttemptedAmount());
        dto.setAttemptedCount(entity.getAttemptedCount());
        dto.setLimitAmount(entity.getLimitAmount());
        dto.setLimitCount(entity.getLimitCount());
        dto.setReason(entity.getReason());
        dto.setBreachType(entity.getBreachType());

        // Convert Instant to LocalDateTime
        Instant breachInstant = entity.getBreachTimestamp();
        if (breachInstant != null) {
            dto.setBreachTimestamp(LocalDateTime.ofInstant(breachInstant, java.time.ZoneId.systemDefault()));
        }

        return dto;
    }
}