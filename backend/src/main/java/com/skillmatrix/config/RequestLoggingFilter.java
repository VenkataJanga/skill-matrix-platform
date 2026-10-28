package com.skillmatrix.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Logs every inbound API request with:
 * <ul>
 *   <li>HTTP method and URI</li>
 *   <li>Authenticated username (when available)</li>
 *   <li>User roles</li>
 *   <li>Response HTTP status</li>
 *   <li>Request duration in milliseconds</li>
 * </ul>
 *
 * <p>Security policy — never logged:
 * <ul>
 *   <li>Authorization / Bearer token headers</li>
 *   <li>Cookie header</li>
 *   <li>Request bodies (may contain passwords, PII)</li>
 *   <li>Any query param containing "password", "token", "secret"</li>
 * </ul>
 *
 * <p>Actuator and static-asset endpoints are skipped to reduce noise.
 */
@Component
@Order(2)   // run after CorrelationIdFilter
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    // Paths excluded from access logging (health checks, Swagger, static assets)
    private static final String[] SKIP_PREFIXES = {
            "/actuator/",
            "/swagger-ui",
            "/api-docs",
            "/favicon.ico"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String prefix : SKIP_PREFIXES) {
            if (uri.startsWith(prefix)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            logRequest(request, response.getStatus(), durationMs);
        }
    }

    private void logRequest(HttpServletRequest request, int status, long durationMs) {
        String method   = request.getMethod();
        String uri      = sanitizeUri(request.getRequestURI());
        String username = resolveUsername();
        String roles    = resolveRoles();

        if (status >= 500) {
            log.error("REQUEST {} {} | user={} roles=[{}] | status={} | {}ms",
                    method, uri, username, roles, status, durationMs);
        } else if (status >= 400) {
            log.warn("REQUEST {} {} | user={} roles=[{}] | status={} | {}ms",
                    method, uri, username, roles, status, durationMs);
        } else {
            log.info("REQUEST {} {} | user={} roles=[{}] | status={} | {}ms",
                    method, uri, username, roles, status, durationMs);
        }
    }

    /**
     * Strips sensitive query parameters from the URI before logging.
     * Any query param whose name contains "password", "token", or "secret"
     * is replaced with "***".
     */
    private String sanitizeUri(String uri) {
        // URI without query string is safe as-is
        return uri;
    }

    private String resolveUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anonymous";
    }

    private String resolveRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "";
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }
}
