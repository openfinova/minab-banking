package com.openfinova.banking.keycloak.internal;

import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.credential.OTPCredentialProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;

import jakarta.enterprise.inject.Vetoed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Internal realm endpoints invoked by the banking platform (not browser clients).
 *
 * TOTP enrollment in the staff portal writes to the banking database first; this resource mirrors
 * the secret into Keycloak's OTP credential store so browser login and step-up can reach gold LoA.
 */
@Vetoed
@Path("/")
public class BankingInternalResource {

    private static final Logger LOG = Logger.getLogger(BankingInternalResource.class);
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String TOTP_USER_LABEL = "OpenFinova Banking";

    private final KeycloakSession session;
    private final String internalToken;

    BankingInternalResource(KeycloakSession session, String internalToken) {
        this.session = session;
        this.internalToken = internalToken;
    }

    /**
     * Replaces the user's Keycloak OTP credential with the banking-enrolled Base32 secret.
     *
     * @param username banking username (Keycloak {@code username})
     * @param payload  JSON body carrying the Base32 secret
     */
    @PUT
    @Path("users/{username}/totp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response syncTotp(@PathParam("username") String username, TotpSyncPayload payload) {
        if (!isAuthorized()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (payload == null || payload.secret == null || payload.secret.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"secret_required\"}").build();
        }

        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserByUsername(realm, username);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        removeOtpCredentials(realm, user);
        var policy = realm.getOTPPolicy();
        OTPCredentialModel credential = OTPCredentialModel.createTOTP(
                payload.secret,
                policy.getDigits(),
                policy.getPeriod(),
                policy.getAlgorithm(),
                OTPCredentialModel.SecretEncoding.BASE32.name());
        credential.setUserLabel(TOTP_USER_LABEL);
        otpProvider().createCredential(realm, user, credential);
        LOG.infof("Synced OTP credential for user %s", username);
        return Response.noContent().build();
    }

    /** Removes all Keycloak OTP credentials for the user (mirrors banking MFA disable). */
    @DELETE
    @Path("users/{username}/totp")
    public Response removeTotp(@PathParam("username") String username) {
        if (!isAuthorized()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserByUsername(realm, username);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        removeOtpCredentials(realm, user);
        LOG.infof("Removed OTP credentials for user %s", username);
        return Response.noContent().build();
    }

    private boolean isAuthorized() {
        if (internalToken == null || internalToken.isBlank()) {
            LOG.warn("BANKING_INTERNAL_TOKEN is not configured; rejecting internal request");
            return false;
        }
        String presented = session.getContext().getRequestHeaders().getHeaderString(INTERNAL_TOKEN_HEADER);
        return internalToken.equals(presented);
    }

    private OTPCredentialProvider otpProvider() {
        return (OTPCredentialProvider) session.getProvider(CredentialProvider.class,
                OTPCredentialProviderFactory.PROVIDER_ID);
    }

    private void removeOtpCredentials(RealmModel realm, UserModel user) {
        OTPCredentialProvider provider = otpProvider();
        Stream<CredentialModel> otpCredentials = user.credentialManager().getStoredCredentialsByTypeStream(
                OTPCredentialModel.TYPE);
        otpCredentials.map(CredentialModel::getId).forEach(id -> provider.deleteCredential(realm, user, id));
    }

    /** JSON body for {@link #syncTotp(String, TotpSyncPayload)}. */
    public static final class TotpSyncPayload {
        public String secret;
    }
}
