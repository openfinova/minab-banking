package com.openfinova.banking.customer.account.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.openfinova.banking.customer.account.entity.AccountRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRelationshipRepository extends JpaRepository<AccountRelationship, UUID> {

    /**
     * Find active relationships for an account.
     *
     * @param accountId the account ID
     * @return list of active relationships
     */
    @Query("""
            SELECT r FROM AccountRelationship r
            WHERE r.customerAccount.id = :accountId
            AND r.status = 'ACTIVE'
            """)
    List<AccountRelationship> findActiveRelationshipsByAccount(@Param("accountId") UUID accountId);

    /**
     * Find active relationship between account and user.
     *
     * @param accountId the account ID
     * @param userProfileId the user profile ID
     * @return optional containing the relationship if found
     */
    @Query("""
            SELECT r FROM AccountRelationship r
            WHERE r.customerAccount.id = :accountId
            AND r.userProfileId = :userProfileId
            AND r.status = 'ACTIVE'
            """)
    Optional<AccountRelationship> findActiveRelationshipByAccountAndUser(@Param("accountId") UUID accountId,
            @Param("userProfileId") UUID userProfileId);

    /**
     * Check if user has specific permission on account.
     *
     * @param accountId the account ID
     * @param userProfileId the user profile ID
     * @param permission the permission to check
     * @return true if user has the permission
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM AccountRelationship r
            WHERE r.customerAccount.id = :accountId
            AND r.userProfileId = :userProfileId
            AND r.status = 'ACTIVE'
            AND r.permissions LIKE CONCAT('%"', :permission, '"%')
            """)
    boolean hasPermission(@Param("accountId") UUID accountId, @Param("userProfileId") UUID userProfileId,
            @Param("permission") String permission);
}
