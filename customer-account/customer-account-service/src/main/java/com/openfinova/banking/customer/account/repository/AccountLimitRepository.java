package com.openfinova.banking.customer.account.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.openfinova.banking.customer.account.entity.AccountLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountLimitRepository extends JpaRepository<AccountLimit, UUID> {

    /**
     * Find active and effective limits for an account.
     *
     * @param accountId the account ID
     * @param now the reference time to check effectiveness
     * @return list of active effective limits
     */
    @Query("""
            SELECT l FROM AccountLimit l
            WHERE l.customerAccount.id = :accountId
              AND l.effectiveFrom <= :now
              AND (l.effectiveUntil IS NULL OR l.effectiveUntil > :now)
            """)
    List<AccountLimit> findActiveEffectiveLimitsByAccount(@Param("accountId") UUID accountId,
            @Param("now") Instant now);

}
