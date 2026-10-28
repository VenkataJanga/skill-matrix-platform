package com.skillmatrix.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CorrelationIdFilter}.
 */
@DisplayName("CorrelationIdFilter tests")
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    // ------------------------------------------------------------------
    // resolveCorrelationId
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Generates a new UUID when X-Correlation-Id header is missing")
    void generatesUuidWhenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String id = filter.resolveCorrelationId(request);
        assertThat(id).isNotNull().hasSize(36);
        // Must be a valid UUID
        UUID.fromString(id); // throws if invalid
    }

    @Test
    @DisplayName("Uses incoming X-Correlation-Id when it is a valid UUID")
    void usesIncomingHeaderWhenValidUuid() {
        String incomingId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, incomingId);

        String id = filter.resolveCorrelationId(request);
        assertThat(id).isEqualTo(incomingId);
    }

    @Test
    @DisplayName("Generates new UUID when incoming header is not a valid UUID (log injection guard)")
    void generatesNewUuidForInvalidHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "not-a-valid-uuid-value-at-all!!!");

        String id = filter.resolveCorrelationId(request);
        assertThat(id).isNotNull().hasSize(36);
        // Should not equal the invalid input
        assertThat(id).isNotEqualTo("not-a-valid-uuid-value-at-all!!!");
    }

    @Test
    @DisplayName("Generates new UUID when incoming header is blank")
    void generatesNewUuidWhenHeaderIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "   ");

        String id = filter.resolveCorrelationId(request);
        assertThat(id).isNotNull().hasSize(36);
    }

    // ------------------------------------------------------------------
    // doFilterInternal — full filter chain
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Sets X-Correlation-Id response header after filtering")
    void setsResponseHeader() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain         chain    = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        String responseHeader = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(responseHeader).isNotNull().hasSize(36);
    }

    @Test
    @DisplayName("Propagates incoming correlation ID to response header")
    void propagatesIncomingCorrelationId() throws Exception {
        String incomingId = UUID.randomUUID().toString();
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, incomingId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain         chain    = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo(incomingId);
    }

    @Test
    @DisplayName("MDC is cleared after filter execution")
    void mdcIsClearedAfterFilter() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain         chain    = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        // MDC must be empty after the filter completes
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
