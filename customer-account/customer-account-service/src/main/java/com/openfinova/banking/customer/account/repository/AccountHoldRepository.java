package com.openfinova.banking.customer.account.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.openfinova.banking.customer.account.entity.AccountHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountHoldRepository extends JpaRepository<AccountHold, UUID> {

    /**
     * Finds all active holds for a specific account.
     *
     * @param accountId the account ID
     * @return list of active holds
     */
    @Query("""
            SELECT h FROM AccountHold h WHERE h.customerAccount.id = :accountId AND h.status = HoldStatus.ACTIVE
            AND (h.expiresAt IS NULL OR h.expiresAt > CURRENT_TIMESTAMP) ORDER BY h.createdAt DESC
            """)
    List<AccountHold> findActiveHoldsByAccount(@Param("accountId") UUID accountId);

    /**
     * Finds all holds that have expired (past their expiration date and still active).
     *
     * @param currentTime the current timestamp
     * @return list of expired holds
     */
    @Query("""
            SELECT h FROM AccountHold h WHERE h.status = HoldStatus.ACTIVE AND h.expiresAt IS NOT NULL
            AND h.expiresAt <= :currentTime
            """)
    List<AccountHold> findExpiredHolds(@Param("currentTime") LocalDateTime currentTime);
}
