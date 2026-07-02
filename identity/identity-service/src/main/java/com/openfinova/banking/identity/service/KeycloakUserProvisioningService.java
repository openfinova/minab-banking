package com.openfinova.banking.identity.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.openfinova.banking.identity.config.KeycloakProvisioningProperties;
import com.openfinova.banking.identity.entity.BankingUser;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Mirrors banking user lifecycle changes into Keycloak via its Admin REST API.
 *
 * Keycloak owns credentials, MFA, and lockout; the banking platform keeps roles, customer linkage,
 * and the {@code banking_user_id} that becomes the JWT {@code sub}. Each Keycloak user carries a
 * {@code banking_user_id} attribute equal to the banking {@code identity_users} id, which the
 * claims-mapper SPI emits as {@code sub} — so the Keycloak internal user id does not need to match.
 *
 * All operations are no-ops when {@code keycloak.enabled} is false, so the app runs standalone in
 * tests and isolated deployments. Failures throw {@link KeycloakProvisioningException} so callers
 * can decide whether to fail the surrounding operation.
 */
@Service
public class KeycloakUserProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserProvisioningService.class);
    private static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String BANKING_INTERNAL_PROVIDER = "banking-internal";

    /** Keycloak user attribute carrying the banking user id; emitted as JWT {@code sub} by the SPI mapper. */
    private static final String BANKING_USER_ID_ATTRIBUTE = "banking_user_id";

    private final KeycloakProvisioningProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public KeycloakUserProvisioningService(KeycloakProvisioningProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates or updates the Keycloak user for a banking account, setting the initial password and
     * the {@code banking_user_id} attribute. Enables/disables the Keycloak user to match the banking
     * account and flags a forced password change as the native {@code UPDATE_PASSWORD} action.
     *
     * @param user                the banking user to mirror
     * @param rawPassword         the initial password to set in Keycloak
     * @param forcePasswordChange whether the user must change the password on first login
     */
    public void ensureUser(BankingUser user, String rawPassword, boolean forcePasswordChange) {
        if (!properties.isEnabled()) {
            return;
        }
        String token = obtainAdminToken();
        String userId = findUserId(token, user.getUsername());
        Map<String, Object> representation = buildUserRepresentation(user, forcePasswordChange);

        if (userId == null) {
            userId = createUser(token, representation);
        } else {
            updateUser(token, userId, representation);
        }
        resetPassword(token, userId, rawPassword, forcePasswordChange);
    }

    /** Enables or disables the Keycloak user matching the given banking username. */
    public void setEnabled(String username, boolean enabled) {
        if (!properties.isEnabled()) {
            return;
        }
        String token = obtainAdminToken();
        String userId = findUserId(token, username);
        if (userId == null) {
            log.warn("Keycloak user not found for username; cannot set enabled state");
            return;
        }
        updateUser(token, userId, Map.of("enabled", enabled));
    }

    /**
     * Sets a new password for the Keycloak user.
     *
     * @param username  the banking username
     * @param rawPassword the new password
     * @param temporary whether Keycloak should force a change on next login
     */
    public void setPassword(String username, String rawPassword, boolean temporary) {
        if (!properties.isEnabled()) {
            return;
        }
        String token = obtainAdminToken();
        String userId = findUserId(token, username);
        if (userId == null) {
            log.warn("Keycloak user not found for username; cannot set password");
            return;
        }
        resetPassword(token, userId, rawPassword, temporary);
    }

    /** Adds the native {@code UPDATE_PASSWORD} required action so Keycloak forces a change on next login. */
    public void requirePasswordChange(String username) {
        if (!properties.isEnabled()) {
            return;
        }
        String token = obtainAdminToken();
        String userId = findUserId(token, username);
        if (userId == null) {
            log.warn("Keycloak user not found for username; cannot require password change");
            return;
        }
        updateUser(token, userId, Map.of("requiredActions", List.of(UPDATE_PASSWORD)));
    }

    /**
     * Mirrors a banking TOTP enrollment into Keycloak so the browser login flow can challenge OTP
     * and issue gold {@code acr}. Keycloak is the authentication authority; banking DB MFA state
     * alone does not affect the login flow.
     *
     * @param username      banking username
     * @param base32Secret  plaintext Base32 TOTP secret (same as used by {@link MfaService})
     */
    public void syncTotpCredential(String username, String base32Secret) {
        if (!properties.isEnabled()) {
            return;
        }
        if (base32Secret == null || base32Secret.isBlank()) {
            return;
        }
        Map<String, Object> body = Map.of("secret", base32Secret);
        HttpRequest request = internalJson(totpSyncUri(username), "PUT", body);
        send(request, 204, "sync TOTP credential");
        log.info("Synced TOTP credential to Keycloak for username={}", username);
    }

    /** Removes all Keycloak OTP credentials for the user (mirrors banking MFA disable). */
    public void removeTotpCredentials(String username) {
        if (!properties.isEnabled()) {
            return;
        }
        HttpRequest request = HttpRequest.newBuilder(totpSyncUri(username)).timeout(Duration.ofSeconds(10))
                .header(INTERNAL_TOKEN_HEADER, properties.getInternalToken()).DELETE().build();
        HttpResponse<String> response = sendRaw(request, "remove TOTP credentials");
        int status = response.statusCode();
        if (status == 204) {
            log.info("Removed TOTP credentials from Keycloak for username={}", username);
            return;
        }
        if (status == 404) {
            log.warn("Keycloak user not found for username={}; no OTP credentials to remove", username);
            return;
        }
        throw new KeycloakProvisioningException("Keycloak remove TOTP credentials failed: HTTP " + status);
    }

    private URI totpSyncUri(String username) {
        return URI.create(
                properties.getBaseUrl() + "/realms/" + properties.getRealm() + "/" + BANKING_INTERNAL_PROVIDER
                        + "/users/" + enc(username) + "/totp");
    }

    private HttpRequest internalJson(URI uri, String method, Object body) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(body);
            return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10))
                    .header(INTERNAL_TOKEN_HEADER, properties.getInternalToken())
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofByteArray(payload)).build();
        } catch (Exception ex) {
            throw new KeycloakProvisioningException("Failed to serialise Keycloak internal request body", ex);
        }
    }

    // --- Admin API plumbing ---

    /**
     * Builds the Keycloak user JSON including profile fields the realm expects.
     *
     * Banking users are provisioned centrally; Keycloak must not prompt for name at browser login.
     */
    private Map<String, Object> buildUserRepresentation(BankingUser user, boolean forcePasswordChange) {
        String[] names = deriveDisplayNames(user.getUsername());
        Map<String, Object> representation = new LinkedHashMap<>();
        representation.put("username", user.getUsername());
        representation.put("email", user.getEmail() != null ? user.getEmail() : "");
        representation.put("firstName", names[0]);
        representation.put("lastName", names[1]);
        representation.put("enabled", user.isEnabled());
        representation.put("emailVerified", true);
        representation.put("attributes", Map.of(BANKING_USER_ID_ATTRIBUTE, List.of(user.getId().toString())));
        representation.put("requiredActions", forcePasswordChange ? List.of(UPDATE_PASSWORD) : List.of());
        return representation;
    }

    /** Derives Keycloak first/last name from the banking username when no separate name fields exist. */
    private static String[] deriveDisplayNames(String username) {
        if (username == null || username.isBlank()) {
            return new String[] { "User", "Account" };
        }
        int separator = Math.max(username.indexOf('.'), username.indexOf('_'));
        if (separator > 0 && separator < username.length() - 1) {
            return new String[] { capitalizeToken(username.substring(0, separator)),
                    capitalizeToken(username.substring(separator + 1)) };
        }
        return new String[] { capitalizeToken(username), "User" };
    }

    private static String capitalizeToken(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "User";
        }
        if (trimmed.length() == 1) {
            return trimmed.toUpperCase();
        }
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1).toLowerCase();
    }

    private String obtainAdminToken() {
        String form = "grant_type=client_credentials" + "&client_id=" + enc(properties.getAdminClientId())
                + "&client_secret=" + enc(properties.getAdminClientSecret());
        HttpRequest request = HttpRequest
                .newBuilder(
                        URI.create(
                                properties.getBaseUrl() + "/realms/" + properties.getRealm()
                                        + "/protocol/openid-connect/token"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        JsonNode body = send(request, 200, "obtain admin token");
        return body.path("access_token").asString();
    }

    private String findUserId(String token, String username) {
        URI uri = URI.create(adminUsersUri() + "?exact=true&username=" + enc(username));
        HttpRequest request = adminGet(uri, token);
        JsonNode array = send(request, 200, "find user");
        if (array.isArray() && !array.isEmpty()) {
            return array.get(0).path("id").asString();
        }
        return null;
    }

    private String createUser(String token, Map<String, Object> representation) {
        HttpRequest request = adminJson(URI.create(adminUsersUri()), token, "POST", representation);
        HttpResponse<String> response = sendRaw(request, "create user");
        if (response.statusCode() != 201) {
            throw new KeycloakProvisioningException("Keycloak user create failed: HTTP " + response.statusCode());
        }
        // 201 returns the new user URL in the Location header.
        String location = response.headers().firstValue("Location").orElse("");
        int idx = location.lastIndexOf('/');
        if (idx < 0 || idx == location.length() - 1) {
            throw new KeycloakProvisioningException("Keycloak user create returned no Location header");
        }
        return location.substring(idx + 1);
    }

    private void updateUser(String token, String userId, Map<String, Object> representation) {
        HttpRequest request = adminJson(URI.create(adminUsersUri() + "/" + userId), token, "PUT", representation);
        send(request, 204, "update user");
    }

    private void resetPassword(String token, String userId, String rawPassword, boolean temporary) {
        Map<String, Object> credential = Map.of("type", "password", "value", rawPassword, "temporary", temporary);
        HttpRequest request = adminJson(
                URI.create(adminUsersUri() + "/" + userId + "/reset-password"),
                token,
                "PUT",
                credential);
        HttpResponse<String> response = sendRaw(request, "reset password");
        if (response.statusCode() == 204) {
            return;
        }
        // Idempotent ensureUser: re-setting the same password hits password history policy.
        if (response.statusCode() == 400 && isUnchangedPasswordRejection(response.body())) {
            log.debug("Keycloak password already set to requested value; continuing");
            return;
        }
        throw new KeycloakProvisioningException("Keycloak reset password failed: HTTP " + response.statusCode());
    }

    private static boolean isUnchangedPasswordRejection(String body) {
        return body != null && body.contains("invalidPasswordHistoryMessage");
    }

    private String adminUsersUri() {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users";
    }

    private HttpRequest adminGet(URI uri, String token) {
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + token)
                .header("Accept", "application/json").GET().build();
    }

    private HttpRequest adminJson(URI uri, String token, String method, Object body) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(body);
            return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofByteArray(payload)).build();
        } catch (Exception ex) {
            throw new KeycloakProvisioningException("Failed to serialise Keycloak request body", ex);
        }
    }

    private JsonNode send(HttpRequest request, int expectedStatus, String action) {
        HttpResponse<String> response = sendRaw(request, action);
        if (response.statusCode() != expectedStatus) {
            throw new KeycloakProvisioningException("Keycloak " + action + " failed: HTTP " + response.statusCode());
        }
        try {
            String body = response.body();
            return (body == null || body.isBlank()) ? objectMapper.createObjectNode() : objectMapper.readTree(body);
        } catch (Exception ex) {
            throw new KeycloakProvisioningException("Failed to parse Keycloak response for " + action, ex);
        }
    }

    private HttpResponse<String> sendRaw(HttpRequest request, String action) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new KeycloakProvisioningException("Failed to reach Keycloak for " + action, ex);
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Raised when a Keycloak Admin API operation fails. */
    public static class KeycloakProvisioningException extends RuntimeException {
        public KeycloakProvisioningException(String message) {
            super(message);
        }

        public KeycloakProvisioningException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
