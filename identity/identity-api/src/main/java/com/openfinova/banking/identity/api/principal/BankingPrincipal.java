package com.openfinova.banking.identity.api.principal;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.api.permission.BankingPermission;

/**
 * Typed view of the authenticated principal, extracted from a validated JWT.
 *
 * Obtain an instance via {@link #from(Authentication)} inside a controller or service. It is
 * intentionally a plain value type — no Spring beans, no repository calls.
 *
 * Example usage in a controller:
 *
 * <pre>{@code
 * BankingPrincipal me = BankingPrincipal.from(authentication);
 * if (me.isCustomer() && !loanAccount.getCustomerId().equals(me.customerPartyId())) {
 *     throw new AccessDeniedException("Not your loan");
 * }
 * }</pre>
 */
public record BankingPrincipal(UUID userId, String username, UserType userType, String branchCode, String employeeId,
        UUID customerPartyId, String glApprovalRole,
        /**
         * KYC status name from customer module (JWT), e.g. VERIFIED; null for staff/system or
         * absent claim.
         */
        String kycStatus,

        /**
         * OAuth2 authorization id from JWT {@link #CLAIM_AUTHZ_ID}; null if absent.
         */
        String authSessionId, List<String> permissions) {

    private static final Logger LOG = Logger.getLogger(BankingPrincipal.class.getName());

    /** JWT claim names — must match {@code TokenCustomizerConfig}. */
    public static final String CLAIM_USER_TYPE = "user_type";
    public static final String CLAIM_BRANCH_CODE = "branch_code";
    public static final String CLAIM_EMPLOYEE_ID = "employee_id";
    public static final String CLAIM_CUSTOMER_PARTY = "customer_party_id";
    public static final String CLAIM_GL_APPROVAL_ROLE = "gl_approval_role";
    public static final String CLAIM_KYC_STATUS = "kyc_status";
    public static final String CLAIM_PERMISSIONS = "permissions";

    /**
     * OAuth2 authorization id for audit correlation. Must not use the standard OIDC {@code sid}
     * claim name — Spring Authorization Server hashes the servlet session into {@code sid} for
     * RP-initiated logout validation.
     */
    public static final String CLAIM_AUTHZ_ID = "openfinova_authz_id";

    /**
     * Pre-authz-id tokens mistakenly stored the OAuth2 authorization id under the OIDC {@code sid} claim name.
     */
    private static final String LEGACY_AUTHZ_ID_CLAIM = "sid";

    public static final String CLAIM_CLIENT_IP = "client_ip";
    /**
     * When true, the user must change password before using banking APIs (resource-server filter).
     */
    public static final String CLAIM_FORCE_PASSWORD_CHANGE = "force_password_change";

    /** Normalizes {@code permissions} to an unmodifiable list. */
    public BankingPrincipal {
        permissions = List.copyOf(permissions != null ? permissions : List.of());
    }

    /**
     * Extracts a {@link BankingPrincipal} from the current {@link Authentication}. Works with both
     * JWT (resource-server) and UsernamePassword (test/dev) tokens.
     *
     * @throws IllegalArgumentException if the authentication is null or unsupported
     */
    public static BankingPrincipal from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("No authenticated principal in security context");
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            UUID userId = null;
            if (sub != null) {
                try {
                    userId = UUID.fromString(sub);
                } catch (IllegalArgumentException e) {
                    LOG.log(Level.WARNING, "Invalid UUID in JWT subject: {0}", sub);
                }
            } else {
                LOG.log(Level.FINE, "JWT subject (sub) is null");
            }

            String userTypeStr = jwt.getClaimAsString(CLAIM_USER_TYPE);
            UserType userType = userTypeStr != null ? UserType.valueOf(userTypeStr) : UserType.STAFF;

            String customerPartyStr = jwt.getClaimAsString(CLAIM_CUSTOMER_PARTY);
            UUID customerPartyId = customerPartyStr != null ? UUID.fromString(customerPartyStr) : null;

            List<String> permissions = jwt.getClaimAsStringList(CLAIM_PERMISSIONS);

            String authzId = jwt.getClaimAsString(CLAIM_AUTHZ_ID);
            if (authzId == null) {
                authzId = jwt.getClaimAsString(LEGACY_AUTHZ_ID_CLAIM);
            }

            return new BankingPrincipal(
                    userId,
                    jwt.getClaimAsString("preferred_username") != null ? jwt.getClaimAsString("preferred_username")
                            : authentication.getName(),
                    userType,
                    jwt.getClaimAsString(CLAIM_BRANCH_CODE),
                    jwt.getClaimAsString(CLAIM_EMPLOYEE_ID),
                    customerPartyId,
                    jwt.getClaimAsString(CLAIM_GL_APPROVAL_ROLE),
                    jwt.getClaimAsString(CLAIM_KYC_STATUS),
                    authzId,
                    permissions);
        }

        // Fallback for non-JWT authentication (tests, Basic auth dev mode)
        return new BankingPrincipal(
                null,
                authentication.getName(),
                UserType.STAFF,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public boolean isStaff() {
        return UserType.STAFF == userType;
    }

    public boolean isCustomer() {
        return UserType.CUSTOMER == userType;
    }

    public boolean hasPermission(BankingPermission permission) {
        return permissions.contains(permission.getAuthority());
    }
}
