package com.orudesk.orudesk;

import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Import;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

/** A real Tomcat on a random port, to check how it runs requests. MockMvc has no Tomcat, so it can't tell. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class VirtualThreadsTest {

    @Autowired
    private ServletWebServerApplicationContext context;

    @Test
    void tomcatRunsEachRequestOnAVirtualThread() {
        TomcatWebServer server = (TomcatWebServer) context.getWebServer();
        Executor executor = server.getTomcat().getConnector().getProtocolHandler().getExecutor();

        assertThat(executor).isInstanceOf(VirtualThreadExecutor.class);
    }
}
