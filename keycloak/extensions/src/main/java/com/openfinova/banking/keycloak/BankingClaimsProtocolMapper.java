package com.openfinova.banking.keycloak;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.authentication.authenticators.util.LoAUtil;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.protocol.oidc.utils.AcrUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.keycloak.services.managers.AuthenticationManager;

/**
 * Injects the banking authorization claims into every issued token.
 *
 * The banking platform owns authorization data; this mapper fetches it for the authenticated
 * user and writes the exact claim contract the banking resource server and BFFs expect:
 * {@code sub} (banking user UUID), {@code permissions}, {@code user_type}, {@code customer_party_id},
 * {@code gl_approval_role}, {@code branch_code}, {@code employee_id}, {@code force_password_change},
 * {@code kyc_status}, and {@code acr}/{@code amr}. This replaces the Spring
 * {@code TokenCustomizerConfig} flattening logic.
 *
 * The banking user id comes from the Keycloak user attribute {@code banking_user_id}; the mapper
 * overrides {@code sub} with it so downstream {@code UUID.fromString(sub)} lookups keep working
 * without aligning Keycloak's internal user id.
 */
public class BankingClaimsProtocolMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    public static final String PROVIDER_ID = "banking-claims-mapper";

    private static final Logger LOG = Logger.getLogger(BankingClaimsProtocolMapper.class);

    public static final String USER_ATTRIBUTE_BANKING_ID = "banking_user_id";

    // Claim names mirror com.openfinova.banking.identity.api.principal.BankingPrincipal.
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_USER_TYPE = "user_type";
    private static final String CLAIM_CUSTOMER_PARTY = "customer_party_id";
    private static final String CLAIM_GL_APPROVAL_ROLE = "gl_approval_role";
    private static final String CLAIM_BRANCH_CODE = "branch_code";
    private static final String CLAIM_EMPLOYEE_ID = "employee_id";
    private static final String CLAIM_FORCE_PASSWORD_CHANGE = "force_password_change";
    private static final String CLAIM_KYC_STATUS = "kyc_status";
    private static final String CLAIM_AMR = "amr";
    private static final String PREFERRED_USERNAME = "preferred_username";
    private static final String ACR_GOLD = "urn:mace:incommon:iap:gold";
    private static final String ACR_SILVER = "urn:mace:incommon:iap:silver";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Banking Claims Mapper";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Injects banking permissions and identity claims fetched from the banking platform.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession,
            KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        String acr = resolveAcr(clientSession);
        token.setAcr(acr);
        token.getOtherClaims().put(CLAIM_AMR, resolveAmr(clientSession, acr));

        UserModel user = userSession.getUser();
        String bankingUserId = user.getFirstAttribute(USER_ATTRIBUTE_BANKING_ID);
        if (bankingUserId == null || bankingUserId.isBlank()) {
            LOG.warnf("User %s has no %s attribute; banking claims not injected", user.getUsername(),
                    USER_ATTRIBUTE_BANKING_ID);
            return;
        }

        BankingClaims claims = resolveClaims(keycloakSession, bankingUserId);
        if (claims == null) {
            // Fail closed on token content: a token without permissions authorizes nothing.
            LOG.warnf("Banking claims unavailable for user %s; issuing token without authorization claims",
                    bankingUserId);
            return;
        }

        // sub must be the persistent banking user UUID.
        token.setSubject(bankingUserId);
        token.getOtherClaims().put(PREFERRED_USERNAME, claims.username != null ? claims.username : user.getUsername());

        if (claims.permissions != null) {
            token.getOtherClaims().put(CLAIM_PERMISSIONS, claims.permissions);
        }
        if (claims.userType != null) {
            token.getOtherClaims().put(CLAIM_USER_TYPE, claims.userType);
        }
        if (claims.customerPartyId != null) {
            token.getOtherClaims().put(CLAIM_CUSTOMER_PARTY, claims.customerPartyId);
        }
        if (claims.glApprovalRole != null) {
            token.getOtherClaims().put(CLAIM_GL_APPROVAL_ROLE, claims.glApprovalRole);
        }
        if (claims.branchCode != null) {
            token.getOtherClaims().put(CLAIM_BRANCH_CODE, claims.branchCode);
        }
        if (claims.employeeId != null) {
            token.getOtherClaims().put(CLAIM_EMPLOYEE_ID, claims.employeeId);
        }
        if (claims.kycStatus != null) {
            token.getOtherClaims().put(CLAIM_KYC_STATUS, claims.kycStatus);
        }
        token.getOtherClaims().put(CLAIM_FORCE_PASSWORD_CHANGE, claims.forcePasswordChange);
    }

    /** Maps the achieved Keycloak LoA to the legacy ACR URNs expected by the banking apps. */
    private String resolveAcr(AuthenticatedClientSessionModel clientSession) {
        int loa = LoAUtil.getCurrentLevelOfAuthentication(clientSession);
        if (loa < Constants.MINIMUM_LOA) {
            loa = AuthenticationManager.isSSOAuthentication(clientSession) ? 0 : 1;
        }

        Map<String, Integer> acrLoaMap = AcrUtils.getAcrLoaMap(clientSession.getClient());
        String acr = AcrUtils.mapLoaToAcr(loa, acrLoaMap, AcrUtils.getRequiredAcrValues(
                clientSession.getNote(OIDCLoginProtocol.CLAIMS_PARAM)));
        if (acr == null) {
            acr = AcrUtils.mapLoaToAcr(loa, acrLoaMap, AcrUtils.getAcrValues(
                    clientSession.getNote(OIDCLoginProtocol.CLAIMS_PARAM),
                    clientSession.getNote(OIDCLoginProtocol.ACR_PARAM),
                    clientSession.getClient()));
        }
        if (acr == null) {
            acr = AcrUtils.mapLoaToAcr(loa, acrLoaMap, acrLoaMap.keySet());
        }
        if (acr == null) {
            acr = loa >= 2 ? ACR_GOLD : ACR_SILVER;
        }
        return acr;
    }

    private List<String> resolveAmr(AuthenticatedClientSessionModel clientSession, String acr) {
        if (ACR_GOLD.equals(acr) || LoAUtil.getCurrentLevelOfAuthentication(clientSession) >= 2) {
            return List.of("pwd", "mfa");
        }
        return List.of("pwd");
    }

    /** Fetches once per token request and caches on the session for the access/id/userinfo passes. */
    private BankingClaims resolveClaims(KeycloakSession session, String bankingUserId) {
        String cacheKey = "banking-claims:" + bankingUserId;
        BankingClaims cached = session.getAttribute(cacheKey, BankingClaims.class);
        if (cached != null) {
            return cached;
        }
        try {
            BankingClaims fetched = BankingClaimsClient.getInstance().fetch(bankingUserId);
            session.setAttribute(cacheKey, fetched);
            return fetched;
        } catch (BankingClaimsClient.BankingClaimsException ex) {
            LOG.warnf(ex, "Could not resolve banking claims for %s", bankingUserId);
            return null;
        }
    }
}
