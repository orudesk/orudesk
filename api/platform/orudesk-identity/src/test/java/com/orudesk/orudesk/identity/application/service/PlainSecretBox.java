package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.out.SecretBox;

import java.util.Base64;

/** No encryption, for unit tests: AesGcmSecretBox has its own test. */
class PlainSecretBox implements SecretBox {

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String seal(byte[] secret) {
        return Base64.getEncoder().encodeToString(secret);
    }

    @Override
    public byte[] open(String sealed) {
        return Base64.getDecoder().decode(sealed);
    }
}
