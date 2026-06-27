package com.openfinova.banking.keycloak.kyc;

import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import com.openfinova.banking.keycloak.BankingClaims;
import com.openfinova.banking.keycloak.BankingClaimsClient;
import com.openfinova.banking.keycloak.BankingClaimsProtocolMapper;

/**
 * Login-flow gate that denies unverified/ineligible users (e.g. CUSTOMER without VERIFIED KYC).
 *
 * Preserves the issuance gate previously enforced in {@code TokenCustomizerConfig}: the banking
 * platform computes an {@code eligible} flag (KYC posture, account state) and this authenticator
 * fails the login when it is false. It fails closed if the banking platform is unreachable.
 */
public class BankingKycAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(BankingKycAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            context.attempted();
            return;
        }

        String bankingUserId = user.getFirstAttribute(BankingClaimsProtocolMapper.USER_ATTRIBUTE_BANKING_ID);
        if (bankingUserId == null || bankingUserId.isBlank()) {
            LOG.warnf("User %s has no banking_user_id; denying login", user.getUsername());
            fail(context, "account_not_provisioned");
            return;
        }

        try {
            BankingClaims claims = BankingClaimsClient.getInstance().fetch(bankingUserId);
            if (claims.eligible) {
                context.success();
            } else {
                LOG.infof("Login denied for banking user %s: %s", bankingUserId,
                        claims.denyReason != null ? claims.denyReason : "ineligible");
                fail(context, claims.denyReason != null ? claims.denyReason : "account_ineligible");
            }
        } catch (BankingClaimsClient.BankingClaimsException ex) {
            // Fail closed: never let a user in when eligibility cannot be confirmed.
            LOG.warnf(ex, "Eligibility check unavailable for %s; denying login", bankingUserId);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR,
                    errorPage(context, "eligibility_check_unavailable"));
        }
    }

    private void fail(AuthenticationFlowContext context, String reason) {
        context.failure(AuthenticationFlowError.ACCESS_DENIED, errorPage(context, reason));
    }

    private Response errorPage(AuthenticationFlowContext context, String reason) {
        return context.form().setError(reason).createErrorPage(Response.Status.FORBIDDEN);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // No interactive step.
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // None.
    }

    @Override
    public void close() {
        // Nothing to release.
    }
}
