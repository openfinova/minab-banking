package com.openfinova.banking.gl.repository;

import com.openfinova.banking.gl.api.entity.GLAuditAction;
import com.openfinova.banking.gl.api.entity.GLEntityType;
import com.openfinova.banking.gl.entity.GLAuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for accessing audit trail records.
 * Supports regulatory compliance queries for SOX, Basel III, and IFRS requirements.
 *
 * IMPORTANT: This repository should only support READ operations.
 * No UPDATE or DELETE operations should be performed on audit records
 * to maintain audit trail integrity.
 */
public interface GLAuditTrailRepository extends JpaRepository<GLAuditTrail, UUID> {

    /**
     * Find all audit records for a specific entity, ordered by time (newest first).
     * Use this for entity change history and regulatory review.
     *
     * @param entityType the type of entity (e.g., GL_ACCOUNT, GL_TRANSACTION)
     * @param entityId the UUID of the specific entity
     * @return list of audit records in chronological order (newest first)
     */
    List<GLAuditTrail> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(GLEntityType entityType, UUID entityId);

    /**
     * Find all audit records for a specific user within a time range.
     * Use this for user activity reports and access review.
     *
     * @param performedBy username
     * @param startTime start of time range (inclusive)
     * @param endTime end of time range (inclusive)
     * @return list of audit records for the user
     */
    List<GLAuditTrail> findByPerformedByAndPerformedAtBetween(String performedBy, Instant startTime, Instant endTime);

    /**
     * Find all audit records for a specific action within a time range.
     * Use this for action-specific reporting (e.g., all reversals in a period).
     *
     * @param action the action type (e.g., REVERSE, DELETE)
     * @param startTime start of time range (inclusive)
     * @param endTime end of time range (inclusive)
     * @return list of audit records for the action
     */
    List<GLAuditTrail> findByActionAndPerformedAtBetween(GLAuditAction action, Instant startTime, Instant endTime);

    /**
     * Find all audit records for a set of actions within a time range.
     * Use this for high-risk action monitoring (REVERSE, DELETE, PERIOD_REOPEN, etc.).
     *
     * @param actions set of action types to filter
     * @param startTime start of time range (inclusive)
     * @param endTime end of time range (inclusive)
     * @return list of audit records for the specified actions
     */
    List<GLAuditTrail> findByActionInAndPerformedAtBetween(Set<GLAuditAction> actions, Instant startTime,
            Instant endTime);

    /**
     * Find all audit records that lack a reason for mandatory-reason actions.
     * Use this for compliance violation detection.
     *
     * @param actions set of actions that require mandatory reason
     * @param startTime start of time range (inclusive)
     * @param endTime end of time range (inclusive)
     * @return list of non-compliant audit records (missing required reason)
     */
    @Query("""
            SELECT a FROM GLAuditTrail a
            WHERE a.action IN :actions
            AND (a.reason IS NULL OR LENGTH(TRIM(a.reason)) < 10)
            AND a.performedAt BETWEEN :startTime AND :endTime
            """)
    List<GLAuditTrail> findByReasonIsNullAndActionInAndPerformedAtBetween(@Param("actions") Set<GLAuditAction> actions,
            @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * Find all audit records with transaction amount greater than threshold.
     * Use this for materiality analysis (e.g., "show all reversals > $1M").
     *
     * @param threshold minimum transaction amount
     * @param action the action type to filter
     * @param startTime start of time range (inclusive)
     * @param endTime end of time range (inclusive)
     * @return list of high-value audit records
     */
    List<GLAuditTrail> findByTransactionAmountGreaterThanAndActionAndPerformedAtBetween(BigDecimal threshold,
            GLAuditAction action, Instant startTime, Instant endTime);

    /**
     * Find all audit records with a specific correlation ID.
     * Use this for tracing multi-step operations (e.g., all changes in a period close).
     *
     * @param correlationId the correlation ID linking related audit entries
     * @return list of correlated audit records
     */
    List<GLAuditTrail> findByCorrelationIdOrderByPerformedAtAsc(UUID correlationId);

    /**
     * Count audit records for a specific entity.
     * Use this for performance optimization before loading full history.
     *
     * @param entityType the type of entity
     * @param entityId the UUID of the entity
     * @return count of audit records
     */
    long countByEntityTypeAndEntityId(GLEntityType entityType, UUID entityId);

    /**
     * Find recent audit records (last N hours) for operational monitoring.
     *
     * @param since timestamp to filter from
     * @return list of recent audit records ordered by time (newest first)
     */
    List<GLAuditTrail> findByPerformedAtAfterOrderByPerformedAtDesc(Instant since);
}
