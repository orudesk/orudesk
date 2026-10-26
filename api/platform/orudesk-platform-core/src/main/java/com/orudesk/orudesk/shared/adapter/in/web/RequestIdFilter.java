package com.orudesk.orudesk.shared.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every request an id, so its log lines can be found together, and a user's "it failed at 10:42" can be
 * matched to the right ones. The web app sends one (X-Request-Id) so the browser, Caddy and the API share it;
 * otherwise we make one. Either way it's in the logs as request_id and goes back in the response header.
 *
 * First of all filters (before security), so even a refused request has an id in its log lines.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "request_id";

    /** A caller's id goes into our logs as is, so only short, plain ones: no log injection, no huge values. */
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = requestIdFrom(request.getHeader(HEADER));
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);   // the thread serves someone else next
        }
    }

    static String requestIdFrom(@Nullable String header) {
        return header != null && SAFE.matcher(header).matches() ? header : UUID.randomUUID().toString();
    }
}
