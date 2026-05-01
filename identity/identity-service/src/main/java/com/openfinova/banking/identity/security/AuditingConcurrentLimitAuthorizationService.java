package com.openfinova.banking.identity.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.openfinova.banking.identity.config.OAuth2TokenPolicyProperties;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.service.SecurityAuditService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * OAuth2 authorization service that wraps JdbcOAuth2AuthorizationService with two additional
 * concerns: per-user concurrent authorization limiting and security audit logging.
 *
 * Concurrent authorization limiting:
 * Each user and OAuth2 client combination is tracked with a FIFO queue of active authorization IDs.
 * When a new authorization is saved and the number of active authorizations for that user and client
 * reaches the configured maximum (identity.oauth2.max-active-authorizations-per-user), the oldest
 * authorization is revoked automatically. This provides a stateless equivalent of server-side
 * concurrent session control. Setting the maximum to 0 or less disables limiting entirely.
 *
 * Audit logging:
 * Every new authorization issuance and every revocation (whether explicit, from logout, or from
 * the concurrent limit eviction) is recorded via SecurityAuditService. Each audit record includes
 * the user ID, username, client IP address, User-Agent header, and a details string describing
 * the authorization ID, client ID, and scopes or revocation reason.
 *
 * Thread safety:
 * Each user-client queue is guarded by its own dedicated lock object to minimize contention.
 * Concurrent authorizations for different users or different clients do not block each other.
 *
 * Persistence:
 * All token storage delegates to JdbcOAuth2AuthorizationService, which persists authorizations
 * in the oauth2_authorization database table. The in-memory queue tracking (sessionQueues,
 * queueLocks, authIdToQueueKey) is local to this instance and is lost on restart. For a single-node
 * deployment this is acceptable; for multi-node deployments the queue tracking would need to move
 * to a shared store to enforce the limit consistently across nodes.
 */
@Component
public class AuditingConcurrentLimitAuthorizationService implements OAuth2AuthorizationService {

    private final JdbcOAuth2AuthorizationService delegate;
    private final SecurityAuditService auditService;
    private final UserRepository userRepository;
    private final OAuth2TokenPolicyProperties tokenPolicy;

    /**
     * Per {@code principal|clientId} queue of authorization IDs in creation order.
     */
    private final Map<String, Deque<String>> sessionQueues = new ConcurrentHashMap<>();
    private final Map<String, Object> queueLocks = new ConcurrentHashMap<>();
    private final Map<String, String> authIdToQueueKey = new ConcurrentHashMap<>();

    public AuditingConcurrentLimitAuthorizationService(JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository, SecurityAuditService auditService,
            UserRepository userRepository, OAuth2TokenPolicyProperties tokenPolicy) {
        this.delegate = new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository);
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.tokenPolicy = tokenPolicy;
    }

    /**
     * Persists a new or updated OAuth2 authorization to the database.
     *
     * For new authorizations that have an access token, this method also enforces the concurrent
     * authorization limit and records an issuance audit event. An authorization is considered new
     * if no existing record with the same ID is found in the delegate store before saving.
     *
     * If the authorization already exists (e.g. a token refresh updating the same record) or does
     * not yet have an access token (e.g. the authorization code stage before token exchange), the
     * method delegates the save and returns without limit enforcement or auditing.
     *
     * When the concurrent limit is active and the queue for the user-client pair is at capacity,
     * the oldest authorization is removed from the store and a revocation audit event is recorded
     * before the new authorization is added to the queue.
     *
     * @param authorization the authorization to save; must not be null
     */
    @Override
    public void save(OAuth2Authorization authorization) {
        boolean isNew = delegate.findById(authorization.getId()) == null;
        delegate.save(authorization);

        if (!isNew || authorization.getAccessToken() == null) {
            return;
        }

        String principal = authorization.getPrincipalName();
        String clientId = authorization.getRegisteredClientId();
        String queueKey = queueKey(principal, clientId);
        if (queueKey == null) {
            auditIssued(authorization, principal, clientId);
            return;
        }

        int max = tokenPolicy.getMaxActiveAuthorizationsPerUser();
        if (max > 0) {
            Object lock = queueLocks.computeIfAbsent(queueKey, k -> new Object());
            synchronized (lock) {
                Deque<String> q = sessionQueues.computeIfAbsent(queueKey, ky -> new ArrayDeque<>());
                pruneMissing(q);
                while (q.size() >= max) {
                    String oldestId = q.pollFirst();
                    if (oldestId == null) {
                        break;
                    }
                    if (oldestId.equals(authorization.getId())) {
                        break;
                    }
                    authIdToQueueKey.remove(oldestId);
                    OAuth2Authorization old = delegate.findById(oldestId);
                    if (old != null) {
                        auditRevoked(old, "Revoked: concurrent authorization limit (" + max + ") exceeded");
                        delegate.remove(old);
                    }
                }
                q.addLast(authorization.getId());
                authIdToQueueKey.put(authorization.getId(), queueKey);
            }
        }

        auditIssued(authorization, principal, clientId);
    }

    /**
     * Removes an OAuth2 authorization from the database and records a revocation audit event.
     *
     * This is called by the framework on explicit logout, token revocation requests, or when the
     * authorization server replaces an existing authorization with a new one. The authorization ID
     * is also removed from the in-memory queue and reverse-lookup map so the slot becomes available
     * for a future authorization from the same user and client.
     *
     * @param authorization the authorization to remove; must not be null
     */
    @Override
    public void remove(OAuth2Authorization authorization) {
        String id = authorization.getId();
        String qKey = authIdToQueueKey.remove(id);
        if (qKey != null) {
            Object lock = queueLocks.computeIfAbsent(qKey, k -> new Object());
            synchronized (lock) {
                Deque<String> q = sessionQueues.get(qKey);
                if (q != null) {
                    q.remove(id);
                    if (q.isEmpty()) {
                        sessionQueues.remove(qKey);
                    }
                }
            }
        }
        auditRevoked(authorization, "Authorization removed (logout, revoke, or replace)");
        delegate.remove(authorization);
    }

    /**
     * Returns the authorization with the given ID, or null if no such authorization exists.
     * This is a direct pass-through to the JDBC delegate with no additional logic.
     *
     * @param id the authorization ID to look up; must not be empty
     * @return the matching authorization, or null
     */
    @Override
    public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
    }

    /**
     * Returns the authorization that contains the given token value, or null if not found.
     * The token type narrows the search to a specific token field (access token, refresh token,
     * authorization code, etc.). If the token type is null, all token fields are searched.
     * This is a direct pass-through to the JDBC delegate with no additional logic.
     *
     * @param token     the raw token string to search for; must not be empty
     * @param tokenType the type of token to match against, or null to search all token fields
     * @return the authorization containing the token, or null
     */
    @Override
    public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
        return delegate.findByToken(token, tokenType);
    }

    /**
     * Removes from the queue any authorization IDs that no longer exist in the delegate store.
     * This guards against authorizations that were removed externally (e.g. expired and cleaned up
     * by a background job) without going through the remove method of this service, which would
     * otherwise leave stale IDs in the queue and cause the limit to be enforced incorrectly.
     * Must be called while holding the lock for the queue's key.
     *
     * @param q the queue to prune in place
     */
    private void pruneMissing(Deque<String> q) {
        q.removeIf(oid -> delegate.findById(oid) == null);
    }

    /**
     * Builds the map key used to identify the queue and lock for a given user-client pair.
     * The key is the principal name and client ID joined by a null character, which cannot
     * appear in either value and therefore guarantees no accidental collisions between different
     * principals or client IDs.
     * Returns null if either argument is null, in which case queue-based limit enforcement is skipped.
     *
     * @param principal the OAuth2 principal name (typically the username)
     * @param clientId  the registered OAuth2 client ID
     * @return the composite queue key, or null if either argument is null
     */
    private static String queueKey(String principal, String clientId) {
        if (principal == null || clientId == null) {
            return null;
        }
        return principal + "\0" + clientId;
    }

    /**
     * Records an OAUTH2_AUTHORIZATION_ISSUED audit event for a newly issued authorization.
     * The event includes the user ID looked up from the user repository, the principal name,
     * the client IP and User-Agent from the current HTTP request, and a details string with
     * the authorization ID, client ID, and authorized scopes.
     *
     * @param authorization the newly issued authorization
     * @param principal     the principal name from the authorization
     * @param clientId      the registered client ID from the authorization
     */
    private void auditIssued(OAuth2Authorization authorization, String principal, String clientId) {
        UUID userId = userRepository.findByUsername(principal).map(u -> u.getId()).orElse(null);
        String ip = currentClientIp();
        String ua = currentUserAgent();
        String details = "OAuth2 authorization id=" + authorization.getId() + " client_id=" + clientId + " scopes="
                + authorization.getAuthorizedScopes();
        auditService.record(SecurityAuditEventType.OAUTH2_AUTHORIZATION_ISSUED, userId, principal, ip, ua, details);
    }

    /**
     * Records an OAUTH2_AUTHORIZATION_REVOKED audit event for a revoked authorization.
     * The event includes the user ID looked up from the user repository, the principal name,
     * the client IP and User-Agent from the current HTTP request, and a details string with
     * the revocation reason, authorization ID, and client ID.
     *
     * Note: when this method is called as a side effect of the concurrent limit eviction inside
     * save(), the current HTTP request belongs to the newly authenticating user, not to the user
     * whose authorization is being evicted. The IP and User-Agent captured in the audit record
     * will therefore reflect the new login request rather than the original session.
     *
     * @param authorization the authorization being revoked
     * @param reason        a human-readable description of why the authorization was revoked
     */
    private void auditRevoked(OAuth2Authorization authorization, String reason) {
        String principal = authorization.getPrincipalName();
        UUID userId = userRepository.findByUsername(principal).map(u -> u.getId()).orElse(null);
        String ip = currentClientIp();
        String ua = currentUserAgent();
        String details = reason + " id=" + authorization.getId() + " client_id="
                + authorization.getRegisteredClientId();
        auditService.record(SecurityAuditEventType.OAUTH2_AUTHORIZATION_REVOKED, userId, principal, ip, ua, details);
    }

    /**
     * Returns the client IP address from the current HTTP request, resolved via ClientIpResolver
     * which handles X-Forwarded-For and similar proxy headers. Returns null if there is no
     * active request context (e.g. when called from a background thread).
     *
     * @return the resolved client IP address string, or null
     */
    private static String currentClientIp() {
        HttpServletRequest req = currentRequest();
        return ClientIpResolver.resolve(req);
    }

    /**
     * Returns the User-Agent header value from the current HTTP request, or null if there is
     * no active request context or no User-Agent header was sent.
     *
     * @return the User-Agent header value, or null
     */
    private static String currentUserAgent() {
        HttpServletRequest req = currentRequest();
        return req != null ? req.getHeader("User-Agent") : null;
    }

    /**
     * Returns the HttpServletRequest bound to the current thread via Spring's RequestContextHolder,
     * or null if the current thread is not processing an HTTP request (e.g. scheduled tasks or
     * asynchronous processing outside of a request scope).
     *
     * @return the current HttpServletRequest, or null
     */
    private static HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }
}
