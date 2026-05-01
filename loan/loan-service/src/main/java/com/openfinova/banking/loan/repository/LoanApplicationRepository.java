package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.ApplicationStatus;
import com.openfinova.banking.loan.entity.LoanApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LoanApplication entities.
 */
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    /**
     * Find loan application by application number.
     *
     * @param applicationNumber the unique application number
     * @return optional containing the application if found
     */
    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);

    /**
     * Find loan applications by customer ID.
     *
     * @param customerId the customer ID
     * @param pageable pagination information
     * @return page of loan applications for the customer
     */
    Page<LoanApplication> findByCustomerId(UUID customerId, Pageable pageable);

    /**
     * Find loan applications by customer ID (non-paginated).
     *
     * @param customerId the customer ID
     * @return list of loan applications for the customer
     */
    List<LoanApplication> findByCustomerId(UUID customerId);

    /**
     * Find loan applications by product ID.
     *
     * @param productId the product ID
     * @param pageable pagination information
     * @return page of loan applications for the product
     */
    Page<LoanApplication> findByProductId(UUID productId, Pageable pageable);

    /**
     * Find loan applications by status.
     *
     * @param status the application status
     * @param pageable pagination information
     * @return page of loan applications with the specified status
     */
    Page<LoanApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    /**
     * Find loan applications by customer and status.
     *
     * @param customerId the customer ID
     * @param status the application status
     * @return list of loan applications matching both criteria
     */
    List<LoanApplication> findByCustomerIdAndStatus(UUID customerId, ApplicationStatus status);

    /**
     * Find pending loan applications (submitted, under review, or underwriting).
     *
     * @param pageable pagination information
     * @return page of pending loan applications
     */
    @Query("""
            SELECT la FROM LoanApplication la
            WHERE la.status IN ('SUBMITTED', 'UNDER_REVIEW', 'UNDERWRITING')
            ORDER BY la.createdAt ASC
            """)
    Page<LoanApplication> findPendingApplications(Pageable pageable);

    /**
     * Find approved loan applications.
     *
     * @param pageable pagination information
     * @return page of approved loan applications
     */
    @Query("""
            SELECT la FROM LoanApplication la
            WHERE la.status = 'APPROVED'
            ORDER BY la.approvalDate DESC
            """)
    Page<LoanApplication> findApprovedApplications(Pageable pageable);

    /**
     * Find rejected loan applications.
     *
     * @param pageable pagination information
     * @return page of rejected loan applications
     */
    @Query("""
            SELECT la FROM LoanApplication la
            WHERE la.status = 'REJECTED'
            ORDER BY la.rejectionDate DESC
            """)
    Page<LoanApplication> findRejectedApplications(Pageable pageable);

    /**
     * Find loan applications created within a date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of loan applications created in the date range
     */
    @Query("""
            SELECT la FROM LoanApplication la
            WHERE la.createdAt BETWEEN :startDate AND :endDate
            ORDER BY la.createdAt DESC
            """)
    Page<LoanApplication> findApplicationsCreatedBetween(@Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate, Pageable pageable);

    /**
     * Find loan applications approved within a date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of loan applications approved in the date range
     */
    @Query("""
            SELECT la FROM LoanApplication la
            WHERE la.approvalDate BETWEEN :startDate AND :endDate
            ORDER BY la.approvalDate DESC
            """)
    Page<LoanApplication> findApplicationsApprovedBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find loan applications by risk rating.
     *
     * @param riskRating the risk rating
     * @param pageable pagination information
     * @return page of loan applications with the specified risk rating
     */
    Page<LoanApplication> findByRiskRating(String riskRating, Pageable pageable);

    /**
     * Find loan applications with credit score in range.
     *
     * @param minScore minimum credit score
     * @param maxScore maximum credit score
     * @param pageable pagination information
     * @return page of loan applications in the score range
     */
    @Query("""
            SELECT la FROM LoanApplication la
            WHERE la.creditScore BETWEEN :minScore AND :maxScore
            ORDER BY la.creditScore DESC
            """)
    Page<LoanApplication> findByCreditScoreBetween(@Param("minScore") BigDecimal minScore,
            @Param("maxScore") BigDecimal maxScore, Pageable pageable);

    /**
     * Find loan applications by currency.
     *
     * @param currency the currency code
     * @param pageable pagination information
     * @return page of loan applications in the specified currency
     */
    Page<LoanApplication> findByCurrency(String currency, Pageable pageable);

    /**
     * Find loan applications with requested amount greater than specified threshold.
     *
     * @param minAmount minimum requested amount
     * @param pageable pagination information
     * @return page of high-value loan applications
     */
    @Query("""
            SELECT la FROM LoanApplication la
            WHERE la.requestedAmount >= :minAmount
            ORDER BY la.requestedAmount DESC
            """)
    Page<LoanApplication> findByRequestedAmountGreaterThanEqual(@Param("minAmount") BigDecimal minAmount,
            Pageable pageable);

    /**
     * Find loan applications requiring guarantors.
     *
     * @param pageable pagination information
     * @return page of loan applications requiring guarantors
     */
    @Query("""
            SELECT la FROM LoanApplication la
            WHERE la.guarantorsRequired > 0
            AND SIZE(la.guarantors) < la.guarantorsRequired
            AND la.status = 'APPROVED'
            """)
    Page<LoanApplication> findApplicationsRequiringGuarantors(Pageable pageable);

    /**
     * Find loan applications with complete guarantor information.
     *
     * @param pageable pagination information
     * @return page of loan applications with all guarantors provided
     */
    @Query("""
            SELECT la FROM LoanApplication la
            WHERE la.guarantorsRequired > 0
            AND SIZE(la.guarantors) >= la.guarantorsRequired
            AND la.status = 'APPROVED'
            """)
    Page<LoanApplication> findApplicationsWithCompleteGuarantors(Pageable pageable);

    /**
     * Count loan applications by status.
     *
     * @param status the application status
     * @return count of loan applications with the specified status
     */
    long countByStatus(ApplicationStatus status);

    /**
     * Count loan applications by customer.
     *
     * @param customerId the customer ID
     * @return count of loan applications for the customer
     */
    long countByCustomerId(UUID customerId);

    /**
     * Count pending loan applications by customer.
     *
     * @param customerId the customer ID
     * @return count of pending loan applications
     */
    @Query("""
            SELECT COUNT(la) FROM LoanApplication la
            WHERE la.customerId = :customerId
            AND la.status IN ('SUBMITTED', 'UNDER_REVIEW', 'UNDERWRITING')
            """)
    long countPendingApplicationsByCustomer(@Param("customerId") UUID customerId);

    /**
     * Update application status.
     *
     * @param applicationId the application ID
     * @param newStatus the new status
     * @return number of applications updated
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE LoanApplication la
            SET la.status = :newStatus, la.updatedAt = :updatedAt
            WHERE la.id = :applicationId
            """)
    int updateApplicationStatus(@Param("applicationId") UUID applicationId,
            @Param("newStatus") ApplicationStatus newStatus, @Param("updatedAt") Instant updatedAt);

    /**
     * Find stale applications (pending for too long).
     *
     * @param cutoffDate cutoff date for staleness
     * @param pageable pagination information
     * @return page of stale applications
     */
    @Query("""
            SELECT la FROM LoanApplication la
            WHERE la.status IN ('SUBMITTED', 'UNDER_REVIEW', 'UNDERWRITING')
            AND la.createdAt <= :cutoffDate
            ORDER BY la.createdAt ASC
            """)
    Page<LoanApplication> findStaleApplications(@Param("cutoffDate") Instant cutoffDate, Pageable pageable);

    /**
     * Sum requested amounts by status.
     *
     * @param status the application status
     * @return total requested amount
     */
    @Query("""
            SELECT COALESCE(SUM(la.requestedAmount), 0)
            FROM LoanApplication la
            WHERE la.status = :status
            """)
    BigDecimal sumRequestedAmountByStatus(@Param("status") ApplicationStatus status);

    /**
     * Sum approved amounts.
     *
     * @return total approved amount
     */
    @Query("""
            SELECT COALESCE(SUM(la.approvedAmount), 0)
            FROM LoanApplication la
            WHERE la.status = 'APPROVED'
            """)
    BigDecimal sumApprovedAmounts();
}
