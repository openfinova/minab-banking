package com.openfinova.banking.tan.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

/**
 * Banking-owned signer/verifier for short-lived TAN enrollment JWTs.
 *
 * Replaces the dependency on the Spring Authorization Server's {@code JwtEncoder}/{@code JwtDecoder}
 * beans, which are removed once Keycloak becomes the authentication authority. The key is owned by
 * the banking app and persisted to {@code tan.enrollment.signing.key-location} so that enrollment
 * tokens survive restarts; if no location is configured an ephemeral key is generated (dev only,
 * single-instance — in-flight enrollment QRs become invalid on restart).
 *
 * This is deliberately not a {@code JwtEncoder}/{@code JwtDecoder} bean: the resource server already
 * owns those for Keycloak access-token validation, and a second bean of those types would break its
 * auto-configuration.
 */
@Component
public class TanEnrollmentJwtCodec {

    private static final Logger log = LoggerFactory.getLogger(TanEnrollmentJwtCodec.class);

    private final NimbusJwtEncoder encoder;
    private final NimbusJwtDecoder decoder;

    public TanEnrollmentJwtCodec(@Value("${tan.enrollment.signing.key-location:}") String keyLocation) {
        RSAKey rsaKey = loadOrCreateKey(keyLocation);
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        try {
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
            this.decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialise TAN enrollment verifier", ex);
        }
    }

    /** Signs the claim set and returns the compact JWT for the enrollment QR. */
    public String encode(JwtClaimsSet claims) {
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /** Verifies signature and expiry and returns the parsed JWT. */
    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    private RSAKey loadOrCreateKey(String keyLocation) {
        if (keyLocation != null && !keyLocation.isBlank()) {
            Path path = Path.of(keyLocation);
            try {
                if (Files.exists(path)) {
                    return RSAKey.parse(Files.readString(path, StandardCharsets.UTF_8));
                }
                RSAKey generated = generateKey();
                Files.createDirectories(path.toAbsolutePath().getParent());
                Files.writeString(path, generated.toJSONString(), StandardCharsets.UTF_8);
                log.info("Generated and persisted TAN enrollment signing key at {}", keyLocation);
                return generated;
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to load or persist TAN enrollment key at " + keyLocation, ex);
            }
        }
        log.warn(
                "No tan.enrollment.signing.key-location configured; using an ephemeral TAN enrollment key "
                        + "(in-flight enrollment QRs will not survive a restart). Configure a location for persistence.");
        return generateKey();
    }

    private RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate TAN enrollment signing key", ex);
        }
    }
}
