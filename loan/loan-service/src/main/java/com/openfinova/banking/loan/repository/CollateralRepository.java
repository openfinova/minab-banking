package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.CollateralStatus;
import com.openfinova.banking.loan.api.entity.CollateralType;
import com.openfinova.banking.loan.entity.Collateral;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Collateral entities.
 */
public interface CollateralRepository extends JpaRepository<Collateral, UUID> {

    /**
     * Find all collateral for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of collateral for the loan account
     */
    @Query("""
            SELECT c FROM Collateral c
            WHERE c.loanAccount.id = :loanAccountId
            ORDER BY c.createdAt DESC
            """)
    List<Collateral> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find collateral by loan account and status.
     *
     * @param loanAccountId the loan account ID
     * @param status the collateral status
     * @return list of collateral matching both criteria
     */
    @Query("""
            SELECT c FROM Collateral c
            WHERE c.loanAccount.id = :loanAccountId
            AND c.status = :status
            ORDER BY c.createdAt DESC
            """)
    List<Collateral> findByLoanAccountIdAndStatus(@Param("loanAccountId") UUID loanAccountId,
            @Param("status") CollateralStatus status);

    /**
     * Find collateral by type.
     *
     * @param collateralType the collateral type
     * @param pageable pagination information
     * @return page of collateral of the specified type
     */
    Page<Collateral> findByCollateralType(CollateralType collateralType, Pageable pageable);

    /**
     * Find collateral by status.
     *
     * @param status the collateral status
     * @param pageable pagination information
     * @return page of collateral with the specified status
     */
    Page<Collateral> findByStatus(CollateralStatus status, Pageable pageable);

    Page<Collateral> findByLoanAccount_IdAndStatus(UUID loanAccountId, CollateralStatus status, Pageable pageable);

    /**
     * Find active collateral for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of active collateral
     */
    @Query("""
            SELECT c FROM Collateral c
            WHERE c.loanAccount.id = :loanAccountId
            AND c.status = 'ACTIVE'
            ORDER BY c.createdAt DESC
            """)
    List<Collateral> findActiveCollateralByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Count collateral for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of collateral
     */
    @Query("""
            SELECT COUNT(c) FROM Collateral c
            WHERE c.loanAccount.id = :loanAccountId
            """)
    long countByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum collateral value for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return total collateral value
     */
    @Query("""
            SELECT COALESCE(SUM(c.valuationAmount), 0)
            FROM Collateral c
            WHERE c.loanAccount.id = :loanAccountId
            AND c.status = 'ACTIVE'
            """)
    BigDecimal sumCollateralValueByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Check if loan account has active collateral.
     *
     * @param loanAccountId the loan account ID
     * @return true if there is active collateral
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM Collateral c
            WHERE c.loanAccount.id = :loanAccountId
            AND c.status = 'ACTIVE'
            """)
    boolean hasActiveCollateral(@Param("loanAccountId") UUID loanAccountId);
}
