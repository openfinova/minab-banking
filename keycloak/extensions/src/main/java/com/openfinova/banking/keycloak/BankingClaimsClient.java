package com.openfinova.banking.keycloak;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.jboss.logging.Logger;
import org.keycloak.util.JsonSerialization;

/**
 * Thin HTTP client to the banking platform's internal claims endpoint.
 *
 * Configuration is read from the Keycloak container environment:
 * {@code BANKING_INTERNAL_URL} (base URL, e.g. http://banking-app:8080) and
 * {@code BANKING_INTERNAL_TOKEN} (shared secret presented as {@code X-Internal-Token}).
 */
public final class BankingClaimsClient {

    private static final Logger LOG = Logger.getLogger(BankingClaimsClient.class);
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private static final BankingClaimsClient INSTANCE = new BankingClaimsClient();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final String baseUrl;
    private final String internalToken;

    private BankingClaimsClient() {
        this.baseUrl = stripTrailingSlash(config("BANKING_INTERNAL_URL", "http://banking-app:8080"));
        this.internalToken = config("BANKING_INTERNAL_TOKEN", "");
    }

    public static BankingClaimsClient getInstance() {
        return INSTANCE;
    }

    /**
     * Fetches banking claims for the given banking user id.
     *
     * @param bankingUserId the persistent banking {@code identity_users} UUID (JWT {@code sub})
     * @return the banking claims
     * @throws BankingClaimsException when the platform is unreachable or returns a non-200 status,
     *         so callers can fail closed
     */
    public BankingClaims fetch(String bankingUserId) {
        URI uri = URI.create(baseUrl + "/internal/identity/claims/" + bankingUserId);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .header(INTERNAL_TOKEN_HEADER, internalToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new BankingClaimsException(
                        "Banking claims endpoint returned status " + response.statusCode());
            }
            return JsonSerialization.readValue(response.body(), BankingClaims.class);
        } catch (BankingClaimsException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.warnf(ex, "Failed to fetch banking claims for user %s", bankingUserId);
            throw new BankingClaimsException("Failed to reach banking claims endpoint", ex);
        }
    }

    private static String config(String envName, String defaultValue) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = System.getProperty(envName.toLowerCase().replace('_', '.'));
        }
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /** Raised when banking claims cannot be retrieved; callers should fail closed. */
    public static class BankingClaimsException extends RuntimeException {
        public BankingClaimsException(String message) {
            super(message);
        }

        public BankingClaimsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
