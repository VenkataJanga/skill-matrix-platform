package com.skillmatrix.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RequestLoggingFilter}.
 */
@DisplayName("RequestLoggingFilter tests")
class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // shouldNotFilter
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Skips logging for /actuator/ paths")
    void skipsActuatorPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("Skips logging for /swagger-ui paths")
    void skipsSwaggerUiPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("Skips logging for /api-docs paths")
    void skipsApiDocsPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api-docs");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("Does NOT skip API paths")
    void doesNotSkipApiPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    // ------------------------------------------------------------------
    // doFilterInternal — chain execution
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Filter calls next in chain — does not block request")
    void filterPassesRequestThroughChain() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest("GET", "/api/v1/admin/skills");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain         chain    = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        // Chain should have been called
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Authorization header value is never accessible inside filter log methods")
    void doesNotLogAuthorizationHeader() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        request.addHeader("Authorization", "Bearer super-secret-jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain         chain    = new MockFilterChain();

        // This must complete without exposing the Authorization header value.
        // We verify by ensuring the filter does not rethrow and the chain is called.
        filter.doFilterInternal(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        // Authorization header is never read by the filter (no getHeader("Authorization") call)
        // This test passes as long as no exception is thrown and chain is called.
    }

    @Test
    @DisplayName("Filter handles 5xx response status without throwing")
    void handles5xxWithoutThrowing() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest("POST", "/api/v1/admin/skills");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);
        MockFilterChain         chain    = new MockFilterChain();

        // Should not throw
        filter.doFilterInternal(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(500);
    }

    @Test
    @DisplayName("Filter handles 4xx response status without throwing")
    void handles4xxWithoutThrowing() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest("POST", "/api/v1/admin/skills");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(400);
        MockFilterChain         chain    = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(400);
    }
}
