package com.orudesk.orudesk.identity.application.port.out;

/**
 * Encrypts small secrets the server must read back (Day 67): a TOTP secret can't be hashed like a password,
 * because the server needs it to compute codes. Encrypted with a key kept outside the database, a stolen backup
 * alone doesn't let anyone generate a user's codes.
 */
public interface SecretBox {

    /** False when no key is configured: features that need it say they're unavailable. */
    boolean available();

    String seal(byte[] secret);

    byte[] open(String sealed);
}
