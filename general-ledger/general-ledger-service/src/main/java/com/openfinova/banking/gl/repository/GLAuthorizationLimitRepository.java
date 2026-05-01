package com.openfinova.banking.gl.repository;

import com.openfinova.banking.gl.api.entity.GLTransactionSource;
import com.openfinova.banking.gl.api.entity.GLApprovalRole;
import com.openfinova.banking.gl.entity.GLAuthorizationLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for GL authorization limits.
 * Provides queries for approval workflow authorization checks.
 */
@Repository
public interface GLAuthorizationLimitRepository extends JpaRepository<GLAuthorizationLimit, UUID> {

    /**
     * Find authorization limit for a specific role, currency, and transaction source.
     * Returns the most specific match (exact source match preferred over NULL source).
     *
     * @param approvalRole the user's role
     * @param currency the transaction currency
     * @param source the transaction source
     * @return optional authorization limit
     */
    @Query("""
            SELECT l FROM GLAuthorizationLimit l
            WHERE l.approvalRole = :approvalRole
            AND l.currency = :currency
            AND l.isActive = true
            AND (l.transactionSource = :source OR l.transactionSource IS NULL)
            ORDER BY CASE WHEN l.transactionSource IS NULL THEN 1 ELSE 0 END
            """)
    List<GLAuthorizationLimit> findByRoleCurrencyAndSource(@Param("approvalRole") GLApprovalRole approvalRole,
            @Param("currency") String currency, @Param("source") GLTransactionSource source);

    /**
     * Find default authorization limit for a role and currency (NULL source).
     * Used when no source-specific limit is configured.
     *
     * @param approvalRole the user's role
     * @param currency the transaction currency
     * @return optional authorization limit
     */
    @Query("""
            SELECT l FROM GLAuthorizationLimit l
            WHERE l.approvalRole = :approvalRole
            AND l.currency = :currency
            AND l.transactionSource IS NULL
            AND l.isActive = true
            """)
    Optional<GLAuthorizationLimit> findByRoleAndCurrency(@Param("approvalRole") GLApprovalRole approvalRole,
            @Param("currency") String currency);

    /**
     * Find all active authorization limits for a role.
     *
     * @param approvalRole the user's role
     * @return list of authorization limits
     */
    @Query("""
            SELECT l FROM GLAuthorizationLimit l
            WHERE l.approvalRole = :approvalRole
            AND l.isActive = true
            ORDER BY l.currency, l.transactionSource
            """)
    List<GLAuthorizationLimit> findByRole(@Param("approvalRole") GLApprovalRole approvalRole);

    /**
     * Find all active authorization limits.
     * Used for administration and reporting.
     *
     * @return list of all active limits
     */
    @Query("""
            SELECT l FROM GLAuthorizationLimit l
            WHERE l.isActive = true
            ORDER BY l.approvalRole, l.currency, l.transactionSource
            """)
    List<GLAuthorizationLimit> findAllActive();

    /**
     * Check if a role exists in the authorization limits.
     * Used to verify if a role has any configured limits.
     *
     * @param approvalRole the user's role
     * @return true if role has at least one active limit
     */
    @Query("""
            SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END
            FROM GLAuthorizationLimit l
            WHERE l.approvalRole = :approvalRole
            AND l.isActive = true
            """)
    boolean existsByRole(@Param("approvalRole") GLApprovalRole approvalRole);

    /**
     * Find authorization limit by exact match (role, currency, source).
     *
     * @param approvalRole the user's role
     * @param currency the currency
     * @param source the transaction source (can be NULL)
     * @return optional authorization limit
     */
    Optional<GLAuthorizationLimit> findByApprovalRoleAndCurrencyAndTransactionSource(GLApprovalRole approvalRole,
            String currency, GLTransactionSource source);
}
