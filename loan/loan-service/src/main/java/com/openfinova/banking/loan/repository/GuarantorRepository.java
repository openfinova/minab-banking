package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.GuarantorStatus;
import com.openfinova.banking.loan.api.entity.GuarantorType;
import com.openfinova.banking.loan.entity.Guarantor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Guarantor entities.
 */
public interface GuarantorRepository extends JpaRepository<Guarantor, UUID> {

    /**
     * Find all guarantors for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of guarantors for the loan account
     */
    @Query("""
            SELECT g FROM Guarantor g
            WHERE g.loanAccount.id = :loanAccountId
            ORDER BY g.createdAt DESC
            """)
    List<Guarantor> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find guarantors by loan account and status.
     *
     * @param loanAccountId the loan account ID
     * @param status the guarantor status
     * @return list of guarantors matching both criteria
     */
    @Query("""
            SELECT g FROM Guarantor g
            WHERE g.loanAccount.id = :loanAccountId
            AND g.status = :status
            ORDER BY g.createdAt DESC
            """)
    List<Guarantor> findByLoanAccountIdAndStatus(@Param("loanAccountId") UUID loanAccountId,
            @Param("status") GuarantorStatus status);

    /**
     * Find guarantors by customer ID (guarantor is a customer).
     *
     * @param customerId the customer ID
     * @param pageable pagination information
     * @return page of guarantor records for the customer
     */
    @Query("""
            SELECT g FROM Guarantor g
            WHERE g.customerId = :customerId
            ORDER BY g.createdAt DESC
            """)
    Page<Guarantor> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    /**
     * Find guarantors by type.
     *
     * @param guarantorType the guarantor type
     * @param pageable pagination information
     * @return page of guarantors of the specified type
     */
    Page<Guarantor> findByGuarantorType(GuarantorType guarantorType, Pageable pageable);

    /**
     * Find guarantors by status.
     *
     * @param status the guarantor status
     * @param pageable pagination information
     * @return page of guarantors with the specified status
     */
    Page<Guarantor> findByStatus(GuarantorStatus status, Pageable pageable);

    Page<Guarantor> findByLoanAccount_IdAndStatus(UUID loanAccountId, GuarantorStatus status, Pageable pageable);

    /**
     * Find active guarantors for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of active guarantors
     */
    @Query("""
            SELECT g FROM Guarantor g
            WHERE g.loanAccount.id = :loanAccountId
            AND g.status = 'ACTIVE'
            ORDER BY g.createdAt DESC
            """)
    List<Guarantor> findActiveGuarantorsByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Count guarantors for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of guarantors
     */
    @Query("""
            SELECT COUNT(g) FROM Guarantor g
            WHERE g.loanAccount.id = :loanAccountId
            """)
    long countByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Count active guarantors for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of active guarantors
     */
    @Query("""
            SELECT COUNT(g) FROM Guarantor g
            WHERE g.loanAccount.id = :loanAccountId
            AND g.status = 'ACTIVE'
            """)
    long countActiveGuarantorsByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Check if loan account has active guarantors.
     *
     * @param loanAccountId the loan account ID
     * @return true if there are active guarantors
     */
    @Query("""
            SELECT COUNT(g) > 0 FROM Guarantor g
            WHERE g.loanAccount.id = :loanAccountId
            AND g.status = 'ACTIVE'
            """)
    boolean hasActiveGuarantors(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find all loans guaranteed by a customer.
     *
     * @param customerId the customer ID
     * @return list of guarantor records
     */
    @Query("""
            SELECT g FROM Guarantor g
            WHERE g.customerId = :customerId
            AND g.status = 'ACTIVE'
            ORDER BY g.createdAt DESC
            """)
    List<Guarantor> findActiveGuaranteesByCustomer(@Param("customerId") UUID customerId);

    /**
     * Find guarantors by customer and status.
     *
     * @param customerId the customer ID
     * @param status the guarantor status
     * @return list of guarantors matching both criteria
     */
    @Query("""
            SELECT g FROM Guarantor g
            WHERE g.customerId = :customerId
            AND g.status = :status
            ORDER BY g.createdAt DESC
            """)
    List<Guarantor> findByCustomerIdAndStatus(@Param("customerId") UUID customerId,
            @Param("status") GuarantorStatus status);

    /**
     * Count guarantors by loan account and status.
     *
     * @param loanAccountId the loan account ID
     * @param status the guarantor status
     * @return count of guarantors with the specified status
     */
    @Query("""
            SELECT COUNT(g) FROM Guarantor g
            WHERE g.loanAccount.id = :loanAccountId
            AND g.status = :status
            """)
    long countByLoanAccountIdAndStatus(@Param("loanAccountId") UUID loanAccountId,
            @Param("status") GuarantorStatus status);

    /**
     * Check if loan account has guarantor with specific status.
     *
     * @param loanAccountId the loan account ID
     * @param status the guarantor status
     * @return true if there are guarantors with the specified status
     */
    @Query("""
            SELECT COUNT(g) > 0 FROM Guarantor g
            WHERE g.loanAccount.id = :loanAccountId
            AND g.status = :status
            """)
    boolean existsByLoanAccountIdAndStatus(@Param("loanAccountId") UUID loanAccountId,
            @Param("status") GuarantorStatus status);

    /**
     * Check if customer is already a guarantor for a loan.
     *
     * @param loanAccountId the loan account ID
     * @param customerId the customer ID
     * @return true if customer is already a guarantor
     */
    @Query("""
            SELECT COUNT(g) > 0 FROM Guarantor g
            WHERE g.loanAccount.id = :loanAccountId
            AND g.customerId = :customerId
            """)
    boolean existsByLoanAccountIdAndCustomerId(@Param("loanAccountId") UUID loanAccountId,
            @Param("customerId") UUID customerId);
}
