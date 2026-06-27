package com.openfinova.banking.tan.crypto;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.openfinova.banking.setup.api.DateTimeService;
import com.openfinova.banking.tan.cache.TanShortLivedCache;
import com.openfinova.banking.tan.exception.TanCodeAlreadyUsedException;

/**
 * Generates and verifies dynamic, transaction-bound TAN codes for payment Strong Customer
 * Authentication (SCA).
 *
 * Regulatory context (PSD2 / EBA RTS on SCA)
 *   Remote electronic payments require SCA with dynamic linking: the authentication code
 *   must be specific to the transaction amount and the payee (creditor). A generic time-based
 *   OTP (e.g. RFC 6238 TOTP alone) does not satisfy this because the same code could authorize
 *   a different amount or beneficiary. This service therefore derives each code from an HMAC over
 *   {@code transactionId}, amount in minor units, and normalized payee IBAN, plus a time step.
 *
 * Cryptographic model
 *   Symmetric HMAC-SHA256 using the 32-byte device-shared secret ({@code tanSecret}). The mobile
 *   app generates codes with the same algorithm ({@code minab-tan-app} {@code TanCodeService});
 *   the server only verifies. Dynamic truncation follows RFC 4226 (HOTP) to produce decimal digits.
 *
 * Design decisions
 *   {@link #TIME_STEP_SECONDS} = 90 — shorter validity window than typical 30 s TOTP, reducing
 *   exposure of a shoulder-surfed code while still allowing the user time to confirm a payment.
 *   {@link #CODE_DIGITS} = 8 — ~10^8 possibilities; acceptable for single-transaction codes
 *   combined with replay prevention and channel rate limiting elsewhere.
 *   {@link #ALLOWED_DRIFT_STEPS} = 1 — accepts the current step and ±1 adjacent step (±90 s) to
 *   tolerate clock skew between device and server without widening the dynamic-linking window
 *   excessively.
 *   Amount as minor units — {@link #buildPayload} uses integer minor units via
 *   {@code movePointRight(2)} to avoid floating-point ambiguity in the authenticated payload.
 *   IBAN normalization — whitespace stripped and uppercased so formatting differences do not
 *   break verification or weaken payee binding.
 *   Per-transaction replay cache — {@link #verifyAndRecordReplay} rejects reuse of a TAN for the
 *   same {@code transactionId}, supporting the one-time nature expected of SCA for a given payment.
 *
 * @see com.openfinova.banking.tan.service.TanAuthorizationService#verifyTan
 * @see com.openfinova.banking.tan.service.TanDeviceService#confirmDevice
 */
@Service
public class TanCodeService {

    /** Time bucket length in seconds; must match the TAN mobile app. */
    public static final int TIME_STEP_SECONDS = 90;

    /** Number of decimal digits in a TAN or confirmation code. */
    public static final int CODE_DIGITS = 8;

    /** Clock-skew tolerance expressed in {@link #TIME_STEP_SECONDS} buckets (each side). */
    public static final int ALLOWED_DRIFT_STEPS = 1;

    /**
     * How long a successful verification is remembered to block replay. Covers two time steps plus
     * margin for pending-authorization latency.
     */
    public static final Duration REPLAY_CACHE_TTL = Duration.ofSeconds(180);

    private static final String HMAC_ALG = "HmacSHA256";
    private static final String REPLAY_KEY_PREFIX = "tan-verified:";

    private final DateTimeService dateTimeService;
    private final TanShortLivedCache replayCache;

    public TanCodeService(DateTimeService dateTimeService, TanShortLivedCache replayCache) {
        this.dateTimeService = dateTimeService;
        this.replayCache = replayCache;
    }

    /**
     * Verifies a payment TAN and atomically records replay prevention for the transaction.
     *
     * @param transactionId TP transaction being authorized (part of the HMAC payload)
     * @param tanSecretBytes decoded 32-byte device secret
     * @param tanCode user-entered or channel-submitted code
     * @param amount payment amount from the {@code TanPendingAuthorization} snapshot
     * @param normalizedIban payee IBAN after {@link #normalizeIban}
     * @throws TanCodeAlreadyUsedException if this transaction was already verified successfully
     * @throws IllegalArgumentException if the code does not match any allowed time step
     */
    public void verifyAndRecordReplay(UUID transactionId, byte[] tanSecretBytes, String tanCode, BigDecimal amount,
            String normalizedIban) {
        String replayKey = REPLAY_KEY_PREFIX + transactionId;
        if (replayCache.exists(replayKey)) {
            throw new TanCodeAlreadyUsedException("TAN code for this transaction has already been used");
        }
        if (!verifyCode(tanSecretBytes, tanCode, transactionId, amount, normalizedIban)) {
            throw new IllegalArgumentException("Invalid TAN code");
        }
        replayCache.put(replayKey, tanCode, REPLAY_CACHE_TTL);
    }

    /**
     * Verifies the one-shot enrollment confirmation code (no time step, no transaction binding).
     *
     * Used during device enrollment to prove possession of the freshly registered secret on a
     * trusted channel before the device becomes {@code ACTIVE}. Comparison uses
     * {@link MessageDigest#isEqual} for constant-time equality.
     */
    public boolean verifyConfirmationCode(byte[] tanSecretBytes, String code, String confirmationInput) {
        if (code == null || code.length() != CODE_DIGITS) {
            return false;
        }
        String expected = truncateToDigits(hmacSha256(tanSecretBytes, confirmationInput));
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Checks whether {@code code} matches the HMAC for the given payment parameters in the current
     * or adjacent time steps.
     */
    public boolean verifyCode(byte[] tanSecretBytes, String code, UUID transactionId, BigDecimal amount,
            String normalizedIban) {
        if (code == null || code.length() != CODE_DIGITS) {
            return false;
        }
        long currentStep = dateTimeService.instant().getEpochSecond() / TIME_STEP_SECONDS;
        String payloadBase = buildPayload(transactionId, amount, normalizedIban);
        for (int drift = -ALLOWED_DRIFT_STEPS; drift <= ALLOWED_DRIFT_STEPS; drift++) {
            String payload = payloadBase + "|" + (currentStep + drift);
            String expected = truncateToDigits(hmacSha256(tanSecretBytes, payload));
            if (MessageDigest
                    .isEqual(expected.getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Canonical IBAN form for inclusion in the HMAC payload (PSD2 payee binding).
     */
    public static String normalizeIban(String iban) {
        return iban == null ? "" : iban.replaceAll("\\s+", "").toUpperCase();
    }

    /**
     * Builds the transaction-specific portion of the HMAC message: {@code txnId|minorUnits|iban}.
     *
     * Must remain stable and identical on server and mobile client; any change requires a
     * coordinated app release.
     */
    public static String buildPayload(UUID transactionId, BigDecimal amount, String normalizedIban) {
        long minorUnits = amount.movePointRight(2).longValueExact();
        return transactionId + "|" + minorUnits + "|" + normalizedIban;
    }

    /** RFC 4226 dynamic truncation to {@link #CODE_DIGITS} decimal digits. */
    private static String truncateToDigits(byte[] hmac) {
        int offset = hmac[hmac.length - 1] & 0x0F;
        int binary = ((hmac[offset] & 0x7F) << 24) | ((hmac[offset + 1] & 0xFF) << 16)
                | ((hmac[offset + 2] & 0xFF) << 8) | (hmac[offset + 3] & 0xFF);
        int mod = (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", binary % mod);
    }

    private static byte[] hmacSha256(byte[] key, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(key, HMAC_ALG));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 failed", e);
        }
    }
}
