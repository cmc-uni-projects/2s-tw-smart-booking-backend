package com.example.smart_booking_system.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Gives every HTTP request a correlation id (MDC "requestId", echoed as X-Request-Id) so all log
 * lines of one request can be grepped together. Requests that never reach a controller (401/403
 * from Spring Security, 404, static files) are logged here; controller calls are logged by
 * {@link LoggingAspect}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    static final String LOGGED_ATTRIBUTE = RequestIdFilter.class.getName() + ".logged";

    private static final Logger log = LoggerFactory.getLogger("http");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId = StringUtils.hasText(incoming) && incoming.length() <= 64
                ? incoming : UUID.randomUUID().toString().substring(0, 8);
        long start = System.nanoTime();
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            if (request.getAttribute(LOGGED_ATTRIBUTE) == null && !isQuiet(request)) {
                long ms = (System.nanoTime() - start) / 1_000_000;
                int status = response.getStatus();
                String line = "{} {} -> {} in {}ms (no handler)";
                if (status >= 400) log.warn(line, request.getMethod(), request.getRequestURI(), status, ms);
                else log.debug(line, request.getMethod(), request.getRequestURI(), status, ms);
            }
            MDC.remove(MDC_KEY);
        }
    }

    private static boolean isQuiet(HttpServletRequest request) {
        return "OPTIONS".equals(request.getMethod());
    }
}
