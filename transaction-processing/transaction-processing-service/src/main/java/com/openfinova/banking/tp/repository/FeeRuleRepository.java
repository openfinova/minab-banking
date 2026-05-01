package com.openfinova.banking.tp.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.openfinova.banking.tp.api.entity.CustomerTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.entity.FeeRule;

/**
 * Repository for FeeRule entities.
 */
public interface FeeRuleRepository extends JpaRepository<FeeRule, UUID> {

    /**
     * Find all fee rules by transaction type.
     */
    List<FeeRule> findByTransactionType(TransactionType transactionType);

    /**
     * Find all active fee rules.
     */
    List<FeeRule> findByIsActiveTrue();

    /**
     * Find fee rules by transaction type and customer tier.
     */
    List<FeeRule> findByTransactionTypeAndCustomerTier(TransactionType transactionType, CustomerTier customerTier);

    /**
     * Find currently effective fee rules (active and within date range).
     */
    @Query("""
            SELECT fr FROM FeeRule fr
            WHERE fr.isActive = true
            AND fr.effectiveFrom <= :currentTime
            AND (fr.effectiveTo IS NULL OR fr.effectiveTo > :currentTime)
            """)
    List<FeeRule> findCurrentlyEffectiveRules(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find effective fee rules for specific transaction type and customer tier.
     * Optimized with proper indexing hints and result limiting.
     */
    @Query("""
            SELECT fr FROM FeeRule fr
            WHERE fr.isActive = true
            AND fr.transactionType = :transactionType
            AND fr.customerTier = :customerTier
            AND fr.effectiveFrom <= :currentTime
            AND (fr.effectiveTo IS NULL OR fr.effectiveTo > :currentTime)
            ORDER BY fr.priority DESC
            """)
    List<FeeRule> findEffectiveRulesForTypeAndTier(@Param("transactionType") TransactionType transactionType,
            @Param("customerTier") CustomerTier customerTier, @Param("currentTime") LocalDateTime currentTime);

    // Optimized batch queries for fee rule management

    /**
     * Find fee rules for multiple transaction types efficiently.
     * Prevents N+1 problems when processing multiple transaction types.
     *
     * @param transactionTypes list of transaction types
     * @return list of fee rules for the transaction types
     */
    @Query("""
            SELECT fr FROM FeeRule fr
            WHERE fr.isActive = true
            AND fr.transactionType IN :transactionTypes
            ORDER BY fr.transactionType, fr.priority DESC
            """)
    List<FeeRule> findByTransactionTypeInBatch(@Param("transactionTypes") List<TransactionType> transactionTypes);

    /**
     * Find fee rules for multiple customer tiers efficiently.
     *
     * @param customerTiers list of customer tiers
     * @return list of fee rules for the customer tiers
     */
    @Query("""
            SELECT fr FROM FeeRule fr
            WHERE fr.isActive = true
            AND fr.customerTier IN :customerTiers
            ORDER BY fr.customerTier, fr.priority DESC
            """)
    List<FeeRule> findByCustomerTierInBatch(@Param("customerTiers") List<CustomerTier> customerTiers);

    /**
     * Find fee rules by multiple criteria for batch processing.
     * Optimized for bulk fee calculation operations.
     *
     * @param transactionTypes list of transaction types
     * @param customerTiers list of customer tiers
     * @param currentTime current time for effectiveness check
     * @return list of effective fee rules
     */
    @Query("""
            SELECT fr FROM FeeRule fr
            WHERE fr.isActive = true
            AND fr.transactionType IN :transactionTypes
            AND fr.customerTier IN :customerTiers
            AND fr.effectiveFrom <= :currentTime
            AND (fr.effectiveTo IS NULL OR fr.effectiveTo > :currentTime)
            ORDER BY fr.transactionType, fr.customerTier, fr.priority DESC
            """)
    List<FeeRule> findEffectiveRulesForBatch(@Param("transactionTypes") List<TransactionType> transactionTypes,
            @Param("customerTiers") List<CustomerTier> customerTiers, @Param("currentTime") LocalDateTime currentTime);
}
