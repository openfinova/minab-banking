package com.openfinova.banking.tan.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.openfinova.banking.setup.api.DateTimeService;
import com.openfinova.banking.tan.cache.TanShortLivedCache;
import com.openfinova.banking.tan.config.TanProperties;

/**
 * Device attestation gate during TAN enrollment.
 *
 * Before a mobile app may submit its TAN secret, it must prove it runs on a genuine device.
 * This service issues a short-lived server nonce bound to the enrollment token's {@code jti},
 * then validates that the app completed the attestation handshake before enrollment proceeds.
 *
 * Expected client flow
 *   Request an attestation nonce ({@link #issueNonce}) using the enrollment token's {@code jti}.
 *   Obtain a platform attestation verdict (Google Play Integrity on Android, Apple App Attest on iOS)
 *   that incorporates the server nonce.
 *   Submit the verdict with the enrollment request ({@link #verifyAttestation}).
 *
 * When {@code tan.attestation.enforce} is {@code true}, production must verify platform verdicts
 * cryptographically; that wiring is pending credential provisioning. When {@code false}, a
 * non-blank attestation token is still required so the enrollment contract stays stable across
 * environments.
 *
 * @see com.openfinova.banking.tan.service.TanDeviceService#issueAttestationNonce
 * @see com.openfinova.banking.tan.service.TanDeviceService#enrollDevice
 */
@Service
public class TanAttestationService {

    /** Time window in which an issued nonce must be consumed by {@link #verifyAttestation}. */
    private static final Duration NONCE_TTL = Duration.ofMinutes(5);
    private static final String ATTEST_NONCE_PREFIX = "attest-nonce:";

    private final TanProperties properties;
    private final DateTimeService dateTimeService;
    private final TanShortLivedCache cache;

    public TanAttestationService(TanProperties properties, DateTimeService dateTimeService, TanShortLivedCache cache) {
        this.properties = properties;
        this.dateTimeService = dateTimeService;
        this.cache = cache;
    }

    /**
     * Issues a one-time nonce for the given enrollment {@code jti} and stores it in the short-lived
     * cache. The mobile app passes this nonce to Play Integrity / App Attest when requesting a
     * device attestation token.
     *
     * @param enrollmentJti {@code jti} claim from the signed enrollment token
     * @return the nonce and its expiry instant ({@link #NONCE_TTL} from issuance)
     */
    public AttestationNonce issueNonce(String enrollmentJti) {
        String nonce = UUID.randomUUID().toString();
        cache.put(ATTEST_NONCE_PREFIX + enrollmentJti, nonce, NONCE_TTL);
        Instant expiresAt = dateTimeService.instant().plus(NONCE_TTL);
        return new AttestationNonce(nonce, expiresAt);
    }

    /**
     * Validates that attestation was completed for this enrollment attempt.
     *
     * Rejects the call when no nonce was issued for {@code enrollmentJti} or it has expired.
     * When attestation enforcement is enabled, a platform verdict token is required; full
     * cryptographic verification against Play Integrity / App Attest is not yet implemented.
     *
     * @param enrollmentJti {@code jti} claim from the enrollment token being consumed
     * @param attestationToken platform attestation verdict from the mobile app
     * @throws IllegalArgumentException if the nonce is missing/expired or the token is absent
     */
    public void verifyAttestation(String enrollmentJti, String attestationToken) {
        if (!cache.exists(ATTEST_NONCE_PREFIX + enrollmentJti)) {
            throw new IllegalArgumentException("Attestation nonce expired or not issued");
        }
        if (properties.getAttestation().isEnforce()) {
            if (attestationToken == null || attestationToken.isBlank()) {
                throw new IllegalArgumentException("Device attestation required");
            }
            // Play Integrity / App Attest verification to be wired when credentials are provisioned.
        } else if (attestationToken == null || attestationToken.isBlank()) {
            throw new IllegalArgumentException("Attestation token required");
        }
    }

    /** Server-issued challenge and expiry returned to the mobile app for platform attestation. */
    public record AttestationNonce(
            String nonce, Instant expiresAt
    ) {
    }
}
