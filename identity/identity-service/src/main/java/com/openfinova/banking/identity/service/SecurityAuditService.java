package com.openfinova.banking.identity.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.identity.api.audit.AuditActor;
import com.openfinova.banking.identity.audit.AuditEventDetail;
import com.openfinova.banking.identity.audit.SecurityAuditExtensions;
import com.openfinova.banking.identity.entity.SecurityAuditEvent;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.repository.SecurityAuditEventRepository;

/**
 * Records and queries security-relevant events for the identity module, including logins,
 * lockouts, password changes, and role mutations.
 *
 * Two transaction strategies are provided: record (REQUIRES_NEW) suspends the caller's
 * transaction so that audit events are persisted even when the surrounding business operation
 * rolls back — use this from authentication filters and login handlers. recordParticipating
 * (MANDATORY) joins the caller's transaction so the audit row commits or rolls back together
 * with the business operation — use this from transactional service methods.
 *
 * TODO (Regulatory — Audit Trail Retention): Banking regulations require audit logs to be
 * retained for 7+ years. Implement an AuditRetentionArchiveJob (scheduled task) that moves
 * rows older than the configured retention period from identity_audit_events into an
 * identity_audit_events_archive table. Add an AuditRetentionProperties configuration class
 * (prefix identity.audit) to control the retention period. Also add a DB-level trigger or
 * row-level security policy to prevent direct DELETE/UPDATE on identity_audit_events,
 * bypassing the JPA @Immutable guard.
 */
@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);

    private final SecurityAuditEventRepository repository;

    public SecurityAuditService(SecurityAuditEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Persists a security audit event in a new independent transaction.
     *
     * The caller's transaction is suspended so that this record is committed regardless of
     * whether the surrounding business operation succeeds or rolls back. Suitable for login
     * flows and authentication filters where failure events must always be retained.
     *
     * @param eventType  the category of security event being recorded
     * @param userId     the UUID of the subject user; may be null for unauthenticated events
     * @param username   the login name of the subject user; may be null
     * @param ipAddress  the client IP address from the request; may be null
     * @param userAgent  the client user-agent string from the request; may be null
     * @param details    human-readable description of the event
     * @return the persisted SecurityAuditEvent entity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityAuditEvent record(SecurityAuditEventType eventType, UUID userId, String username, String ipAddress,
            String userAgent, String details) {
        return insertEvent(
                eventType,
                userId,
                username,
                ipAddress,
                userAgent,
                details,
                SecurityAuditExtensions.NONE,
                null);
    }

    /**
     * Persists a security audit event with a structured audit detail payload in a new
     * independent transaction.
     *
     * Extends the base record overload by attaching an AuditEventDetail object that carries
     * operation-specific structured data alongside the free-text description.
     *
     * @param eventType  the category of security event being recorded
     * @param userId     the UUID of the subject user; may be null for unauthenticated events
     * @param username   the login name of the subject user; may be null
     * @param ipAddress  the client IP address from the request; may be null
     * @param userAgent  the client user-agent string from the request; may be null
     * @param details    human-readable description of the event
     * @param detail     structured event-specific payload; may be null
     * @return the persisted SecurityAuditEvent entity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityAuditEvent record(SecurityAuditEventType eventType, UUID userId, String username, String ipAddress,
            String userAgent, String details, AuditEventDetail detail) {
        return insertEvent(
                eventType,
                userId,
                username,
                ipAddress,
                userAgent,
                details,
                SecurityAuditExtensions.NONE,
                detail);
    }

    /**
     * Persists a security audit event with custom extension metadata in a new independent
     * transaction.
     *
     * Extends the base record overload by attaching a SecurityAuditExtensions object that
     * carries change-tracking fields such as previous value, current value, and approver
     * information.
     *
     * @param eventType   the category of security event being recorded
     * @param userId      the UUID of the subject user; may be null for unauthenticated events
     * @param username    the login name of the subject user; may be null
     * @param ipAddress   the client IP address from the request; may be null
     * @param userAgent   the client user-agent string from the request; may be null
     * @param details     human-readable description of the event
     * @param extensions  additional change-tracking metadata; null is treated as NONE
     * @return the persisted SecurityAuditEvent entity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityAuditEvent record(SecurityAuditEventType eventType, UUID userId, String username, String ipAddress,
            String userAgent, String details, SecurityAuditExtensions extensions) {
        return insertEvent(
                eventType,
                userId,
                username,
                ipAddress,
                userAgent,
                details,
                extensions != null ? extensions : SecurityAuditExtensions.NONE,
                null);
    }

    /**
     * Convenience overload for server-side administrative operations where no HTTP request
     * context is available, so IP address and user-agent are omitted.
     *
     * Runs in a new independent transaction so the event is always committed.
     *
     * @param eventType  the category of security event being recorded
     * @param userId     the UUID of the subject user
     * @param username   the login name of the subject user
     * @param details    human-readable description of the event
     * @return the persisted SecurityAuditEvent entity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityAuditEvent record(SecurityAuditEventType eventType, UUID userId, String username, String details) {
        return insertEvent(eventType, userId, username, null, null, details, SecurityAuditExtensions.NONE, null);
    }

    /**
     * Convenience overload for server-side administrative operations where no HTTP request
     * context is available, with a structured audit detail payload.
     *
     * Runs in a new independent transaction so the event is always committed.
     *
     * @param eventType  the category of security event being recorded
     * @param userId     the UUID of the subject user
     * @param username   the login name of the subject user
     * @param details    human-readable description of the event
     * @param detail     structured event-specific payload; may be null
     * @return the persisted SecurityAuditEvent entity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityAuditEvent record(SecurityAuditEventType eventType, UUID userId, String username, String details,
            AuditEventDetail detail) {
        return insertEvent(eventType, userId, username, null, null, details, SecurityAuditExtensions.NONE, detail);
    }

    /**
     * Records a rejected role-assignment decision in a new independent transaction so the
     * event is persisted even when the surrounding business transaction rolls back.
     *
     * Builds a SecurityAuditExtensions snapshot from the supplied actor so the reviewer who
     * rejected the assignment is captured alongside the target user in the audit record.
     *
     * @param eventType       the category of security event; typically ROLE_ASSIGNMENT_REJECTED
     * @param targetUserId    the UUID of the user whose role assignment was rejected
     * @param targetUsername  the login name of that user
     * @param details         human-readable description of the rejection reason
     * @param actor           the authenticated actor who performed the rejection; may be null
     * @return the persisted SecurityAuditEvent entity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityAuditEvent recordRoleAssignmentRejected(SecurityAuditEventType eventType, UUID targetUserId,
            String targetUsername, String details, AuditActor actor) {
        SecurityAuditExtensions ext = SecurityAuditExtensions.changeTracking(
                actor != null ? actor.userId() : null,
                actor != null ? actor.username() : null,
                null,
                details);
        return insertEvent(eventType, targetUserId, targetUsername, null, null, details, ext, null);
    }

    /**
     * Persists a security audit event by joining the caller's existing transaction.
     *
     * The event commits or rolls back atomically with the surrounding business operation. A
     * pre-existing transaction is required; this method will throw if none is active. Suitable
     * for service-layer operations such as user creation where the audit row must not outlive
     * the entity it describes.
     *
     * @param eventType  the category of security event being recorded
     * @param userId     the UUID of the subject user; may be null
     * @param username   the login name of the subject user; may be null
     * @param ipAddress  the client IP address from the request; may be null
     * @param userAgent  the client user-agent string from the request; may be null
     * @param details    human-readable description of the event
     * @return the persisted SecurityAuditEvent entity
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public SecurityAuditEvent recordParticipating(SecurityAuditEventType eventType, UUID userId, String username,
            String ipAddress, String userAgent, String details) {
        return insertEvent(
                eventType,
                userId,
                username,
                ipAddress,
                userAgent,
                details,
                SecurityAuditExtensions.NONE,
                null);
    }

    /**
     * Convenience overload of recordParticipating for server-side operations where no HTTP
     * request context (IP address, user-agent) is available, without an explicit actor.
     *
     * Delegates to the three-parameter actor overload with actor set to null.
     *
     * @param eventType  the category of security event being recorded
     * @param userId     the UUID of the subject user
     * @param username   the login name of the subject user
     * @param details    human-readable description of the event
     * @return the persisted SecurityAuditEvent entity
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public SecurityAuditEvent recordParticipating(SecurityAuditEventType eventType, UUID userId, String username,
            String details) {
        return recordParticipating(eventType, userId, username, details, null);
    }

    /**
     * Convenience overload of recordParticipating for server-side operations without an
     * HTTP request context, with a plain actor and no custom extensions.
     *
     * Captures the actor's identity in the extension metadata using NONE extensions as
     * baseline, then delegates to the full extensions overload.
     *
     * @param eventType  the category of security event being recorded
     * @param userId     the UUID of the subject user
     * @param username   the login name of the subject user
     * @param details    human-readable description of the event
     * @param actor      the authenticated actor performing the operation; may be null
     * @return the persisted SecurityAuditEvent entity
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public SecurityAuditEvent recordParticipating(SecurityAuditEventType eventType, UUID userId, String username,
            String details, AuditActor actor) {
        return recordParticipating(eventType, userId, username, details, actor, SecurityAuditExtensions.NONE);
    }

    /**
     * Convenience overload of recordParticipating for server-side operations without an HTTP
     * request context, with both an actor and custom extension metadata.
     *
     * Merges the actor identity into the extensions before persisting so that changedByUserId
     * and changedByUsername are always populated from the actor when available.
     *
     * @param eventType   the category of security event being recorded
     * @param userId      the UUID of the subject user
     * @param username    the login name of the subject user
     * @param details     human-readable description of the event
     * @param actor       the authenticated actor performing the operation; may be null
     * @param extensions  additional change-tracking metadata; null is treated as NONE
     * @return the persisted SecurityAuditEvent entity
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public SecurityAuditEvent recordParticipating(SecurityAuditEventType eventType, UUID userId, String username,
            String details, AuditActor actor, SecurityAuditExtensions extensions) {
        return insertEvent(eventType, userId, username, null, null, details, mergeActor(actor, extensions), null);
    }

    /**
     * Full-detail overload of recordParticipating for server-side operations, combining an
     * actor, custom extension metadata, and a structured audit detail payload.
     *
     * All change-tracking fields, the actor identity, and operation-specific structured data
     * are captured in a single audit row.
     *
     * @param eventType   the category of security event being recorded
     * @param userId      the UUID of the subject user
     * @param username    the login name of the subject user
     * @param details     human-readable description of the event
     * @param actor       the authenticated actor performing the operation; may be null
     * @param extensions  additional change-tracking metadata; null is treated as NONE
     * @param detail      structured event-specific payload; may be null
     * @return the persisted SecurityAuditEvent entity
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public SecurityAuditEvent recordParticipating(SecurityAuditEventType eventType, UUID userId, String username,
            String details, AuditActor actor, SecurityAuditExtensions extensions, AuditEventDetail detail) {
        return insertEvent(eventType, userId, username, null, null, details, mergeActor(actor, extensions), detail);
    }

    /**
     * Full HTTP-context overload of recordParticipating that includes IP address and
     * user-agent alongside an actor, joining the caller's existing transaction.
     *
     * Use this variant when the audit event originates from an HTTP-scoped service call where
     * request metadata is available and the actor identity should be captured.
     *
     * @param eventType  the category of security event being recorded
     * @param userId     the UUID of the subject user
     * @param username   the login name of the subject user
     * @param ipAddress  the client IP address from the request; may be null
     * @param userAgent  the client user-agent string from the request; may be null
     * @param details    human-readable description of the event
     * @param actor      the authenticated actor performing the operation; may be null
     * @return the persisted SecurityAuditEvent entity
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public SecurityAuditEvent recordParticipating(SecurityAuditEventType eventType, UUID userId, String username,
            String ipAddress, String userAgent, String details, AuditActor actor) {
        return insertEvent(
                eventType,
                userId,
                username,
                ipAddress,
                userAgent,
                details,
                mergeActor(actor, SecurityAuditExtensions.NONE),
                null);
    }

    private SecurityAuditEvent insertEvent(SecurityAuditEventType eventType, UUID userId, String username,
            String ipAddress, String userAgent, String details, SecurityAuditExtensions extensions,
            AuditEventDetail detail) {
        SecurityAuditEvent event = new SecurityAuditEvent(
                eventType,
                userId,
                username,
                ipAddress,
                userAgent,
                details,
                extensions,
                detail);
        SecurityAuditEvent saved = repository.save(event);
        log.debug(
                "Security audit: {} user={} ip={} changedBy={}",
                eventType,
                username,
                ipAddress,
                saved.getChangedByUsername());
        return saved;
    }

    private static SecurityAuditExtensions mergeActor(AuditActor actor, SecurityAuditExtensions ext) {
        SecurityAuditExtensions e = ext != null ? ext : SecurityAuditExtensions.NONE;
        if (actor == null) {
            return e;
        }
        return new SecurityAuditExtensions(
                actor.userId(),
                actor.username(),
                e.previousValue(),
                e.currentValue(),
                e.approvedByUserId(),
                e.approvedByUsername(),
                e.approvalDate());
    }

    /**
     * Searches the security audit log with optional filters applied in combination.
     *
     * All parameters are optional; null or blank values are ignored. Filters are composed
     * using a JPA Criteria Specification so only the supplied constraints are included in
     * the SQL predicate. Results are paginated and ordered according to the given Pageable.
     *
     * @param userId     filter by the UUID of the subject user; null matches all users
     * @param eventType  filter by event category; null matches all event types
     * @param username   exact-match filter on the subject username; null or blank ignored
     * @param ipAddress  exact-match filter on the client IP address; null or blank ignored
     * @param from       lower bound (inclusive) on the event timestamp; null means unbounded
     * @param to         upper bound (inclusive) on the event timestamp; null means unbounded
     * @param pageable   pagination and sort descriptor
     * @return a page of matching SecurityAuditEvent entities
     */
    @Transactional(readOnly = true)
    public Page<SecurityAuditEvent> search(UUID userId, SecurityAuditEventType eventType, String username,
            String ipAddress, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Specification<SecurityAuditEvent> spec = (root, query, cb) -> cb.conjunction();

        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }

        if (eventType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("eventType"), eventType));
        }

        if (username != null && !username.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("username"), username));
        }

        if (ipAddress != null && !ipAddress.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ipAddress"), ipAddress));
        }

        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }

        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return repository.findAll(spec, pageable);
    }
}
