package com.openfinova.banking.identity.crypto;

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
 * Encrypts TOTP shared secrets at rest (AES-256-GCM). Plaintext is only held in memory on the entity.
 * <p>
 * Values persisted before encryption was introduced lack the {@value #PREFIX} prefix and are returned
 * as-is on read; the next save re-encrypts them.
 */
@Service
public class MfaSecretEncryptionService {

    static final String PREFIX = "enc1:";

    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String CIPHER = "AES/GCM/NoPadding";

    private final SecretKeySpec aesKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public MfaSecretEncryptionService(@Value("${identity.mfa-secret.encryption.key}") String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalStateException("identity.mfa-secret.encryption.key must be non-blank");
        }
        byte[] keyBytes = sha256(rawKey.getBytes(StandardCharsets.UTF_8));
        this.aesKey = new SecretKeySpec(keyBytes, "AES");
    }

    /** For persistence: null stays null; already-prefixed values are stored unchanged. */
    public String encryptToColumn(String plaintextMfaSecret) {
        if (plaintextMfaSecret == null) {
            return null;
        }
        if (plaintextMfaSecret.startsWith(PREFIX)) {
            return plaintextMfaSecret;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plaintextMfaSecret.getBytes(StandardCharsets.UTF_8));
            byte[] combined = ByteBuffer.allocate(iv.length + cipherBytes.length).put(iv).put(cipherBytes).array();
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("MFA secret encryption failed", e);
        }
    }

    /** For loading: null stays null; values without prefix are treated as legacy plaintext. */
    public String decryptFromColumn(String columnValue) {
        if (columnValue == null) {
            return null;
        }
        if (!columnValue.startsWith(PREFIX)) {
            return columnValue;
        }
        try {
            String b64 = columnValue.substring(PREFIX.length());
            byte[] combined = Base64.getDecoder().decode(b64);
            if (combined.length < GCM_IV_LENGTH_BYTES + 1) {
                throw new IllegalArgumentException("truncated ciphertext");
            }
            ByteBuffer buf = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            buf.get(iv);
            byte[] cipherBytes = new byte[buf.remaining()];
            buf.get(cipherBytes);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid MFA secret ciphertext (corrupt identity_users.mfa_secret?)", e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Failed to decrypt MFA secret (wrong identity.mfa-secret.encryption.key or corrupt data?)",
                    e);
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
