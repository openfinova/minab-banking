package com.openfinova.banking.identity.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.identity.entity.DelegationOfAuthority;
import com.openfinova.banking.identity.entity.DelegationStatus;

public interface DelegationOfAuthorityRepository extends JpaRepository<DelegationOfAuthority, UUID> {

    @Query("""
            SELECT d FROM DelegationOfAuthority d
            JOIN FETCH d.delegatedFrom JOIN FETCH d.delegatedTo
            WHERE d.delegatedTo.id = :toUserId
              AND d.status = :status
              AND d.validFrom <= :now
              AND (d.validUntil IS NULL OR d.validUntil >= :now)
              AND (d.transactionType = :txType OR d.transactionType = '*')
            """)
    List<DelegationOfAuthority> findActiveForDelegatee(@Param("toUserId") UUID toUserId,
            @Param("txType") String transactionType, @Param("status") DelegationStatus status,
            @Param("now") LocalDateTime now);

    List<DelegationOfAuthority> findByDelegatedFromId(UUID delegatedFromId);

    List<DelegationOfAuthority> findByDelegatedToId(UUID delegatedToId);
}
