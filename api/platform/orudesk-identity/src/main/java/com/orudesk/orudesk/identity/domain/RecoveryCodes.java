package com.orudesk.orudesk.identity.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * One-time codes for when the phone is lost (Day 67): ten of them, each "ABCD-EFGH-JKLM" (12 base32 characters,
 * 60 random bits). Shown once; only their SHA-256 hashes are stored. 60 bits is far beyond online guessing (login
 * attempts are limited); against a stolen database a plain hash is the trade-off for a fast lookup.
 */
public final class RecoveryCodes {

    public static final int COUNT = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RecoveryCodes() {
    }

    public static List<String> generate() {
        List<String> codes = new ArrayList<>(COUNT);
        for (int i = 0; i < COUNT; i++) {
            byte[] bytes = new byte[8];
            RANDOM.nextBytes(bytes);
            String raw = Base32.encode(bytes).substring(0, 12);
            codes.add(raw.substring(0, 4) + "-" + raw.substring(4, 8) + "-" + raw.substring(8, 12));
        }
        return codes;
    }

    /** Spaces, dashes and case don't matter when a code is typed back. */
    public static String hash(String code) {
        String normal = code.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normal.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in Java", e);
        }
    }

    /** Does this look like a recovery code rather than six digits? */
    public static boolean looksLikeOne(String typed) {
        return typed.replace("-", "").replace(" ", "").length() == 12;
    }
}
