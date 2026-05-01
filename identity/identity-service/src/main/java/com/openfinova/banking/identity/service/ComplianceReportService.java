package com.openfinova.banking.identity.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.identity.config.RbacProperties;
import com.openfinova.banking.identity.dto.LoginActivityReportRow;
import com.openfinova.banking.identity.dto.PermissionChangeReportRow;
import com.openfinova.banking.identity.dto.SodViolationResponse;
import com.openfinova.banking.identity.dto.UserAccessReportRow;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.entity.SecurityAuditEvent;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.repository.SecurityAuditEventRepository;
import com.openfinova.banking.identity.repository.UserRepository;

/**
 * Produces compliance reports for regulatory submissions and internal audits.
 *
 * Covers four report types: user access snapshots (who has which roles), role and permission
 * change history, login activity timelines, and separation-of-duties (SoD) violation
 * listings. All methods expose paginated or list results sourced from the user and audit
 * event stores. Access control enforcement (requiring the report:generate authority) is
 * delegated to the controller layer.
 */
@Service
public class ComplianceReportService {

    private static final Set<SecurityAuditEventType> PERMISSION_CHANGE_TYPES = Set.of(
            SecurityAuditEventType.ROLE_CREATED,
            SecurityAuditEventType.ROLE_UPDATED,
            SecurityAuditEventType.ROLE_DELETED,
            SecurityAuditEventType.ROLE_PERMISSIONS_CHANGED,
            SecurityAuditEventType.PERMISSION_ADDED,
            SecurityAuditEventType.PERMISSION_REMOVED,
            SecurityAuditEventType.ROLE_ASSIGNED,
            SecurityAuditEventType.ROLE_REVOKED);

    private static final Set<SecurityAuditEventType> LOGIN_ACTIVITY_TYPES = Set
            .of(SecurityAuditEventType.LOGIN_SUCCESS, SecurityAuditEventType.LOGIN_FAILURE);

    private final UserRepository userRepository;
    private final SecurityAuditEventRepository auditEventRepository;
    private final RbacProperties rbacProperties;

    public ComplianceReportService(UserRepository userRepository, SecurityAuditEventRepository auditEventRepository,
            RbacProperties rbacProperties) {
        this.userRepository = userRepository;
        this.auditEventRepository = auditEventRepository;
        this.rbacProperties = rbacProperties;
    }

    /**
     * Returns a paginated snapshot of all users with their currently assigned roles.
     *
     * Each row in the result maps a user account to the set of roles held at the time of the
     * query. This report is suitable for periodic user-access reviews required by regulations
     * such as SOX and PCI-DSS that mandate evidence of who had access at a given point in
     * time. The caller controls ordering and page size through the Pageable argument.
     *
     * @param pageable  pagination and sort descriptor
     * @return a page of UserAccessReportRow projections, one per user
     */
    @Transactional(readOnly = true)
    public Page<UserAccessReportRow> getUserAccessReport(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserAccessReportRow::from);
    }

    /**
     * Returns a paginated list of role and permission change events within the given time
     * window.
     *
     * Covers role creation, role deletion, permission set replacements, individual permission
     * grants and revocations, and role assignments to users. These event types are defined by
     * the PERMISSION_CHANGE_TYPES constant. Either or both of the time bounds may be null to
     * produce an open-ended range.
     *
     * @param from      lower bound (inclusive) on the event timestamp; null means unbounded
     * @param to        upper bound (inclusive) on the event timestamp; null means unbounded
     * @param pageable  pagination and sort descriptor
     * @return a page of PermissionChangeReportRow projections matching the time window
     */
    @Transactional(readOnly = true)
    public Page<PermissionChangeReportRow> getPermissionChangeReport(LocalDateTime from, LocalDateTime to,
            Pageable pageable) {
        Specification<SecurityAuditEvent> spec = (root, query, cb) -> root.get("eventType").in(PERMISSION_CHANGE_TYPES);

        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return auditEventRepository.findAll(spec, pageable).map(PermissionChangeReportRow::from);
    }

    /**
     * Returns a paginated list of login success and failure events within the given time
     * window, optionally filtered to a specific user.
     *
     * Covers LOGIN_SUCCESS and LOGIN_FAILURE audit event types. When username is supplied
     * only events for that user are returned; when null or blank all users are included.
     * Either or both time bounds may be null to produce an open-ended range. The result
     * is ordered according to the supplied Pageable.
     *
     * @param username  exact login name to filter by; null or blank returns all users
     * @param from      lower bound (inclusive) on the event timestamp; null means unbounded
     * @param to        upper bound (inclusive) on the event timestamp; null means unbounded
     * @param pageable  pagination and sort descriptor
     * @return a page of LoginActivityReportRow projections matching the filters
     */
    @Transactional(readOnly = true)
    public Page<LoginActivityReportRow> getLoginActivityReport(String username, LocalDateTime from, LocalDateTime to,
            Pageable pageable) {
        Specification<SecurityAuditEvent> spec = (root, query, cb) -> root.get("eventType").in(LOGIN_ACTIVITY_TYPES);

        if (username != null && !username.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("username"), username));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return auditEventRepository.findAll(spec, pageable).map(LoginActivityReportRow::from);
    }

    /**
     * Scans all users and returns those who currently hold a pair of roles that violates a
     * configured separation-of-duties rule.
     *
     * Each configured SoD pair is checked against every user's current role set. A user is
     * included in the result for each distinct pair they violate, so a user breaching two
     * independent SoD rules will appear twice. This method performs a full-table user scan
     * and should only be used for scheduled compliance reviews, not on hot request paths.
     *
     * @return a list of SodViolationResponse records, one per user-SoD-pair combination,
     *         or an empty list if no violations are found
     */
    @Transactional(readOnly = true)
    public List<SodViolationResponse> getSodViolations() {
        List<BankingUser> users = userRepository.findAllWithRoles();
        List<SodViolationResponse> violations = new ArrayList<>();

        for (BankingUser user : users) {
            Set<String> userRoleNames = user.getRoles().stream().map(r -> r.getName().toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            for (Set<String> pair : rbacProperties.parsedSodPairs()) {
                if (userRoleNames.containsAll(pair)) {
                    violations.add(new SodViolationResponse(user.getId(), user.getUsername(), List.copyOf(pair)));
                }
            }
        }
        return violations;
    }
}
