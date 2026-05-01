package com.openfinova.banking.gl.repository;

import com.openfinova.banking.gl.api.entity.ClearingRuleType;
import com.openfinova.banking.gl.entity.SuspenseClearingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for SuspenseClearingRule entity operations.
 */
@Repository
public interface SuspenseClearingRuleRepository extends JpaRepository<SuspenseClearingRule, UUID> {

    /**
     * Find all active clearing rules.
     */
    List<SuspenseClearingRule> findByIsActiveTrueOrderByPriorityAsc();

    /**
     * Find active clearing rules by type.
     */
    List<SuspenseClearingRule> findByRuleTypeAndIsActiveTrueOrderByPriorityAsc(ClearingRuleType ruleType);

    /**
     * Find active clearing rules for a specific currency.
     */
    @Query("""
            SELECT r FROM SuspenseClearingRule r
            WHERE r.isActive = true
              AND (r.currency IS NULL OR r.currency = :currency)
            ORDER BY r.priority ASC
            """)
    List<SuspenseClearingRule> findActiveRulesForCurrency(@Param("currency") String currency);

    /**
     * Find all rules (active and inactive).
     */
    List<SuspenseClearingRule> findAllByOrderByPriorityAsc();
}
