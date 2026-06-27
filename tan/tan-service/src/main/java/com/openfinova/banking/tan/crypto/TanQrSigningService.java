package com.openfinova.banking.tan.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Produces and verifies message authentication codes (MACs) on payment authorization QR payloads.
 *
 * Role in the SCA flow
 *   The banking channel embeds {@code txnId} and {@code mac} in the deep link scanned by the TAN
 *   app. This MAC is not the user's TAN and does not satisfy PSD2 dynamic linking by itself; it
 *   protects the handoff between channels so an attacker cannot craft a QR that points the app
 *   at an arbitrary transaction id before the app calls {@code getPendingTransaction}.
 *
 * Cryptographic design
 *   HMAC-SHA256 over the string form of {@code transactionId}, keyed by {@code tan.qr.signing.key}
 *   (server secret from environment / secrets manager, distinct from device {@code tanSecret} and
 *   {@link TanSecretEncryptionService} database key — separation of duties).
 *   The full 256-bit digest is hex-encoded rather than truncated because the MAC is never typed
 *   by a human; length maximizes collision resistance for identifier integrity.
 *
 * Verification uses {@link MessageDigest#isEqual} for constant-time comparison. The mobile client
 * should validate the MAC before displaying payment details or generating a TAN.
 *
 * @see com.openfinova.banking.tan.service.TanAuthorizationService#buildPaymentQr
 */
@Service
public class TanQrSigningService {

    private static final String HMAC_ALG = "HmacSHA256";

    private final byte[] signingKey;

    public TanQrSigningService(@Value("${tan.qr.signing.key}") String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalStateException("tan.qr.signing.key must be non-blank");
        }
        this.signingKey = rawKey.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Computes the MAC appended to payment authorization QR URLs as {@code mac=...}.
     *
     * @param transactionId TP transaction the QR opens in the TAN app
     * @return lowercase hex HMAC-SHA256 digest
     */
    public String signTransactionId(UUID transactionId) {
        return bytesToHex(hmac(transactionId.toString()));
    }

    /**
     * Validates that {@code mac} was issued by this server for {@code transactionId}.
     *
     * @return {@code false} if {@code mac} is missing or does not match; never throws for bad input
     */
    public boolean verifyMac(UUID transactionId, String mac) {
        if (mac == null || mac.isBlank()) {
            return false;
        }
        String expected = signTransactionId(transactionId);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), mac.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] hmac(String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALG));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("QR signing HMAC failed", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
