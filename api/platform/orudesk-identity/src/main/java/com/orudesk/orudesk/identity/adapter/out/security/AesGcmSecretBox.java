package com.orudesk.orudesk.identity.adapter.out.security;

import com.orudesk.orudesk.identity.application.port.out.SecretBox;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM (Day 67): authenticated encryption, so a changed ciphertext fails to open instead of decrypting to
 * garbage. Each seal uses a fresh random 96-bit nonce, stored in front of the ciphertext. The stored text starts
 * with "v1:" so a later key (rotation) can be told apart from this one.
 *
 * The key is ORUDESK_MFA_KEY: 32 random bytes in base64 ({@code openssl rand -base64 32}). Not the JWT secret:
 * rotating that one must not make every authenticator app useless.
 */
@Component
class AesGcmSecretBox implements SecretBox {

    private static final String VERSION = "v1:";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;

    AesGcmSecretBox(@Value("${orudesk.identity.mfa.key:}") String base64Key) {
        if (base64Key.isBlank()) {
            this.key = null;
            return;
        }
        byte[] bytes = Base64.getDecoder().decode(base64Key.strip());
        if (bytes.length != 32) {
            throw new IllegalStateException("orudesk.identity.mfa.key must be 32 bytes in base64 (openssl rand -base64 32)");
        }
        this.key = new SecretKeySpec(bytes, "AES");
    }

    @Override
    public boolean available() {
        return key != null;
    }

    @Override
    public String seal(byte[] secret) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] sealed = cipher.doFinal(secret);
            return VERSION + Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(nonce.length + sealed.length).put(nonce).put(sealed).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("could not encrypt", e);
        }
    }

    @Override
    public byte[] open(String sealed) {
        if (!sealed.startsWith(VERSION)) {
            throw new IllegalStateException("unknown secret format");
        }
        try {
            byte[] all = Base64.getDecoder().decode(sealed.substring(VERSION.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, requireKey(),
                    new GCMParameterSpec(TAG_BITS, Arrays.copyOfRange(all, 0, NONCE_BYTES)));
            return cipher.doFinal(all, NONCE_BYTES, all.length - NONCE_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("could not decrypt: wrong ORUDESK_MFA_KEY, or the data was changed", e);
        }
    }

    private SecretKey requireKey() {
        if (key == null) {
            throw new IllegalStateException("orudesk.identity.mfa.key is not set");
        }
        return key;
    }
}
