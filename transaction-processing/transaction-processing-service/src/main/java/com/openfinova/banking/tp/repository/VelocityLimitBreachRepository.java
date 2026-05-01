package com.openfinova.banking.tp.repository;

import com.openfinova.banking.tp.entity.VelocityLimitBreach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for VelocityLimitBreachEntity entities.
 */
public interface VelocityLimitBreachRepository extends JpaRepository<VelocityLimitBreach, UUID> {

    /**
     * Find breaches for an account within a date range.
     */
    @Query("""
            SELECT vb FROM VelocityLimitBreach vb
            WHERE vb.accountId = :accountId
            AND vb.breachTimestamp >= :startDate
            AND vb.breachTimestamp <= :endDate
            ORDER BY vb.breachTimestamp DESC
            """)
    List<VelocityLimitBreach> findByAccountIdAndDateRange(@Param("accountId") UUID accountId,
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find recent breaches for an account.
     */
    @Query("""
            SELECT vb FROM VelocityLimitBreach vb
            WHERE vb.accountId = :accountId
            AND vb.breachTimestamp >= :since
            ORDER BY vb.breachTimestamp DESC
            """)
    List<VelocityLimitBreach> findRecentBreaches(@Param("accountId") UUID accountId,
            @Param("since") LocalDateTime since);
}