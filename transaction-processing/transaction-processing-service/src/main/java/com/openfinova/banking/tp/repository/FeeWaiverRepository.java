package com.openfinova.banking.tp.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.openfinova.banking.tp.api.entity.CustomerTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.entity.FeeWaiver;

/**
 * Repository for FeeWaiver entities.
 */
public interface FeeWaiverRepository extends JpaRepository<FeeWaiver, UUID> {

    /**
     * Find active fee waivers for a specific account.
     */
    @Query("""
            SELECT fw FROM FeeWaiver fw
            WHERE fw.isActive = true
            AND (fw.accountId = :accountId OR fw.isGlobal = true)
            AND fw.effectiveFrom <= :currentTime
            AND (fw.effectiveTo IS NULL OR fw.effectiveTo > :currentTime)
            AND (fw.maxUsageCount IS NULL OR fw.usageCount < fw.maxUsageCount)
            """)
    List<FeeWaiver> findActiveWaiversForAccount(@Param("accountId") UUID accountId,
            @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find active fee waivers by campaign code.
     */
    @Query("""
            SELECT fw FROM FeeWaiver fw
            WHERE fw.isActive = true
            AND fw.campaignCode = :campaignCode
            AND fw.effectiveFrom <= :currentTime
            AND (fw.effectiveTo IS NULL OR fw.effectiveTo > :currentTime)
            AND (fw.maxUsageCount IS NULL OR fw.usageCount < fw.maxUsageCount)
            """)
    List<FeeWaiver> findActiveWaiversByCampaign(@Param("campaignCode") String campaignCode,
            @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find applicable waivers for transaction type and customer tier.
     */
    @Query("""
            SELECT fw FROM FeeWaiver fw
            WHERE fw.isActive = true
            AND (fw.transactionType IS NULL OR fw.transactionType = :transactionType)
            AND (fw.customerTier IS NULL OR fw.customerTier = :customerTier)
            AND fw.effectiveFrom <= :currentTime
            AND (fw.effectiveTo IS NULL OR fw.effectiveTo > :currentTime)
            AND (fw.maxUsageCount IS NULL OR fw.usageCount < fw.maxUsageCount)
            """)
    List<FeeWaiver> findApplicableWaivers(@Param("transactionType") TransactionType transactionType,
            @Param("customerTier") CustomerTier customerTier, @Param("currentTime") LocalDateTime currentTime);
}
