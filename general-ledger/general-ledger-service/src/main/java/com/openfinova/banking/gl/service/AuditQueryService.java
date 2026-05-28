package com.openfinova.banking.gl.service;

import com.openfinova.banking.gl.api.entity.GLAuditAction;
import com.openfinova.banking.gl.api.entity.GLEntityType;
import com.openfinova.banking.gl.entity.GLAuditTrail;
import com.openfinova.banking.gl.repository.GLAuditTrailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for querying audit trail records for regulatory reporting and compliance.
 * All methods are read-only to maintain audit trail integrity.
 *
 * This service supports various regulatory requirements:
 * - Entity change history (SOX compliance)
 * - User activity reports (access review)
 * - High-risk action monitoring (Basel III)
 * - Compliance violation detection (missing required reasons)
 * - Materiality analysis (large amount changes)
 * - Multi-step operation tracing (correlation ID)
 */
@Service
@Transactional(readOnly = true)
public class AuditQueryService {

    private static final Logger logger = LoggerFactory.getLogger(AuditQueryService.class);

    /**
     * High-risk actions that require special monitoring and review.
     */
    private static final Set<GLAuditAction> HIGH_RISK_ACTIONS = Set.of(
            GLAuditAction.REVERSE,
            GLAuditAction.DELETE,
            GLAuditAction.PERIOD_REOPEN,
            GLAuditAction.BALANCE_ADJUSTMENT);

    /**
     * Actions that require mandatory reason (same as in AuditService).
     */
    private static final Set<GLAuditAction> MANDATORY_REASON_ACTIONS = Set.of(
            GLAuditAction.REVERSE,
            GLAuditAction.DELETE,
            GLAuditAction.STATUS_CHANGE,
            GLAuditAction.PERIOD_REOPEN,
            GLAuditAction.BALANCE_ADJUSTMENT,
            GLAuditAction.REJECTION);

    private final GLAuditTrailRepository auditTrailRepository;

    public AuditQueryService(GLAuditTrailRepository auditTrailRepository) {
        this.auditTrailRepository = auditTrailRepository;
    }

    /**
     * Get complete change history for a specific entity.
     * Use this for regulatory review of account or transaction changes.
     *
     * @param entityType type of entity (e.g., GL_ACCOUNT, GL_TRANSACTION)
     * @param entityId UUID of the entity
     * @return list of audit records in chronological order (newest first)
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public List<GLAuditTrail> getEntityHistory(GLEntityType entityType, UUID entityId) {
        logger.debug("Retrieving audit history for {} {}", entityType, entityId);
        return auditTrailRepository.findByEntityTypeAndEntityIdOrderByPerformedAtDesc(entityType, entityId);
    }

    /**
     * Get user activity within a time range for access review.
     *
     * @param username the username to query
     * @param from start date (inclusive)
     * @param to end date (inclusive)
     * @return list of audit records for the user
     */
    public List<GLAuditTrail> getUserAuditTrail(String username, Instant from, Instant to) {
        logger.debug("Retrieving audit trail for user {} from {} to {}", username, from, to);
        return auditTrailRepository.findByPerformedByAndPerformedAtBetween(username, from, to);
    }

    /**
     * Get all changes on a specific date for daily reconciliation.
     *
     * @param date the date to query
     * @return list of audit records for the date
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public List<GLAuditTrail> getRecentChanges(LocalDate date) {
        Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = date.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        logger.debug("Retrieving changes for date {}", date);
        return auditTrailRepository.findByPerformedAtAfterOrderByPerformedAtDesc(startOfDay).stream()
                .filter(audit -> audit.getPerformedAt().isBefore(endOfDay) || audit.getPerformedAt().equals(endOfDay))
                .toList();
    }

    /**
     * Get all transaction reversals in a period for regulatory review.
     * Critical for audit committees and external auditors.
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of reversal audit records
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public List<GLAuditTrail> getReversalsInPeriod(LocalDate startDate, LocalDate endDate) {
        Instant start = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = endDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        logger.debug("Retrieving REVERSE actions from {} to {}", startDate, endDate);
        return auditTrailRepository.findByActionAndPerformedAtBetween(GLAuditAction.REVERSE, start, end);
    }

    /**
     * Get user activity report for a date range.
     * Use this for user access review and compliance audits.
     *
     * @param username the username to query
     * @param from start date (inclusive)
     * @param to end date (inclusive)
     * @return list of audit records for the user
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public List<GLAuditTrail> getUserActivityReport(String username, LocalDate from, LocalDate to) {
        Instant startTime = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endTime = to.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        return getUserAuditTrail(username, startTime, endTime);
    }

    /**
     * Get all high-risk actions in a period for compliance monitoring.
     * High-risk actions: REVERSE, DELETE, PERIOD_REOPEN, BALANCE_ADJUSTMENT.
     *
     * @param from start date (inclusive)
     * @param to end date (inclusive)
     * @return list of high-risk audit records
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public List<GLAuditTrail> getHighRiskActions(LocalDate from, LocalDate to) {
        Instant startTime = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endTime = to.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        logger.debug("Retrieving high-risk actions from {} to {}", from, to);
        return auditTrailRepository.findByActionInAndPerformedAtBetween(HIGH_RISK_ACTIONS, startTime, endTime);
    }

    /**
     * Get all changes that lack required reason (compliance violations).
     * Use this to identify and remediate non-compliant audit records.
     *
     * @param from start date (inclusive)
     * @param to end date (inclusive)
     * @return list of non-compliant audit records
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public List<GLAuditTrail> getChangesWithoutReason(LocalDate from, LocalDate to) {
        Instant startTime = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endTime = to.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        logger.warn("Querying for potentially non-compliant audit records (missing required reason)");
        return auditTrailRepository
                .findByReasonIsNullAndActionInAndPerformedAtBetween(MANDATORY_REASON_ACTIONS, startTime, endTime);
    }

    /**
     * Get all changes involving amounts greater than threshold.
     * Use this for materiality analysis (e.g., "all reversals > $1M").
     *
     * @param threshold minimum transaction amount
     * @param from start date (inclusive)
     * @param to end date (inclusive)
     * @return list of high-value audit records
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public List<GLAuditTrail> getLargeAmountChanges(BigDecimal threshold, LocalDate from, LocalDate to) {
        Instant startTime = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endTime = to.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        logger.debug("Retrieving changes with amount > {} from {} to {}", threshold, from, to);

        // Query for each relevant action type that might have amounts
        List<GLAuditTrail> reversals = auditTrailRepository
                .findByTransactionAmountGreaterThanAndActionAndPerformedAtBetween(
                        threshold,
                        GLAuditAction.REVERSE,
                        startTime,
                        endTime);

        return reversals; // Can be extended to include other action types
    }

    /**
     * Get all audit records linked by correlation ID.
     * Use this to trace complete multi-step operations (e.g., period close).
     *
     * @param correlationId UUID linking related audit entries
     * @return list of correlated audit records in chronological order
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public List<GLAuditTrail> getCorrelatedAuditTrail(UUID correlationId) {
        logger.debug("Retrieving correlated audit trail for correlation ID {}", correlationId);
        return auditTrailRepository.findByCorrelationIdOrderByPerformedAtAsc(correlationId);
    }

    /**
     * Count audit records for an entity (for performance checks before loading full history).
     *
     * @param entityType type of entity
     * @param entityId UUID of entity
     * @return count of audit records
     */
    public long getEntityAuditCount(GLEntityType entityType, UUID entityId) {
        return auditTrailRepository.countByEntityTypeAndEntityId(entityType, entityId);
    }
}
