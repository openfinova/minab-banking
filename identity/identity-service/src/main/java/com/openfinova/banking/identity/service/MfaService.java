package com.openfinova.banking.identity.service;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * TOTP (RFC 6238) multi-factor authentication service.
 * Generates secrets, validates codes, and manages recovery codes.
 */
@Service
public class MfaService {

    private static final int SECRET_BYTES = 20;
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int ALLOWED_DRIFT_STEPS = 1;
    private static final int RECOVERY_CODE_COUNT = 10;
    private static final int RECOVERY_CODE_LENGTH = 8;
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final PasswordEncoder passwordEncoder;
    private final DateTimeService dateTimeService;
    private final SecureRandom secureRandom = new SecureRandom();

    public MfaService(PasswordEncoder passwordEncoder, DateTimeService dateTimeService) {
        this.passwordEncoder = passwordEncoder;
        this.dateTimeService = dateTimeService;
    }

    /** Generate a Base32-encoded TOTP shared secret. */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** Build the otpauth:// URI suitable for QR-code rendering. */
    public String generateQrUri(String secret, String username, String issuer) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=%d&period=%d",
                urlEncode(issuer),
                urlEncode(username),
                secret,
                urlEncode(issuer),
                CODE_DIGITS,
                TIME_STEP_SECONDS);
    }

    /** Validate a TOTP code against the secret, allowing +/- one time step of drift. */
    public boolean verifyCode(String base32Secret, String code) {
        if (code == null || code.length() != CODE_DIGITS) {
            return false;
        }
        byte[] key = base32Decode(base32Secret);
        long currentStep = dateTimeService.instant().getEpochSecond() / TIME_STEP_SECONDS;

        for (int drift = -ALLOWED_DRIFT_STEPS; drift <= ALLOWED_DRIFT_STEPS; drift++) {
            String expected = generateTotpCode(key, currentStep + drift);
            if (expected.equals(code)) {
                return true;
            }
        }
        return false;
    }

    /** Generate plaintext recovery codes (shown to user once). */
    public List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>(RECOVERY_CODE_COUNT);
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            StringBuilder sb = new StringBuilder(RECOVERY_CODE_LENGTH);
            for (int j = 0; j < RECOVERY_CODE_LENGTH; j++) {
                sb.append(secureRandom.nextInt(10));
            }
            codes.add(sb.toString());
        }
        return codes;
    }

    /** Hash recovery codes for persistent storage. */
    public Set<String> hashRecoveryCodes(List<String> plaintextCodes) {
        return plaintextCodes.stream().map(passwordEncoder::encode).collect(Collectors.toSet());
    }

    /**
     * True if {@code code} has the shape of a recovery code (length and decimal digits).
     * Used to route MFA challenge input: 6-digit TOTP vs 8-digit recovery.
     */
    public boolean isRecoveryCodeFormat(String code) {
        if (code == null || code.length() != RECOVERY_CODE_LENGTH) {
            return false;
        }
        for (int i = 0; i < code.length(); i++) {
            if (!Character.isDigit(code.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Attempt to consume a recovery code. Returns true if matched
     * (and removes the used code hash from the user's set).
     */
    public boolean consumeRecoveryCode(BankingUser user, String code) {
        Iterator<String> it = user.getMfaRecoveryCodes().iterator();
        while (it.hasNext()) {
            if (passwordEncoder.matches(code, it.next())) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Generate the TOTP code for a given secret and time step.
     *
     * @param key the secret key as a byte array
     * @param timeStep the time step for which to generate the code
     * @return the generated TOTP code as a string of digits
     */
    private String generateTotpCode(byte[] key, long timeStep) {
        byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();
        byte[] hash = hmacSha1(key, timeBytes);

        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);

        int otp = binary % (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", otp);
    }

    /**
     * Compute HMAC-SHA1 of the given data using the given key.
     *
     * @param key the secret key as a byte array
     * @param data the data to be hashed
     * @return the HMAC-SHA1 hash as a byte array
     */
    private static byte[] hmacSha1(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA1 unavailable", e);
        }
    }

    /**
     * Encode bytes to Base32 string without padding, using the RFC 4648 alphabet.
     *
     * @param data the byte array to encode
     * @return the Base32 encoded string
     */
    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return sb.toString();
    }

    /**
     * Decode a Base32-encoded string to bytes, ignoring non-alphabet characters.
     *
     * @param encoded the Base32 encoded string
     * @return the decoded byte array
     */
    private static byte[] base32Decode(String encoded) {
        encoded = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int outputLength = encoded.length() * 5 / 8;
        byte[] result = new byte[outputLength];
        int buffer = 0, bitsLeft = 0, index = 0;
        for (char c : encoded.toCharArray()) {
            buffer = (buffer << 5) | BASE32_ALPHABET.indexOf(c);
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return result;
    }

    /**
     * URL-encode a string, replacing spaces and '@' characters with their percent-encoded equivalents.
     *
     * @param value the string to encode
     * @return the URL-encoded string
     */
    private static String urlEncode(String value) {
        return value.replace(" ", "%20").replace("@", "%40");
    }
}
