package com.orudesk.orudesk.identity.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TotpTest {

    /** RFC 6238 appendix B's SHA-1 key. */
    private static final byte[] RFC_SECRET = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    /** RFC 6238's test vectors, cut to six digits (the RFC prints eight). */
    @ParameterizedTest
    @CsvSource({"59, 287082", "1111111109, 081804", "1111111111, 050471", "1234567890, 005924", "2000000000, 279037"})
    void matchesTheRfcTestVectors(long epochSecond, String expected) {
        assertThat(Totp.code(RFC_SECRET, Totp.step(Instant.ofEpochSecond(epochSecond)))).isEqualTo(expected);
    }

    @Test
    void acceptsTheStepBeforeAndAfterButNotFurther() {
        Instant now = Instant.ofEpochSecond(1_000_000_000);
        long step = Totp.step(now);

        assertThat(Totp.verify(RFC_SECRET, Totp.code(RFC_SECRET, step - 1), now, 0)).hasValue(step - 1);
        assertThat(Totp.verify(RFC_SECRET, Totp.code(RFC_SECRET, step + 1), now, 0)).hasValue(step + 1);
        assertThat(Totp.verify(RFC_SECRET, Totp.code(RFC_SECRET, step - 2), now, 0)).isEmpty();
    }

    @Test
    void aCodeWorksOnce() {
        Instant now = Instant.ofEpochSecond(1_000_000_000);
        long step = Totp.step(now);
        String code = Totp.code(RFC_SECRET, step);

        assertThat(Totp.verify(RFC_SECRET, code, now, step - 1)).hasValue(step);
        assertThat(Totp.verify(RFC_SECRET, code, now, step)).isEmpty();   // already used
    }

    @Test
    void refusesWhatIsNotSixDigits() {
        Instant now = Instant.ofEpochSecond(59);
        assertThat(Totp.verify(RFC_SECRET, "28708", now, 0)).isEmpty();
        assertThat(Totp.verify(RFC_SECRET, "28708x", now, 0)).isEmpty();
        assertThat(Totp.verify(RFC_SECRET, "287 082", now, 0)).isPresent();   // spaces as some apps show them
    }

    @Test
    void base32RoundTripsAndIgnoresGroupingAndCase() {
        byte[] bytes = {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef, 1, 2, 3};
        String text = Base32.encode(bytes);

        assertThat(Base32.encode("foobar".getBytes(StandardCharsets.US_ASCII))).isEqualTo("MZXW6YTBOI");   // RFC 4648
        assertThat(Base32.decode(text)).isEqualTo(bytes);
        assertThat(Base32.decode(text.toLowerCase().substring(0, 4) + " " + text.substring(4))).isEqualTo(bytes);
    }

    @Test
    void recoveryCodesAreTenDistinctGroupsAndHashTheSameHoweverTyped() {
        var codes = RecoveryCodes.generate();

        assertThat(codes).hasSize(10).doesNotHaveDuplicates().allMatch(c -> c.matches("[A-Z2-7]{4}-[A-Z2-7]{4}-[A-Z2-7]{4}"));
        String code = codes.getFirst();
        assertThat(RecoveryCodes.hash(code.toLowerCase().replace("-", " "))).isEqualTo(RecoveryCodes.hash(code));
        assertThat(RecoveryCodes.looksLikeOne(code)).isTrue();
        assertThat(RecoveryCodes.looksLikeOne("123456")).isFalse();
    }
}
