package com.orudesk.orudesk.shared.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void theCallersIdIsKeptForTheLogsAndTheResponse() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/people");
        request.addHeader("X-Request-Id", "3f2a9c1e-web");
        var response = new MockHttpServletResponse();
        var seenInLogs = new AtomicReference<String>();

        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                seenInLogs.set(MDC.get("request_id"));
            }
        });

        assertThat(seenInLogs.get()).isEqualTo("3f2a9c1e-web");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("3f2a9c1e-web");
        assertThat(MDC.get("request_id")).isNull();   // gone after the request: the thread is reused
    }

    @Test
    void withoutOneTheApiMakesOne() throws Exception {
        var response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getHeader("X-Request-Id")).hasSize(36);
    }

    @Test
    void anIdThatCouldForgeLogLinesIsReplaced() {
        assertThat(RequestIdFilter.requestIdFrom("abc\n2026-10-26 ERROR forged")).hasSize(36);
        assertThat(RequestIdFilter.requestIdFrom("x".repeat(65))).hasSize(36);
        assertThat(RequestIdFilter.requestIdFrom("short")).hasSize(36);
        assertThat(RequestIdFilter.requestIdFrom("abcd-1234")).isEqualTo("abcd-1234");
    }
}
