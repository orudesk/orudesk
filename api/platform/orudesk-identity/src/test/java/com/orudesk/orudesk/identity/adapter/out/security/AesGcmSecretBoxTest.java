package com.orudesk.orudesk.identity.adapter.out.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmSecretBoxTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_KEY = Base64.getEncoder().encodeToString("another-key-of-32-bytes-exactly!".getBytes());

    @Test
    void sealsAndOpensWithADifferentNonceEachTime() {
        AesGcmSecretBox box = new AesGcmSecretBox(KEY);
        byte[] secret = "twenty bytes secret!".getBytes();

        String once = box.seal(secret);
        String twice = box.seal(secret);

        assertThat(once).startsWith("v1:").isNotEqualTo(twice);
        assertThat(box.open(once)).isEqualTo(secret);
    }

    @Test
    void theWrongKeyOrAChangedTextFailsInsteadOfGivingGarbage() {
        String sealed = new AesGcmSecretBox(KEY).seal("secret".getBytes());
        byte[] raw = Base64.getDecoder().decode(sealed.substring(3));
        raw[raw.length - 1] ^= 1;
        String tampered = "v1:" + Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> new AesGcmSecretBox(OTHER_KEY).open(sealed)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new AesGcmSecretBox(KEY).open(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void withoutAKeyItIsUnavailable() {
        assertThat(new AesGcmSecretBox("").available()).isFalse();
        assertThatThrownBy(() -> new AesGcmSecretBox("c2hvcnQ=")).isInstanceOf(IllegalStateException.class);
    }
}
