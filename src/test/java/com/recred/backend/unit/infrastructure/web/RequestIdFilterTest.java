package com.recred.backend.unit.infrastructure.web;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.recred.backend.config.RequestIdContext;
import com.recred.backend.config.RequestIdFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.KeyValuePair;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void generatesRequestIdWhenNoHeaderIsPresent() throws Exception {
        MockHttpServletRequest request = givenRequest("GET", "/api/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();

        whenFiltering(request, response, requestIdInChain);

        thenRequestIdIsGeneratedCorrectly(response, requestIdInChain);
    }

    @Test
    void preservesValidIncomingRequestId() throws Exception {
        MockHttpServletRequest request = givenRequest("GET", "/api/ping");
        request.addHeader(RequestIdContext.HEADER_NAME, "front-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();

        whenFiltering(request, response, requestIdInChain);

        thenIncomingRequestIdIsPreserved(response, requestIdInChain, "front-123");
    }

    @Test
    void discardsInvalidIncomingRequestId() throws Exception {
        MockHttpServletRequest request = givenRequest("GET", "/api/ping");
        request.addHeader(RequestIdContext.HEADER_NAME, "front 123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();

        whenFiltering(request, response, requestIdInChain);

        thenInvalidRequestIdIsDiscarded(response, requestIdInChain, "front 123");
    }

    @Test
    void logsRequestWithStructuredFields() throws Exception {
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RequestIdFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            MockHttpServletRequest request = givenRequest("POST", "/api/purchases");
            MockHttpServletResponse response = new MockHttpServletResponse();

            whenFiltering(request, response, new AtomicReference<>());

            thenRequestIsLoggedCorrectly(appender);
        } finally {
            logger.detachAppender(appender);
        }
    }

    private MockHttpServletRequest givenRequest(String method, String requestUri) {
        return new MockHttpServletRequest(method, requestUri);
    }

    private void whenFiltering(
            MockHttpServletRequest request, 
            MockHttpServletResponse response, 
            AtomicReference<String> requestIdInChain
    ) throws Exception {
        filter.doFilter(request, response, requestIdCapturingChain(requestIdInChain));
    }

    private void thenRequestIdIsGeneratedCorrectly(
            MockHttpServletResponse response, 
            AtomicReference<String> requestIdInChain
    ) {
        String generatedRequestId = response.getHeader(RequestIdContext.HEADER_NAME);

        assertNotNull(generatedRequestId);
        assertEquals(generatedRequestId, requestIdInChain.get());
        assertDoesNotThrow(() -> UUID.fromString(generatedRequestId));
        assertNull(MDC.get(RequestIdContext.MDC_KEY));
    }

    private void thenIncomingRequestIdIsPreserved(
            MockHttpServletResponse response, 
            AtomicReference<String> requestIdInChain, 
            String expectedId
    ) {
        assertEquals(expectedId, response.getHeader(RequestIdContext.HEADER_NAME));
        assertEquals(expectedId, requestIdInChain.get());
        assertNull(MDC.get(RequestIdContext.MDC_KEY));
    }

    private void thenInvalidRequestIdIsDiscarded(
            MockHttpServletResponse response, 
            AtomicReference<String> requestIdInChain, 
            String invalidId
    ) {
        String generatedRequestId = response.getHeader(RequestIdContext.HEADER_NAME);

        assertNotEquals(invalidId, generatedRequestId);
        assertEquals(generatedRequestId, requestIdInChain.get());
        assertDoesNotThrow(() -> UUID.fromString(generatedRequestId));
    }

    private void thenRequestIsLoggedCorrectly(ListAppender<ILoggingEvent> appender) {
        ILoggingEvent event = appender.list.get(0);
        List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();

        assertEquals("http_request", event.getMessage());
        assertEquals("POST", valueFor(keyValuePairs, "method"));
        assertEquals("/api/purchases", valueFor(keyValuePairs, "path"));
        assertEquals(200, valueFor(keyValuePairs, "status"));
        assertNotNull(valueFor(keyValuePairs, "durationMs"));
    }

    private FilterChain requestIdCapturingChain(AtomicReference<String> requestIdInChain) {
        return (request, response) -> requestIdInChain.set(MDC.get(RequestIdContext.MDC_KEY));
    }

    private Object valueFor(List<KeyValuePair> keyValuePairs, String key) {
        return keyValuePairs.stream()
                .filter(pair -> key.equals(pair.key))
                .map(pair -> pair.value)
                .findFirst()
                .orElse(null);
    }
}
