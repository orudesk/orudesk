package com.orudesk.orudesk.identity.domain;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.OptionalLong;

/**
 * Time-based one-time passwords, RFC 6238 (Day 67): the six digits an authenticator app shows. The app and the
 * server share a secret; both compute HMAC-SHA1(secret, the number of 30-second steps since 1970) and cut it down
 * to six digits (RFC 4226's "dynamic truncation"). Same secret, same clock: same code. No network involved.
 *
 * SHA-1 is still what every authenticator app supports by default; its collision weakness doesn't matter for an
 * HMAC with a secret key.
 */
public final class Totp {

    public static final int DIGITS = 6;
    public static final long STEP_SECONDS = 30;

    /** Phones' clocks drift: a code from the step before or after also counts (RFC 6238 §5.2). */
    private static final int ALLOWED_DRIFT_STEPS = 1;

    private Totp() {
    }

    public static long step(Instant time) {
        return Math.floorDiv(time.getEpochSecond(), STEP_SECONDS);
    }

    public static String code(byte[] secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            return String.format("%0" + DIGITS + "d", binary % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 is always available in Java", e);
        }
    }

    /**
     * The step the code belongs to, if it's right for now (give or take one step) and newer than
     * {@code lastUsedStep}: a code works once, so someone who watched it being typed can't use it again.
     */
    public static OptionalLong verify(byte[] secret, String code, Instant now, long lastUsedStep) {
        String typed = code.replace(" ", "");
        if (typed.length() != DIGITS || !typed.chars().allMatch(Character::isDigit)) {
            return OptionalLong.empty();
        }
        long current = step(now);
        for (long step = current - ALLOWED_DRIFT_STEPS; step <= current + ALLOWED_DRIFT_STEPS; step++) {
            // Compared in constant time, so the response time says nothing about how many digits were right
            boolean matches = MessageDigest.isEqual(code(secret, step).getBytes(StandardCharsets.US_ASCII),
                    typed.getBytes(StandardCharsets.US_ASCII));
            if (matches && step > lastUsedStep) {
                return OptionalLong.of(step);
            }
        }
        return OptionalLong.empty();
    }
}
