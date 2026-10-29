package com.orudesk.orudesk.identity.application.port.in;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ClientInfoTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0 | Edge on Windows",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15 | Safari on macOS",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1 | Safari on iOS",
            "Mozilla/5.0 (X11; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0 | Firefox on Linux",
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0 Mobile Safari/537.36 OPR/90.0 | Opera on Android",
            "okhttp/4.12.0 | okhttp",
    })
    void namesTheDeviceRoughly(String userAgent, String expected) {
        assertThat(ClientInfo.deviceName(userAgent)).isEqualTo(expected);
    }

    @Test
    void aClientThatSaysNothingIsAnUnknownDevice() {
        assertThat(ClientInfo.of(null, null)).isEqualTo(new ClientInfo("Unknown device", ""));
        assertThat(ClientInfo.of("   ", " 203.0.113.7 ")).isEqualTo(new ClientInfo("Unknown device", "203.0.113.7"));
    }
}
