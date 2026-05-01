package com.openfinova.banking.gl.repository;

import com.openfinova.banking.gl.api.entity.EscalationLevel;
import com.openfinova.banking.gl.entity.SuspenseEscalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for SuspenseEscalation entity operations.
 */
@Repository
public interface SuspenseEscalationRepository extends JpaRepository<SuspenseEscalation, UUID> {

    /**
     * Find escalations by suspense item.
     */
    @Query("""
            SELECT e FROM SuspenseEscalation e
            WHERE e.suspenseItem.id = :suspenseItemId
            ORDER BY e.escalationLevel DESC
            """)
    List<SuspenseEscalation> findBySuspenseItemId(@Param("suspenseItemId") UUID suspenseItemId);

    /**
     * Find unresolved escalations.
     */
    List<SuspenseEscalation> findByIsResolvedFalseOrderByDueDateAsc();

    /**
     * Find unresolved escalations assigned to a user.
     */
    List<SuspenseEscalation> findByAssignedToAndIsResolvedFalseOrderByDueDateAsc(String assignedTo);

    /**
     * Find escalations by level.
     */
    List<SuspenseEscalation> findByEscalationLevelAndIsResolvedFalseOrderByDueDateAsc(EscalationLevel level);

    /**
     * Find overdue escalations (past due date and not resolved).
     */
    @Query("""
            SELECT e FROM SuspenseEscalation e
            WHERE e.isResolved = false
              AND e.dueDate < :today
            ORDER BY e.dueDate ASC
            """)
    List<SuspenseEscalation> findOverdueEscalations(@Param("today") LocalDate today);

    /**
     * Find escalations due soon (within next N days).
     */
    @Query("""
            SELECT e FROM SuspenseEscalation e
            WHERE e.isResolved = false
              AND e.dueDate BETWEEN :today AND :futureDate
            ORDER BY e.dueDate ASC
            """)
    List<SuspenseEscalation> findEscalationsDueSoon(@Param("today") LocalDate today,
            @Param("futureDate") LocalDate futureDate);

    /**
     * Find SLA breached escalations.
     */
    List<SuspenseEscalation> findBySlaBreachedTrueAndIsResolvedFalseOrderByEscalatedDateAsc();

    /**
     * Count unresolved escalations by level.
     */
    @Query("""
            SELECT e.escalationLevel, COUNT(e)
            FROM SuspenseEscalation e
            WHERE e.isResolved = false
            GROUP BY e.escalationLevel
            """)
    List<Object[]> countUnresolvedEscalationsByLevel();
}
