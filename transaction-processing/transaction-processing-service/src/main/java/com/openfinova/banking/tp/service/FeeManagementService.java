package com.openfinova.banking.tp.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.customer.account.api.CustomerAccountService;
import com.openfinova.banking.customer.api.CustomerInfoService;
import com.openfinova.banking.customer.api.dto.CustomerInfo;
import com.openfinova.banking.customer.api.entity.CustomerType;
import com.openfinova.banking.setup.api.DateTimeService;
import com.openfinova.banking.tp.api.dto.FeeCalculationResult;
import com.openfinova.banking.tp.api.entity.CustomerTier;
import com.openfinova.banking.tp.api.entity.FeeTier;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.entity.FeeRule;
import com.openfinova.banking.tp.entity.FeeWaiver;
import com.openfinova.banking.tp.entity.Transaction;
import com.openfinova.banking.tp.entity.TransactionRequest;
import com.openfinova.banking.tp.repository.FeeRuleRepository;
import com.openfinova.banking.tp.repository.FeeWaiverRepository;

/**
 * Implementation of FeeManagementService with enhanced fee rule management,
 * dynamic fee calculation, and batch processing capabilities.
 *
 * Requirements addressed:
 * - 3.2: Fee rule management and dynamic fee calculation
 */
@Service
@Transactional
public class FeeManagementService {

    private static final Logger logger = LoggerFactory.getLogger(FeeManagementService.class);

    private final FeeRuleRepository feeRuleRepository;
    private final FeeWaiverRepository feeWaiverRepository;
    private final DateTimeService dateTimeService;
    private final CustomerAccountService customerAccountService;
    private final CustomerInfoService customerInfoService;

    public FeeManagementService(FeeRuleRepository feeRuleRepository, FeeWaiverRepository feeWaiverRepository,
            DateTimeService dateTimeService, CustomerAccountService customerAccountService,
            CustomerInfoService customerInfoService) {
        this.feeRuleRepository = feeRuleRepository;
        this.feeWaiverRepository = feeWaiverRepository;
        this.dateTimeService = dateTimeService;
        this.customerAccountService = customerAccountService;
        this.customerInfoService = customerInfoService;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('transaction:read', 'service:transaction:read')")
    public BigDecimal calculateFees(Transaction transaction) {
        logger.debug("Calculating fees for transaction: {}", transaction.getId());

        // Get customer tier (simplified - in real implementation would call customer service)
        com.openfinova.banking.tp.api.entity.CustomerTier customerTier = getCustomerTierForTransaction(transaction);

        // Find applicable fee rule
        Optional<FeeRule> applicableRule = getApplicableFeeRule(
                transaction.getRequest().getTransactionType(),
                customerTier);

        if (applicableRule.isEmpty()) {
            logger.warn(
                    "No applicable fee rule found for transaction type: {} and tier: {}",
                    transaction.getRequest().getTransactionType(),
                    customerTier);
            return BigDecimal.ZERO;
        }

        // Calculate base fee
        BigDecimal baseFee = calculateBaseFee(transaction, applicableRule.get());

        // Apply waivers
        BigDecimal finalFee = applyWaivers(transaction, applicableRule.get());

        logger.debug("Calculated fee for transaction {}: base={}, final={}", transaction.getId(), baseFee, finalFee);

        return finalFee;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "feeRules", key = "#type + '_' + #tier")
    public Optional<FeeRule> getApplicableFeeRule(TransactionType type,
            com.openfinova.banking.tp.api.entity.CustomerTier tier) {
        logger.debug("Finding applicable fee rule for type: {} and tier: {}", type, tier);

        List<FeeRule> effectiveRules = feeRuleRepository
                .findEffectiveRulesForTypeAndTier(type, tier, dateTimeService.now());

        if (effectiveRules.isEmpty()) {
            logger.warn("No effective fee rules found for type: {} and tier: {}", type, tier);
            return Optional.empty();
        }

        // Return the highest priority rule
        return Optional.of(effectiveRules.get(0));
    }

    public BigDecimal applyWaivers(Transaction transaction, FeeRule rule) {
        logger.debug("Applying waivers for transaction: {}", transaction.getId());

        BigDecimal baseFee = calculateBaseFee(transaction, rule);
        BigDecimal adjustedFee = baseFee;

        // Get customer tier
        com.openfinova.banking.tp.api.entity.CustomerTier customerTier = getCustomerTierForTransaction(transaction);

        // Find applicable waivers
        List<FeeWaiver> applicableWaivers = feeWaiverRepository.findApplicableWaivers(
                transaction.getRequest().getTransactionType(),
                customerTier,
                dateTimeService.now());

        // Apply account-specific waivers if transaction has source account
        if (transaction.getRequest().getSourceAccountId() != null) {
            List<FeeWaiver> accountWaivers = feeWaiverRepository
                    .findActiveWaiversForAccount(transaction.getRequest().getSourceAccountId(), dateTimeService.now());
            applicableWaivers.addAll(accountWaivers);
        }

        // Apply waivers (simplified logic - could be more complex)
        for (FeeWaiver waiver : applicableWaivers) {
            if (isWaiverApplicable(transaction, waiver)) {
                // For simplicity, assume 100% waiver - in real implementation would be configurable
                adjustedFee = BigDecimal.ZERO;
                logger.debug("Applied waiver {} to transaction {}", waiver.getWaiverName(), transaction.getId());
                break; // Apply first applicable waiver
            }
        }

        return adjustedFee;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "customerTiers", key = "#customerId")
    @PreAuthorize("hasAnyAuthority('transaction:read', 'service:transaction:read')")
    public CustomerTier evaluateTierEligibility(UUID customerId) {
        logger.debug("Evaluating tier eligibility for customer: {}", customerId);
        return customerInfoService.getCustomer(customerId).map(this::customerInfoToTier).orElse(CustomerTier.BASIC);
    }

    // Fee Rule Management Methods

    @Caching(evict = { @CacheEvict(value = "feeRules", key = "#rule.transactionType + '_' + #rule.customerTier"),
            @CacheEvict(value = "feeRules", key = "'type_' + #rule.transactionType") })
    public FeeRule createFeeRule(FeeRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Fee rule cannot be null");
        }

        logger.info("Creating new fee rule: {}", rule.getRuleName());

        // Validate the rule configuration
        rule.validateConfiguration();

        FeeRule savedRule = feeRuleRepository.save(rule);
        logger.info("Created fee rule with ID: {}", savedRule.getId());

        return savedRule;
    }

    @CacheEvict(value = "feeRules", allEntries = true)
    public FeeRule updateFeeRule(UUID ruleId, FeeRule updatedRule) {
        logger.info("Updating fee rule: {}", ruleId);

        if (ruleId == null || updatedRule == null) {
            throw new IllegalArgumentException("Rule ID and updated rule cannot be null");
        }

        FeeRule existingRule = feeRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Fee rule not found with ID: " + ruleId));

        // Validate the updated rule configuration
        updatedRule.validateConfiguration();

        // Update fields on the managed entity
        existingRule.setRuleName(updatedRule.getRuleName());
        existingRule.setTransactionType(updatedRule.getTransactionType());
        existingRule.setCustomerTier(updatedRule.getCustomerTier());
        existingRule.setPriority(updatedRule.getPriority());
        existingRule.setCompoundable(updatedRule.isCompoundable());
        existingRule.setGlRevenueAccountId(updatedRule.getGlRevenueAccountId());
        existingRule.setFeeType(updatedRule.getFeeType());
        existingRule.setFixedAmount(updatedRule.getFixedAmount());
        existingRule.setCurrency(updatedRule.getCurrency());
        existingRule.setPercentageRate(updatedRule.getPercentageRate());
        existingRule.setMinimumFee(updatedRule.getMinimumFee());
        existingRule.setMaximumFee(updatedRule.getMaximumFee());
        existingRule.setTierConfiguration(updatedRule.getTierConfiguration());
        existingRule.setMinTransactionAmount(updatedRule.getMinTransactionAmount());
        existingRule.setMaxTransactionAmount(updatedRule.getMaxTransactionAmount());
        existingRule.setTimeBasedStart(updatedRule.getTimeBasedStart());
        existingRule.setTimeBasedEnd(updatedRule.getTimeBasedEnd());
        existingRule.setIsActive(updatedRule.getIsActive());
        existingRule.setIsPromotional(updatedRule.getIsPromotional());
        existingRule.setEffectiveFrom(updatedRule.getEffectiveFrom());
        existingRule.setEffectiveTo(updatedRule.getEffectiveTo());
        existingRule.setMetadata(updatedRule.getMetadata());
        existingRule.setDescription(updatedRule.getDescription());

        FeeRule savedRule = feeRuleRepository.save(existingRule);
        logger.info("Updated fee rule: {}", savedRule.getId());

        return savedRule;
    }

    @CacheEvict(value = "feeRules", allEntries = true)
    public void deleteFeeRule(UUID ruleId) {
        logger.info("Deleting fee rule: {}", ruleId);

        if (ruleId == null) {
            throw new IllegalArgumentException("Rule ID cannot be null");
        }

        if (!feeRuleRepository.existsById(ruleId)) {
            throw new IllegalArgumentException("Fee rule not found with ID: " + ruleId);
        }

        feeRuleRepository.deleteById(ruleId);
        logger.info("Deleted fee rule: {}", ruleId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "feeRules", key = "'type_' + #type")
    public List<FeeRule> getFeeRulesByType(TransactionType type) {
        logger.debug("Getting fee rules for transaction type: {}", type);

        if (type == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }

        return feeRuleRepository.findByTransactionType(type);
    }

    @Transactional(readOnly = true)
    public List<FeeRule> getActiveFeeRules() {
        logger.debug("Getting all active fee rules");

        return feeRuleRepository.findByIsActiveTrue();
    }

    // Dynamic Fee Calculation Methods

    @Transactional(readOnly = true)
    public BigDecimal calculateDynamicFee(Transaction transaction, Map<String, Object> parameters) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        logger.debug(
                "Calculating dynamic fee for transaction: {} with parameters: {}",
                transaction.getId(),
                parameters);

        // Get base fee calculation
        BigDecimal baseFee = calculateFees(transaction);

        // Apply dynamic parameters
        if (parameters != null && !parameters.isEmpty()) {
            baseFee = applyDynamicParameters(baseFee, parameters);
        }

        return baseFee;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('transaction:read', 'service:transaction:read')")
    public FeeCalculationResult calculateDetailedFees(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        logger.debug("Calculating detailed fees for transaction: {}", transaction.getId());

        CustomerTier customerTier = getCustomerTierForTransaction(transaction);
        Optional<FeeRule> applicableRule = getApplicableFeeRule(
                transaction.getRequest().getTransactionType(),
                customerTier);

        FeeCalculationResult result = new FeeCalculationResult();
        result.setCalculationTimestamp(dateTimeService.now());

        if (applicableRule.isEmpty()) {
            result.setBaseFee(BigDecimal.ZERO);
            result.setAdjustedFee(BigDecimal.ZERO);
            result.setTotalFee(BigDecimal.ZERO);
            result.setCalculationMethod("NO_RULE_FOUND");
            return result;
        }

        // Calculate base fee
        FeeRule rule = applicableRule.get();
        BigDecimal baseFee = calculateBaseFee(transaction, rule);
        result.setBaseFee(baseFee);

        // Build fee components
        List<FeeCalculationResult.FeeComponent> components = buildFeeComponents(transaction, rule);
        result.setFeeComponents(components);

        // Apply waivers and build waiver details
        List<FeeCalculationResult.AppliedWaiver> appliedWaivers = new ArrayList<>();
        BigDecimal adjustedFee = applyWaiversDetailed(transaction, rule, appliedWaivers);
        result.setAdjustedFee(adjustedFee);
        result.setAppliedWaivers(appliedWaivers);

        result.setTotalFee(adjustedFee);
        result.setCalculationMethod(rule.getFeeType().name());

        return result;
    }

    // Fee Waiver Management Methods

    @CacheEvict(value = "feeWaivers", allEntries = true)
    public FeeWaiver createFeeWaiver(FeeWaiver waiver) {
        if (waiver == null) {
            throw new IllegalArgumentException("Fee waiver cannot be null");
        }

        logger.info("Creating new fee waiver: {}", waiver.getWaiverName());

        FeeWaiver savedWaiver = feeWaiverRepository.save(waiver);
        logger.info("Created fee waiver with ID: {}", savedWaiver.getId());

        return savedWaiver;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "feeWaivers", key = "#customerId")
    public List<FeeWaiver> getActiveFeeWaivers(UUID customerId) {
        logger.debug("Getting active fee waivers for customer: {}", customerId);

        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }

        return feeWaiverRepository.findActiveWaiversForAccount(customerId, dateTimeService.now());
    }

    @Transactional(readOnly = true)
    public boolean isWaiverApplicable(Transaction transaction, FeeWaiver waiver) {
        if (transaction == null || waiver == null) {
            return false;
        }

        // Check if waiver is currently effective
        if (!waiver.isCurrentlyEffective(dateTimeService.now())) {
            return false;
        }

        // Check transaction type compatibility
        if (!waiver.appliesToTransactionType(transaction.getRequest().getTransactionType())) {
            return false;
        }

        // Check customer tier compatibility
        com.openfinova.banking.tp.api.entity.CustomerTier customerTier = getCustomerTierForTransaction(transaction);
        if (!waiver.appliesToCustomerTier(customerTier)) {
            return false;
        }

        // Check account compatibility
        if (transaction.getRequest().getSourceAccountId() != null) {
            if (!waiver.appliesToAccount(transaction.getRequest().getSourceAccountId())) {
                return false;
            }
        }

        return true;
    }

    // Batch Operations

    @Transactional(readOnly = true)
    public List<BigDecimal> calculateFeesBatch(List<Transaction> transactions) {
        logger.debug("Calculating fees for {} transactions in batch", transactions.size());

        if (transactions == null || transactions.isEmpty()) {
            return new ArrayList<>();
        }

        return transactions.stream().map(this::calculateFees).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<UUID, FeeCalculationResult> calculateDetailedFeesBatch(List<Transaction> transactions) {
        logger.debug("Calculating detailed fees for {} transactions in batch", transactions.size());

        if (transactions == null || transactions.isEmpty()) {
            return new HashMap<>();
        }

        return transactions.stream().collect(Collectors.toMap(Transaction::getId, this::calculateDetailedFees));
    }

    /**
     * Resolves the customer tier for a transaction from the account owner's customer profile.
     * Uses source or destination account to resolve primary user profile, then customer info, then maps to tier.
     */
    @Transactional(readOnly = true)
    public CustomerTier getCustomerTierForTransaction(Transaction transaction) {
        UUID customerId = resolveCustomerIdForTransaction(transaction);
        if (customerId == null) {
            return CustomerTier.BASIC;
        }
        return evaluateTierEligibility(customerId);
    }

    /**
     * Resolves the customer (primary user profile) ID for a transaction from its source or destination account.
     * Assumes primary user profile ID is used as customer ID in the customer module.
     */
    private UUID resolveCustomerIdForTransaction(Transaction transaction) {
        TransactionRequest request = transaction.getRequest();
        UUID accountId = request.getSourceAccountId() != null ? request.getSourceAccountId()
                : request.getDestinationAccountId();
        if (accountId == null) {
            return null;
        }
        return customerAccountService.getPrimaryUserProfileIdForAccount(accountId).orElse(null);
    }

    /**
     * Maps customer profile to fee tier (e.g. BUSINESS/TRUST -> PREMIUM, INDIVIDUAL -> BASIC).
     * Can be extended to use segment, risk rating, or product holdings.
     */
    private CustomerTier customerInfoToTier(CustomerInfo customer) {
        if (customer == null || customer.getCustomerType() == null) {
            return CustomerTier.BASIC;
        }
        return switch (customer.getCustomerType()) {
            case CustomerType.BUSINESS, CustomerType.TRUST -> CustomerTier.PREMIUM;
            case CustomerType.INDIVIDUAL -> CustomerTier.BASIC;
        };
    }

    // Private helper methods

    private BigDecimal calculateBaseFee(Transaction transaction, FeeRule rule) {
        BigDecimal amount = transaction.getRequest().getAmount();

        switch (rule.getFeeType()) {
            case FIXED_AMOUNT:
            case FLAT:
                return rule.getFixedAmount() != null ? rule.getFixedAmount() : BigDecimal.ZERO;

            case PERCENTAGE:
                BigDecimal percentageFee = amount.multiply(rule.getPercentageRate());
                return applyMinMaxLimits(percentageFee, rule);

            case MINIMUM:
                BigDecimal minFee = amount.multiply(rule.getPercentageRate());
                return minFee.max(rule.getFixedAmount());

            case MAXIMUM:
                BigDecimal maxFee = amount.multiply(rule.getPercentageRate());
                return maxFee.min(rule.getFixedAmount());

            case TIERED:
                return calculateTieredFee(amount, rule);

            case NONE:
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal applyMinMaxLimits(BigDecimal fee, FeeRule rule) {
        if (rule.getMinimumFee() != null && fee.compareTo(rule.getMinimumFee()) < 0) {
            fee = rule.getMinimumFee();
        }

        if (rule.getMaximumFee() != null && fee.compareTo(rule.getMaximumFee()) > 0) {
            fee = rule.getMaximumFee();
        }

        return fee;
    }

    private BigDecimal calculateTieredFee(BigDecimal amount, FeeRule rule) {
        if (rule.getTierConfiguration() == null || rule.getTierConfiguration().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalFee = BigDecimal.ZERO;

        for (FeeTier tier : rule.getTierConfiguration()) {
            if (tier.appliesToAmount(amount)) {
                totalFee = totalFee.add(tier.calculateFee(amount));
                break; // Use first applicable tier
            }
        }

        return totalFee;
    }

    private BigDecimal applyDynamicParameters(BigDecimal baseFee, Map<String, Object> parameters) {
        BigDecimal adjustedFee = baseFee;

        // Apply multiplier if provided
        if (parameters.containsKey("multiplier")) {
            Object multiplierObj = parameters.get("multiplier");
            if (multiplierObj instanceof Number) {
                BigDecimal multiplier = new BigDecimal(multiplierObj.toString());
                adjustedFee = adjustedFee.multiply(multiplier);
            }
        }

        // Apply additional fee if provided
        if (parameters.containsKey("additionalFee")) {
            Object additionalFeeObj = parameters.get("additionalFee");
            if (additionalFeeObj instanceof Number) {
                BigDecimal additionalFee = new BigDecimal(additionalFeeObj.toString());
                adjustedFee = adjustedFee.add(additionalFee);
            }
        }

        // Apply discount if provided
        if (parameters.containsKey("discount")) {
            Object discountObj = parameters.get("discount");
            if (discountObj instanceof Number) {
                BigDecimal discount = new BigDecimal(discountObj.toString());
                adjustedFee = adjustedFee.subtract(discount);
            }
        }

        // Ensure fee is not negative
        return adjustedFee.max(BigDecimal.ZERO);
    }

    private List<FeeCalculationResult.FeeComponent> buildFeeComponents(Transaction transaction, FeeRule rule) {
        List<FeeCalculationResult.FeeComponent> components = new ArrayList<>();

        BigDecimal baseFee = calculateBaseFee(transaction, rule);

        components.add(
                new FeeCalculationResult.FeeComponent(
                        rule.getRuleName(),
                        rule.getFeeType().name(),
                        baseFee,
                        "Base fee calculation using " + rule.getFeeType().name() + " method"));

        return components;
    }

    private BigDecimal applyWaiversDetailed(Transaction transaction, FeeRule rule,
            List<FeeCalculationResult.AppliedWaiver> appliedWaivers) {
        BigDecimal baseFee = calculateBaseFee(transaction, rule);
        BigDecimal adjustedFee = baseFee;

        com.openfinova.banking.tp.api.entity.CustomerTier customerTier = getCustomerTierForTransaction(transaction);

        // Find applicable waivers
        List<FeeWaiver> applicableWaivers = feeWaiverRepository.findApplicableWaivers(
                transaction.getRequest().getTransactionType(),
                customerTier,
                dateTimeService.now());

        // Apply waivers and record details
        for (FeeWaiver waiver : applicableWaivers) {
            if (isWaiverApplicable(transaction, waiver)) {
                BigDecimal waiverAmount = adjustedFee; // For simplicity, assume 100% waiver
                adjustedFee = BigDecimal.ZERO;

                appliedWaivers.add(
                        new FeeCalculationResult.AppliedWaiver(
                                waiver.getId(),
                                waiver.getWaiverName(),
                                waiver.getCampaignCode(),
                                waiverAmount,
                                "FULL_WAIVER"));

                break; // Apply first applicable waiver
            }
        }

        return adjustedFee;
    }
}