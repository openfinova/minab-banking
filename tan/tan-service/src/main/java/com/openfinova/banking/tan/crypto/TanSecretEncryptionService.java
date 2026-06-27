package com.openfinova.banking.tan.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Encrypts device {@code tanSecret} values at rest in the database.
 *
 * Regulatory and security context
 *   The shared TAN secret is a possession factor: compromise would allow generating valid payment
 *   codes. PSD2 and general banking practice require protecting authentication credentials at rest.
 *   Plaintext storage of keys/secrets in the application database is not acceptable; this service
 *   ensures ciphertext is persisted while the entity layer still sees decrypted values in memory
 *   during verification only.
 *
 * Algorithm (NIST SP 800-38D)
 *   AES-256-GCM ({@code AES/GCM/NoPadding}) provides authenticated encryption: confidentiality
 *   and integrity under a single primitive. Parameters follow common deployment guidance:
 *   12-byte random IV per encryption, 128-bit authentication tag.
 *
 * Key handling
 *   Operator-supplied {@code tan.secret.encryption.key} (from environment / secrets manager,
 *   never committed to source control) is hashed with SHA-256 to derive a 256-bit AES key,
 *   allowing configurable passphrase length while fixing the key size.
 *   This key is independent of {@link TanQrSigningService} and per-device {@code tanSecret}
 *   values (key separation).
 *
 * Wire format
 *   {@value #PREFIX} + Base64( IV || ciphertext+tag ). The version prefix supports future
 *   algorithm rotation without breaking reads of legacy rows. Values without the prefix are
 *   returned unchanged on decrypt to support migration from pre-encryption data.
 *
 * @see TanSecretAttributeConverter
 * @see com.openfinova.banking.tan.entity.TanDevice#tanSecret
 */
@Service
public class TanSecretEncryptionService {

    /** Column value prefix identifying AES-GCM v1 ciphertext. */
    static final String PREFIX = "enc1:";

    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String CIPHER = "AES/GCM/NoPadding";

    private final SecretKeySpec aesKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public TanSecretEncryptionService(@Value("${tan.secret.encryption.key}") String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalStateException("tan.secret.encryption.key must be non-blank");
        }
        byte[] keyBytes = sha256(rawKey.getBytes(StandardCharsets.UTF_8));
        this.aesKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts a plaintext secret for database persistence. Idempotent if already prefixed.
     *
     * @param plaintextSecret Base64-encoded device secret as held in the JPA entity
     * @return {@value #PREFIX} ciphertext, or {@code null} if input is {@code null}
     */
    public String encryptToColumn(String plaintextSecret) {
        if (plaintextSecret == null) {
            return null;
        }
        if (plaintextSecret.startsWith(PREFIX)) {
            return plaintextSecret;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plaintextSecret.getBytes(StandardCharsets.UTF_8));
            byte[] combined = ByteBuffer.allocate(iv.length + cipherBytes.length).put(iv).put(cipherBytes).array();
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("TAN secret encryption failed", e);
        }
    }

    /**
     * Decrypts a column value for use in {@link TanCodeService} verification.
     *
     * @param columnValue stored column value (encrypted or legacy plaintext)
     * @return plaintext secret for in-memory use only
     */
    public String decryptFromColumn(String columnValue) {
        if (columnValue == null) {
            return null;
        }
        if (!columnValue.startsWith(PREFIX)) {
            return columnValue;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(columnValue.substring(PREFIX.length()));
            ByteBuffer buf = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            buf.get(iv);
            byte[] cipherBytes = new byte[buf.remaining()];
            buf.get(cipherBytes);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt TAN secret", e);
        }
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
