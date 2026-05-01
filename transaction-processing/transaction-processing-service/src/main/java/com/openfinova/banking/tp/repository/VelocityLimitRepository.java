package com.openfinova.banking.tp.repository;

import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.entity.VelocityLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for VelocityLimit entities.
 */
public interface VelocityLimitRepository extends JpaRepository<VelocityLimit, UUID> {

    /**
     * Find all velocity limits for a specific account.
     */
    List<VelocityLimit> findByAccountId(UUID accountId);

    /**
     * Find all velocity limits for a specific account and transaction type.
     */
    List<VelocityLimit> findByAccountIdAndTransactionType(UUID accountId, TransactionType transactionType);

    /**
     * Find all velocity limits for a specific transaction type.
     */
    List<VelocityLimit> findByTransactionType(TransactionType transactionType);

    /**
     * Find active velocity limits for a specific account and transaction type.
     */
    List<VelocityLimit> findByAccountIdAndTransactionTypeAndIsActiveTrue(UUID accountId,
            TransactionType transactionType);

    /**
     * Find all active velocity limits.
     */
    List<VelocityLimit> findByIsActiveTrue();

    /**
     * Find velocity limits that need period reset.
     */
    @Query("SELECT vl FROM VelocityLimit vl WHERE vl.isActive = true AND vl.periodEnd <= :currentTime")
    List<VelocityLimit> findLimitsNeedingReset(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find velocity limits by account and active status.
     */
    List<VelocityLimit> findByAccountIdAndIsActive(UUID accountId, boolean isActive);

    // Optimized batch queries for velocity limit management

    /**
     * Find velocity limits for multiple accounts efficiently.
     * Prevents N+1 problems when processing multiple accounts.
     *
     * @param accountIds list of account IDs
     * @return list of velocity limits for the accounts
     */
    @Query("SELECT vl FROM VelocityLimit vl WHERE vl.accountId IN :accountIds AND vl.isActive = true ORDER BY vl.accountId, vl.transactionType")
    List<VelocityLimit> findByAccountIdInBatch(@Param("accountIds") List<UUID> accountIds);

    /**
     * Find velocity limits for multiple transaction types efficiently.
     *
     * @param transactionTypes list of transaction types
     * @return list of velocity limits for the transaction types
     */
    @Query("SELECT vl FROM VelocityLimit vl WHERE vl.transactionType IN :transactionTypes AND vl.isActive = true ORDER BY vl.transactionType, vl.accountId")
    List<VelocityLimit> findByTransactionTypeInBatch(@Param("transactionTypes") List<TransactionType> transactionTypes);

    /**
     * Find velocity limits by customer tier efficiently.
     *
     * @param customerTier the customer tier
     * @return list of velocity limits for the customer tier
     */
    @Query("SELECT vl FROM VelocityLimit vl WHERE vl.customerTier = :customerTier AND vl.isActive = true")
    List<VelocityLimit> findByCustomerTier(
            @Param("customerTier") com.openfinova.banking.tp.api.entity.CustomerTier customerTier);

    /**
     * Find velocity limits for batch limit checking.
     * Optimized for high-volume limit validation scenarios.
     *
     * @param accountIds list of account IDs
     * @param transactionTypes list of transaction types
     * @return list of active velocity limits
     */
    @Query("""
            SELECT vl FROM VelocityLimit vl
            WHERE vl.accountId IN :accountIds
            AND vl.transactionType IN :transactionTypes
            AND vl.isActive = true
            ORDER BY vl.accountId, vl.transactionType, vl.limitPeriod
            """)
    List<VelocityLimit> findForBatchLimitCheck(@Param("accountIds") List<UUID> accountIds,
            @Param("transactionTypes") List<TransactionType> transactionTypes);

    /**
     * Count velocity limits by account for reporting.
     *
     * @param accountIds list of account IDs
     * @return map of account ID to velocity limit count
     */
    @Query("""
            SELECT vl.accountId as accountId, COUNT(vl) as limitCount
            FROM VelocityLimit vl
            WHERE vl.accountId IN :accountIds
            AND vl.isActive = true
            GROUP BY vl.accountId
            """)
    List<Object[]> countLimitsByAccountIds(@Param("accountIds") List<UUID> accountIds);
}
