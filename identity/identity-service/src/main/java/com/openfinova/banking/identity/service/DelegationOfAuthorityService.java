package com.openfinova.banking.identity.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.identity.api.audit.AuditActor;
import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.dto.CreateDelegationRequest;
import com.openfinova.banking.identity.dto.DelegationResponse;
import com.openfinova.banking.identity.dto.UserResponse;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.entity.DelegationOfAuthority;
import com.openfinova.banking.identity.entity.DelegationStatus;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.repository.DelegationOfAuthorityRepository;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.validation.GlApprovalRoleValidation;
import com.openfinova.banking.setup.api.DateTimeService;

import jakarta.persistence.criteria.Predicate;

/**
 * Service for managing delegations of authority between banking users.
 *
 * A delegation allows one user, the delegating principal, to temporarily grant another user,
 * the delegate, the right to act on their behalf within a defined scope and time window.
 * This is commonly used for holiday cover and absence management in banking operations.
 *
 * The service enforces that delegator and delegate are different users, validates that both
 * accounts exist and are of an appropriate type, and manages the delegation lifecycle through
 * the states ACTIVE, REVOKED, and EXPIRED. Time-based expiry is evaluated against the
 * DateTimeService clock to support deterministic testing. All state transitions are recorded
 * as security audit events.
 */
@Service
public class DelegationOfAuthorityService {

    private static final Logger log = LoggerFactory.getLogger(DelegationOfAuthorityService.class);

    private static final String OPAQUE_NOT_FOUND = "The requested resource was not found.";

    private final DelegationOfAuthorityRepository delegationRepository;
    private final UserRepository userRepository;
    private final SecurityAuditService auditService;
    private final DateTimeService dateTimeService;

    public DelegationOfAuthorityService(DelegationOfAuthorityRepository delegationRepository,
            UserRepository userRepository, SecurityAuditService auditService, DateTimeService dateTimeService) {
        this.delegationRepository = delegationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Creates a new active delegation of authority between two staff users.
     *
     * Validates that the delegating user and the delegate are different accounts, that both
     * exist in the database, and that both are of type STAFF. If a validUntil date is provided
     * it must be strictly after validFrom. When an approvalLimit is specified a non-blank
     * currency code must also be supplied. The actingGlApprovalRole is validated against the
     * allowed GL role codes. On success the delegation is stored with ACTIVE status and a
     * DOA_CREATED audit event is recorded against the delegate user.
     *
     * @param request  the creation payload containing delegatedFromUserId, delegatedToUserId
     *                 (each a UUID or a unique staff username/email search term), transactionType,
     *                 validFrom, optional validUntil, optional approvalLimit with currency, and
     *                 optional actingGlApprovalRole
     * @param actor    the authenticated actor performing the operation, used for audit recording
     * @return the persisted DelegationOfAuthority entity with ACTIVE status
     * @throws IllegalArgumentException  if delegator and delegate are the same user, if either
     *                                   user is not STAFF, if validUntil is not after validFrom,
     *                                   or if approvalLimit is set without a currency
     * @throws ResourceNotFoundException if either user ID does not exist
     */
    @Transactional
    public DelegationOfAuthority create(CreateDelegationRequest request, AuditActor actor) {
        BankingUser from = resolveStaffUser(request.getDelegatedFromUserId());
        BankingUser to = resolveStaffUser(request.getDelegatedToUserId());
        if (from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("delegatedFrom and delegatedTo must differ");
        }

        LocalDateTime validFrom = request.getValidFrom();
        LocalDateTime validUntil = request.getValidUntil();
        if (validUntil != null && !validUntil.isAfter(validFrom)) {
            throw new IllegalArgumentException("validUntil must be after validFrom");
        }

        if (request.getApprovalLimit() != null && (request.getCurrency() == null || request.getCurrency().isBlank())) {
            throw new IllegalArgumentException("currency is required when approvalLimit is set");
        }

        GlApprovalRoleValidation.requireValidOrNull(request.getActingGlApprovalRole());

        DelegationOfAuthority d = new DelegationOfAuthority(
                from,
                to,
                request.getTransactionType(),
                validFrom,
                validUntil);
        d.setApprovalLimit(request.getApprovalLimit());
        d.setCurrency(request.getCurrency());
        d.setActingGlApprovalRole(request.getActingGlApprovalRole());
        d.setStatus(DelegationStatus.ACTIVE);

        DelegationOfAuthority saved = delegationRepository.save(d);
        auditService.recordParticipating(
                SecurityAuditEventType.DOA_CREATED,
                to.getId(),
                to.getUsername(),
                "Delegation from " + from.getUsername() + " type=" + request.getTransactionType(),
                actor);
        return saved;
    }

    /**
     * Revokes an existing delegation of authority, preventing further use.
     *
     * If the delegation is already in REVOKED status this method is idempotent and returns the
     * existing record without making changes. Otherwise the status is set to REVOKED and a
     * DOA_REVOKED audit event is recorded against the delegate user.
     *
     * @param delegationId  the UUID of the delegation to revoke
     * @param actor         the authenticated actor performing the revocation, used for audit
     *                      recording
     * @return the updated DelegationOfAuthority entity
     * @throws ResourceNotFoundException if no delegation exists with the given ID
     */
    @Transactional
    public DelegationOfAuthority revoke(UUID delegationId, AuditActor actor) {
        DelegationOfAuthority d = delegationRepository.findById(delegationId)
                .orElseThrow(() -> new ResourceNotFoundException("Delegation not found: " + delegationId));
        if (d.getStatus() == DelegationStatus.REVOKED) {
            return d;
        }
        d.setStatus(DelegationStatus.REVOKED);
        DelegationOfAuthority saved = delegationRepository.save(d);
        auditService.recordParticipating(
                SecurityAuditEventType.DOA_REVOKED,
                d.getDelegatedTo().getId(),
                d.getDelegatedTo().getUsername(),
                "Delegation revoked id=" + delegationId,
                actor);
        return saved;
    }

    /**
     * Retrieves a single delegation of authority by its internal UUID.
     *
     * @param id  the unique identifier of the delegation to look up
     * @return the matching DelegationOfAuthority entity
     * @throws ResourceNotFoundException if no delegation exists with the given ID
     */
    @Transactional(readOnly = true)
    public DelegationOfAuthority get(UUID id) {
        return delegationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delegation not found: " + id));
    }

    /**
     * Returns all delegations that a user has granted to others.
     *
     * Returns delegations in all statuses, including REVOKED and EXPIRED. The result
     * is mapped to DelegationResponse DTOs suitable for returning from the API layer.
     *
     * @param userRef  the delegating principal: UUID, or username/email term matching exactly one STAFF user
     * @return a list of DelegationResponse DTOs; empty if the user has no outgoing delegations
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional(readOnly = true)
    public List<DelegationResponse> listOutgoing(String userRef) {
        UUID userId = resolveStaffUser(userRef).getId();
        return delegationRepository.findByDelegatedFromId(userId).stream().map(DelegationResponse::from).toList();
    }

    /**
     * Returns all delegations that have been granted to a user by others.
     *
     * Returns delegations in all statuses, including REVOKED and EXPIRED. The result
     * is mapped to DelegationResponse DTOs suitable for returning from the API layer.
     *
     * @param userRef  the delegate user: UUID, or username/email term matching exactly one STAFF user
     * @return a list of DelegationResponse DTOs; empty if the user has no incoming delegations
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional(readOnly = true)
    public List<DelegationResponse> listIncoming(String userRef) {
        UUID userId = resolveStaffUser(userRef).getId();
        return delegationRepository.findByDelegatedToId(userId).stream().map(DelegationResponse::from).toList();
    }

    /**
     * Returns the delegations currently active for a delegatee user and transaction type.
     *
     * A delegation is considered active when its status is ACTIVE, the current clock time
     * from DateTimeService falls on or after validFrom, and either validUntil is null or the
     * current time has not yet passed validUntil. This method is called by access-checking
     * logic to determine whether a user may act on behalf of another for a given transaction
     * type.
     *
     * @param delegateeUserRef  staff delegatee: UUID, or username/email term matching exactly one STAFF user
     * @param transactionType  the transaction type the delegate intends to perform
     * @return the list of currently active DelegationOfAuthority records; empty if none apply
     */
    @Transactional(readOnly = true)
    public List<DelegationOfAuthority> findActiveForDelegatee(String delegateeUserRef, String transactionType) {
        UUID delegateeUserId = resolveStaffUser(delegateeUserRef).getId();
        LocalDateTime now = dateTimeService.now();
        return delegationRepository
                .findActiveForDelegatee(delegateeUserId, transactionType, DelegationStatus.ACTIVE, now);
    }

    /**
     * Typeahead suggestions for STAFF users (delegation UI). Does not require {@code admin:users:read}.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> suggestStaffUsers(String rawQ, int limit) {
        int cap = Math.min(Math.max(limit, 1), 50);
        String term = rawQ == null ? "" : rawQ.trim();
        if (term.isEmpty()) {
            return List.of();
        }
        UUID id = tryParseUuid(term);
        if (id != null) {
            return userRepository.findById(id).filter(u -> u.getUserType() == UserType.STAFF)
                    .map(u -> List.of(UserResponse.from(u))).orElseGet(List::of);
        }
        Page<BankingUser> page = userRepository.findAll(staffWithFreeText(term), PageRequest.of(0, cap));
        return page.getContent().stream().map(UserResponse::from).toList();
    }

    /**
     * Resolves a staff user from a UUID string or from a case-insensitive username / email substring
     * (must match exactly one STAFF user).
     */
    private BankingUser resolveStaffUser(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("User reference must not be blank");
        }
        String s = raw.strip();
        UUID id = tryParseUuid(s);
        if (id != null) {
            BankingUser u = userRepository.findById(id).orElseThrow(
                    () -> ResourceNotFoundException.opaque("Delegation user not found id=" + id, OPAQUE_NOT_FOUND));
            requireStaff(u, "user");
            return u;
        }
        return resolveStaffUserByNonUuidTerm(s);
    }

    private static UUID tryParseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private BankingUser resolveStaffUserByNonUuidTerm(String raw) {
        List<BankingUser> found = userRepository.findAll(staffWithFreeText(raw), PageRequest.of(0, 6)).getContent();
        if (found.isEmpty()) {
            log.warn("Delegation user resolve failed: no STAFF match for term={}", raw);
            throw ResourceNotFoundException.opaque("No staff user matches the given reference", OPAQUE_NOT_FOUND);
        }
        if (found.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple staff users match \"" + raw + "\"; use the exact user id (UUID) or a unique term.");
        }
        return found.get(0);
    }

    /**
     * STAFF users only; OR on username / email contains (same wildcard stripping as user search {@code q}).
     */
    private static Specification<BankingUser> staffWithFreeText(String raw) {
        return (root, query, cb) -> {
            Predicate staffPred = cb.equal(root.get("userType"), UserType.STAFF);
            String term = raw.toLowerCase(Locale.ROOT).replace("%", "").replace("_", "").trim();
            if (term.isEmpty()) {
                return cb.disjunction();
            }
            String like = "%" + term + "%";
            Predicate usernamePred = cb.like(cb.lower(root.get("username")), like);
            Predicate emailPred = cb.and(cb.isNotNull(root.get("email")), cb.like(cb.lower(root.get("email")), like));
            return cb.and(staffPred, cb.or(usernamePred, emailPred));
        };
    }

    private static void requireStaff(BankingUser u, String label) {
        if (u.getUserType() != UserType.STAFF) {
            throw new IllegalArgumentException(label + " user must be STAFF");
        }
    }
}
