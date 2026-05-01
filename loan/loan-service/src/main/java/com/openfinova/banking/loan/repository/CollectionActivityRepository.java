package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.CollectionActivityType;
import com.openfinova.banking.loan.api.entity.CollectionStatus;
import com.openfinova.banking.loan.entity.CollectionActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CollectionActivity entities.
 */
public interface CollectionActivityRepository extends JpaRepository<CollectionActivity, UUID> {

    /**
     * Find all collection activities for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of collection activities for the loan account
     */
    @Query("""
            SELECT ca FROM CollectionActivity ca
            WHERE ca.loanAccount.id = :loanAccountId
            ORDER BY ca.activityDate DESC
            """)
    List<CollectionActivity> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find collection activities by loan account and activity type.
     *
     * @param loanAccountId the loan account ID
     * @param activityType the activity type
     * @return list of collection activities matching both criteria
     */
    @Query("""
            SELECT ca FROM CollectionActivity ca
            WHERE ca.loanAccount.id = :loanAccountId
            AND ca.activityType = :activityType
            ORDER BY ca.activityDate DESC
            """)
    List<CollectionActivity> findByLoanAccountIdAndActivityType(@Param("loanAccountId") UUID loanAccountId,
            @Param("activityType") CollectionActivityType activityType);

    /**
     * Find collection activities by loan account and status.
     *
     * @param loanAccountId the loan account ID
     * @param status the collection status
     * @return list of collection activities matching both criteria
     */
    @Query("""
            SELECT ca FROM CollectionActivity ca
            WHERE ca.loanAccount.id = :loanAccountId
            AND ca.status = :status
            ORDER BY ca.activityDate DESC
            """)
    List<CollectionActivity> findByLoanAccountIdAndStatus(@Param("loanAccountId") UUID loanAccountId,
            @Param("status") CollectionStatus status);

    /**
     * Find collection activities by activity type.
     *
     * @param activityType the activity type
     * @param pageable pagination information
     * @return page of collection activities of the specified type
     */
    Page<CollectionActivity> findByActivityType(CollectionActivityType activityType, Pageable pageable);

    /**
     * Find collection activities by status.
     *
     * @param status the collection status
     * @param pageable pagination information
     * @return page of collection activities with the specified status
     */
    Page<CollectionActivity> findByStatus(CollectionStatus status, Pageable pageable);

    Page<CollectionActivity> findByLoanAccount_IdAndStatus(UUID loanAccountId, CollectionStatus status,
            Pageable pageable);

    long countByLoanAccount_IdAndStatus(UUID loanAccountId, CollectionStatus status);

    /**
     * Find collection activities by date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of collection activities in the date range
     */
    @Query("""
            SELECT ca FROM CollectionActivity ca
            WHERE ca.activityDate BETWEEN :startDate AND :endDate
            ORDER BY ca.activityDate DESC
            """)
    Page<CollectionActivity> findByActivityDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("""
            SELECT ca FROM CollectionActivity ca
            WHERE ca.loanAccount.id = :loanAccountId
            AND ca.activityDate BETWEEN :startDate AND :endDate
            ORDER BY ca.activityDate DESC
            """)
    Page<CollectionActivity> findByLoanAccountIdAndActivityDateBetweenPage(@Param("loanAccountId") UUID loanAccountId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find pending collection activities.
     *
     * @param pageable pagination information
     * @return page of pending collection activities
     */
    @Query("""
            SELECT ca FROM CollectionActivity ca
            WHERE ca.status = 'PENDING'
            ORDER BY ca.activityDate ASC
            """)
    Page<CollectionActivity> findPendingActivities(Pageable pageable);

    /**
     * Find collection activities with scheduled follow-up.
     *
     * @param followUpDate the follow-up date
     * @param pageable pagination information
     * @return page of collection activities with follow-up on the date
     */
    @Query("""
            SELECT ca FROM CollectionActivity ca
            WHERE ca.followUpDate = :followUpDate
            AND ca.status IN ('PENDING', 'IN_PROGRESS')
            ORDER BY ca.activityDate ASC
            """)
    Page<CollectionActivity> findActivitiesWithFollowUpOnDate(@Param("followUpDate") LocalDate followUpDate,
            Pageable pageable);

    /**
     * Find overdue follow-ups.
     *
     * @param currentDate the current date
     * @param pageable pagination information
     * @return page of collection activities with overdue follow-ups
     */
    @Query("""
            SELECT ca FROM CollectionActivity ca
            WHERE ca.followUpDate < :currentDate
            AND ca.status IN ('PENDING', 'IN_PROGRESS')
            ORDER BY ca.followUpDate ASC
            """)
    Page<CollectionActivity> findOverdueFollowUps(@Param("currentDate") LocalDate currentDate, Pageable pageable);

    /**
     * Find latest collection activity for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return optional containing the latest collection activity if found
     */
    @Query("""
            SELECT ca FROM CollectionActivity ca
            WHERE ca.loanAccount.id = :loanAccountId
            ORDER BY ca.activityDate DESC, ca.createdAt DESC
            LIMIT 1
            """)
    Optional<CollectionActivity> findLatestActivityByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find latest collection activity for a loan account (alternative method name).
     *
     * @param loanAccountId the loan account ID
     * @return optional containing the latest collection activity if found
     */
    @Query("""
            SELECT ca FROM CollectionActivity ca
            WHERE ca.loanAccount.id = :loanAccountId
            ORDER BY ca.activityDate DESC
            LIMIT 1
            """)
    Optional<CollectionActivity> findTopByLoanAccountIdOrderByActivityDateDesc(
            @Param("loanAccountId") UUID loanAccountId);

    /**
     * Find collection activities by follow-up date.
     *
     * @param followUpDate the follow-up date
     * @param pageable pagination information
     * @return page of collection activities with the specified follow-up date
     */
    @Query("""
            SELECT ca FROM CollectionActivity ca
            WHERE ca.followUpDate = :followUpDate
            ORDER BY ca.activityDate ASC
            """)
    Page<CollectionActivity> findByFollowUpDate(@Param("followUpDate") LocalDate followUpDate, Pageable pageable);

    /**
     * Find collection activities by follow-up date before and status.
     *
     * @param followUpDate the follow-up date
     * @param status the collection status
     * @param pageable pagination information
     * @return page of collection activities matching criteria
     */
    @Query("""
            SELECT ca FROM CollectionActivity ca
            WHERE ca.followUpDate < :followUpDate
            AND ca.status = :status
            ORDER BY ca.followUpDate ASC
            """)
    Page<CollectionActivity> findByFollowUpDateBeforeAndStatus(@Param("followUpDate") LocalDate followUpDate,
            @Param("status") CollectionStatus status, Pageable pageable);

    /**
     * Find collection activities by loan account and date range.
     *
     * @param loanAccountId the loan account ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of collection activities in the date range
     */
    @Query("""
            SELECT ca FROM CollectionActivity ca
            WHERE ca.loanAccount.id = :loanAccountId
            AND ca.activityDate BETWEEN :startDate AND :endDate
            ORDER BY ca.activityDate DESC
            """)
    List<CollectionActivity> findByLoanAccountIdAndActivityDateBetween(@Param("loanAccountId") UUID loanAccountId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Check if loan account has collection activities with specific status.
     *
     * @param loanAccountId the loan account ID
     * @param status the collection status
     * @return true if there are activities with the specified status
     */
    @Query("""
            SELECT COUNT(ca) > 0 FROM CollectionActivity ca
            WHERE ca.loanAccount.id = :loanAccountId
            AND ca.status = :status
            """)
    boolean existsByLoanAccountIdAndStatus(@Param("loanAccountId") UUID loanAccountId,
            @Param("status") CollectionStatus status);

    /**
     * Count collection activities for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of collection activities
     */
    @Query("""
            SELECT COUNT(ca) FROM CollectionActivity ca
            WHERE ca.loanAccount.id = :loanAccountId
            """)
    long countByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Count collection activities by type for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @param activityType the activity type
     * @return count of collection activities
     */
    @Query("""
            SELECT COUNT(ca) FROM CollectionActivity ca
            WHERE ca.loanAccount.id = :loanAccountId
            AND ca.activityType = :activityType
            """)
    long countByLoanAccountIdAndActivityType(@Param("loanAccountId") UUID loanAccountId,
            @Param("activityType") CollectionActivityType activityType);

    /**
     * Check if loan account has pending collection activities.
     *
     * @param loanAccountId the loan account ID
     * @return true if there are pending activities
     */
    @Query("""
            SELECT COUNT(ca) > 0 FROM CollectionActivity ca
            WHERE ca.loanAccount.id = :loanAccountId
            AND ca.status = 'PENDING'
            """)
    boolean hasPendingActivities(@Param("loanAccountId") UUID loanAccountId);
}
