package com.openfinova.banking.identity.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.identity.entity.AccountProvisioningStatus;
import com.openfinova.banking.identity.entity.BankingUser;

public interface UserRepository extends JpaRepository<BankingUser, UUID>, JpaSpecificationExecutor<BankingUser> {

    Optional<BankingUser> findByUsername(String username);

    /**
     * Loads the user and roles in one query; role permission collections use
     * {@link org.hibernate.annotations.FetchMode#SUBSELECT} on {@code BankingRole} to avoid N+1.
     */
    @Query("""
            SELECT DISTINCT u FROM BankingUser u
            LEFT JOIN FETCH u.roles
            WHERE u.username = :username
            """)
    Optional<BankingUser> findByUsernameWithRoles(@Param("username") String username);

    @Query("""
            SELECT DISTINCT u FROM BankingUser u
            LEFT JOIN FETCH u.roles
            WHERE u.username IN :usernames
            """)
    List<BankingUser> findByUsernameInWithRoles(@Param("usernames") Collection<String> usernames);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<BankingUser> findByEnabledTrue(Pageable pageable);

    Optional<BankingUser> findByCustomerPartyId(UUID customerPartyId);

    List<BankingUser> findByUsernameIn(Collection<String> usernames);

    /**
     * Fetches all users with their roles eagerly in a single query.
     * Used by the SoD violation scanner and user-access compliance report.
     */
    @Query("""
            SELECT DISTINCT u FROM BankingUser u
            LEFT JOIN FETCH u.roles
            """)
    List<BankingUser> findAllWithRoles();

    @Query("""
            SELECT u FROM BankingUser u
            WHERE u.accountExpiresAt IS NOT NULL
            AND u.accountExpiresAt > :now
            AND u.accountExpiresAt <= :horizon
            AND u.provisioningStatus = :status
            AND u.enabled = true
            """)
    List<BankingUser> findAccountsInExpiryWarningWindow(@Param("now") LocalDateTime now,
            @Param("horizon") LocalDateTime horizon, @Param("status") AccountProvisioningStatus status);
}
