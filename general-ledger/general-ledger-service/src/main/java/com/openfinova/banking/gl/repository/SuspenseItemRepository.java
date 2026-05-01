package com.openfinova.banking.gl.repository;

import com.openfinova.banking.gl.api.entity.SuspenseStatus;
import com.openfinova.banking.gl.entity.SuspenseItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for SuspenseItem entity operations.
 */
@Repository
public interface SuspenseItemRepository extends JpaRepository<SuspenseItem, UUID> {

    /**
     * Find all suspense items by status.
     */
    List<SuspenseItem> findByStatus(SuspenseStatus status);

    /**
     * Find all suspense items by status with pagination.
     */
    Page<SuspenseItem> findByStatus(SuspenseStatus status, Pageable pageable);

    /**
     * Find all active suspense items (not cleared/written off/cancelled).
     */
    @Query("""
            SELECT s FROM SuspenseItem s
            WHERE s.status IN ('PENDING', 'UNDER_INVESTIGATION', 'ESCALATED')
            """)
    List<SuspenseItem> findAllActive();

    /**
     * Find suspense items assigned to a specific user.
     */
    List<SuspenseItem> findByAssignedTo(String assignedTo);

    /**
     * Find suspense items by source system.
     */
    List<SuspenseItem> findBySourceSystem(String sourceSystem);

    /**
     * Find suspense items by currency.
     */
    List<SuspenseItem> findByCurrency(String currency);

    /**
     * Find suspense items posted before a specific date (for aging analysis).
     */
    @Query("""
            SELECT s FROM SuspenseItem s
            WHERE s.postingDate <= :cutoffDate
              AND s.status IN ('PENDING', 'UNDER_INVESTIGATION', 'ESCALATED')
            """)
    List<SuspenseItem> findItemsOlderThan(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Find suspense items requiring AML review.
     */
    @Query("""
            SELECT s FROM SuspenseItem s
            WHERE s.reasonCode IN ('UNIDENTIFIED_DEPOSIT', 'OTHER')
              AND s.status IN ('PENDING', 'UNDER_INVESTIGATION', 'ESCALATED')
            """)
    List<SuspenseItem> findItemsRequiringAMLReview();

    /**
     * Find suspense items by GL transaction.
     */
    @Query("""
            SELECT s FROM SuspenseItem s
            WHERE s.glTransaction.id = :transactionId
            """)
    List<SuspenseItem> findByGLTransactionId(@Param("transactionId") UUID transactionId);

    /**
     * Count active suspense items.
     */
    @Query("""
            SELECT COUNT(s) FROM SuspenseItem s
            WHERE s.status IN ('PENDING', 'UNDER_INVESTIGATION', 'ESCALATED')
            """)
    Long countActiveSuspenseItems();

    /**
     * Sum of active suspense items by currency.
     */
    @Query("""
            SELECT s.currency, SUM(s.amount)
            FROM SuspenseItem s
            WHERE s.status IN ('PENDING', 'UNDER_INVESTIGATION', 'ESCALATED')
            GROUP BY s.currency
            """)
    List<Object[]> sumActiveSuspenseBycurrencies();

    /**
     * Find escalated items (status = ESCALATED).
     */
    List<SuspenseItem> findByStatusOrderByPostingDateAsc(SuspenseStatus status);

    /**
     * Complex filter query for search functionality.
     */
    @Query("""
            SELECT s FROM SuspenseItem s
            WHERE (:status IS NULL OR s.status = :status)
              AND (:assignedTo IS NULL OR s.assignedTo = :assignedTo)
              AND (:sourceSystem IS NULL OR s.sourceSystem = :sourceSystem)
              AND (:currency IS NULL OR s.currency = :currency)
              AND (:fromDate IS NULL OR s.postingDate >= :fromDate)
              AND (:toDate IS NULL OR s.postingDate <= :toDate)
            """)
    Page<SuspenseItem> findByFilters(@Param("status") SuspenseStatus status, @Param("assignedTo") String assignedTo,
            @Param("sourceSystem") String sourceSystem, @Param("currency") String currency,
            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate, Pageable pageable);

    /**
     * Find items with amount greater than threshold.
     */
    @Query("""
            SELECT s FROM SuspenseItem s
            WHERE s.amount > :threshold
              AND s.status IN ('PENDING', 'UNDER_INVESTIGATION', 'ESCALATED')
            """)
    List<SuspenseItem> findItemsAboveThreshold(@Param("threshold") BigDecimal threshold);
}
