package com.openfinova.banking.tan.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.stereotype.Service;

import com.openfinova.banking.setup.api.DateTimeService;
import com.openfinova.banking.tan.cache.TanShortLivedCache;
import com.openfinova.banking.tan.config.TanEnrollmentJwtCodec;

/**
 * Issues and validates short-lived signed JWTs for TAN device enrollment bootstrap.
 *
 * These are purpose-scoped enrollment tokens, not OAuth2 access tokens. They bind a QR scan
 * to a specific user, carry a server nonce in the {@code nonce} claim, and are single-use via
 * {@code jti} tracking in {@link TanShortLivedCache}. The mobile app presents the token when
 * requesting an attestation nonce and when submitting {@link TanDeviceService#enrollDevice};
 * {@link #consumeEnrollmentToken} marks the {@code jti} used once enrollment succeeds.
 *
 * Token lifetime matches the enrollment QR TTL ({@link #TOKEN_TTL}, 15 minutes).
 *
 * @see TanDeviceService#createEnrollmentQr
 * @see TanAttestationService#issueNonce
 */
@Service
public class TanEnrollmentTokenService {

    /** Custom claim marking the JWT as a TAN enrollment token rather than a session access token. */
    public static final String CLAIM_PURPOSE = "tan_enrollment";

    /** Server nonce embedded at QR creation; echoed in enrollment flows for binding. */
    public static final String CLAIM_NONCE = "nonce";

    /** Validity window for issued enrollment tokens. */
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);

    /** How long a consumed {@code jti} remains marked as used to block replay. */
    private static final Duration JTI_CACHE_TTL = Duration.ofMinutes(15);
    private static final String JTI_PREFIX = "enroll-jti:";

    private final TanEnrollmentJwtCodec jwtCodec;
    private final DateTimeService dateTimeService;
    private final TanShortLivedCache cache;
    private final String issuer;

    public TanEnrollmentTokenService(TanEnrollmentJwtCodec jwtCodec, DateTimeService dateTimeService,
            TanShortLivedCache cache,
            @Value("${tan.enrollment.signing.issuer:urn:openfinova:tan-enrollment}") String issuer) {
        this.jwtCodec = jwtCodec;
        this.dateTimeService = dateTimeService;
        this.cache = cache;
        this.issuer = issuer;
    }

    /**
     * Creates a signed enrollment JWT for embedding in the enrollment QR.
     *
     * @param userId banking user enrolling a device (JWT {@code sub})
     * @param nonce server-generated nonce stored in {@link #CLAIM_NONCE}
     * @return compact serialized JWT for the QR payload
     */
    public String createEnrollmentToken(UUID userId, String nonce) {
        Instant now = dateTimeService.instant();
        String jti = UUID.randomUUID().toString();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(userId.toString()).issuedAt(now)
                .expiresAt(now.plus(TOKEN_TTL)).id(jti).claim(CLAIM_PURPOSE, true).claim(CLAIM_NONCE, nonce).build();
        return jwtCodec.encode(claims);
    }

    /**
     * Validates and parses an enrollment token without consuming it.
     *
     * Verifies signature, expiry, purpose claim, and that the {@code jti} has not already
     * been consumed. Call {@link #consumeEnrollmentToken} after successful device enrollment.
     *
     * @param token JWT from the enrollment QR or mobile app request
     * @return resolved user, nonce, and {@code jti} for downstream attestation and enrollment
     * @throws IllegalArgumentException if the token is invalid, expired, or already used
     */
    public EnrollmentTokenClaims parseEnrollmentToken(String token) {
        Jwt jwt;
        try {
            jwt = jwtCodec.decode(token);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid or expired enrollment token");
        }
        if (!Boolean.TRUE.equals(jwt.getClaim(CLAIM_PURPOSE))) {
            throw new IllegalArgumentException("Invalid enrollment token purpose");
        }
        String jti = jwt.getId();
        if (jti == null) {
            throw new IllegalArgumentException("Enrollment token missing jti");
        }
        if (cache.exists(JTI_PREFIX + jti)) {
            throw new IllegalArgumentException("Enrollment token already used");
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        String nonce = jwt.getClaimAsString(CLAIM_NONCE);
        return new EnrollmentTokenClaims(userId, nonce, jti);
    }

    /**
     * Marks the enrollment token as consumed so the same QR cannot enroll multiple devices.
     * Must be called once after {@link TanDeviceService#enrollDevice} persists the new device.
     *
     * @param jti {@code jti} claim from {@link #parseEnrollmentToken}
     * @throws IllegalArgumentException if this {@code jti} was already consumed
     */
    public void consumeEnrollmentToken(String jti) {
        if (!cache.putIfAbsent(JTI_PREFIX + jti, JTI_CACHE_TTL)) {
            throw new IllegalArgumentException("Enrollment token already used");
        }
    }

    /** Parsed enrollment JWT claims used during attestation and device registration. */
    public record EnrollmentTokenClaims(UUID userId, String nonce, String jti) {
    }
}
