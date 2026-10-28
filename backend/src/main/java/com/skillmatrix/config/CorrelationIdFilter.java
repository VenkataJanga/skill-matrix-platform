package com.skillmatrix.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that establishes a correlation ID for every incoming HTTP request.
 *
 * <p>Behaviour:
 * <ul>
 *   <li>If the request already contains an {@code X-Correlation-Id} header, that value is used.</li>
 *   <li>Otherwise a new UUID is generated.</li>
 *   <li>The correlation ID is stored in the SLF4J {@link MDC} under key {@code correlationId}
 *       so that every log statement written during the request automatically includes it.</li>
 *   <li>The correlation ID is written to the response as an {@code X-Correlation-Id} header
 *       so callers can trace their requests end-to-end.</li>
 *   <li>MDC is unconditionally cleared after the request completes to prevent thread-local leaks
 *       in pooled container threads.</li>
 * </ul>
 *
 * <p>Security: the filter validates that an incoming header is a well-formed UUID; if it is not,
 * a fresh UUID is generated to prevent log injection.
 */
@Component
@Order(1)   // run before RequestLoggingFilter and Spring Security
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME    = "X-Correlation-Id";
    public static final String MDC_KEY        = "correlationId";
    private static final int   UUID_LENGTH    = 36;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);
        try {
            MDC.put(MDC_KEY, correlationId);
            response.setHeader(HEADER_NAME, correlationId);
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Returns the incoming correlation ID if it looks like a valid UUID;
     * otherwise generates a fresh one.
     */
    String resolveCorrelationId(HttpServletRequest request) {
        String incoming = request.getHeader(HEADER_NAME);
        if (incoming != null && !incoming.isBlank() && incoming.length() == UUID_LENGTH) {
            // Basic guard against log injection: only accept UUID-shaped values
            try {
                UUID.fromString(incoming);
                return incoming;
            } catch (IllegalArgumentException ignored) {
                // fall through to generate new
            }
        }
        return UUID.randomUUID().toString();
    }
}
