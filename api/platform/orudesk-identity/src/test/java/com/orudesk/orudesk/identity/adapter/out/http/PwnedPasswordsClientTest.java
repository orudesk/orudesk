package com.orudesk.orudesk.identity.adapter.out.http;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Against a local stand-in for the range API: tests never call the real service. */
class PwnedPasswordsClientTest {

    /** SHA-1 of "password" is 5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8. */
    private static final String PASSWORD_PREFIX = "5BAA6";
    private static final String PASSWORD_SUFFIX = "1E4C9B93F3F0682250B6CF8331B7EE68FD8";

    private HttpServer server;
    private final List<String> requested = new ArrayList<>();
    private volatile String answer = "";
    private volatile int status = 200;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/range/", exchange -> {
            requested.add(exchange.getRequestURI().getPath() + " padding=" + exchange.getRequestHeaders().getFirst("Add-Padding"));
            byte[] body = answer.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void sendsOnlyTheFirstFiveCharactersOfTheHashAndFindsTheRest() {
        answer = "0018A45C4D1DEF81644B54AB7F969B88D65:1\r\n" + PASSWORD_SUFFIX + ":10434004\r\n";

        assertThat(client(true).isBreached("password")).isTrue();
        assertThat(requested).containsExactly("/range/" + PASSWORD_PREFIX + " padding=true");
    }

    @Test
    void aPaddingLineIsNotAHit() {
        answer = PASSWORD_SUFFIX + ":0\r\n";

        assertThat(client(true).isBreached("password")).isFalse();
    }

    @Test
    void aFailingServiceLetsThePasswordThrough() {
        status = 503;

        assertThat(client(true).isBreached("password")).isFalse();
    }

    @Test
    void switchedOffItAsksNobody() {
        answer = PASSWORD_SUFFIX + ":10434004\r\n";

        assertThat(client(false).isBreached("password")).isFalse();
        assertThat(requested).isEmpty();
    }

    private PwnedPasswordsClient client(boolean enabled) {
        return new PwnedPasswordsClient(enabled, "http://127.0.0.1:" + server.getAddress().getPort() + "/range/",
                Duration.ofSeconds(2));
    }
}
