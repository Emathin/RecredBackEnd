package com.recred.backend.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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
    void generaRequestIdCuandoNoLlegaHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();

        filter.doFilter(request, response, requestIdCapturingChain(requestIdInChain));

        String generatedRequestId = response.getHeader(RequestIdContext.HEADER_NAME);

        assertNotNull(generatedRequestId);
        assertEquals(generatedRequestId, requestIdInChain.get());
        assertDoesNotThrow(() -> UUID.fromString(generatedRequestId));
        assertNull(MDC.get(RequestIdContext.MDC_KEY));
    }

    @Test
    void conservaRequestIdEntranteValido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();

        request.addHeader(RequestIdContext.HEADER_NAME, "front-123");

        filter.doFilter(request, response, requestIdCapturingChain(requestIdInChain));

        assertEquals("front-123", response.getHeader(RequestIdContext.HEADER_NAME));
        assertEquals("front-123", requestIdInChain.get());
        assertNull(MDC.get(RequestIdContext.MDC_KEY));
    }

    @Test
    void descartaRequestIdEntranteInvalido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();

        request.addHeader(RequestIdContext.HEADER_NAME, "front 123");

        filter.doFilter(request, response, requestIdCapturingChain(requestIdInChain));

        String generatedRequestId = response.getHeader(RequestIdContext.HEADER_NAME);

        assertNotEquals("front 123", generatedRequestId);
        assertEquals(generatedRequestId, requestIdInChain.get());
        assertDoesNotThrow(() -> UUID.fromString(generatedRequestId));
    }

    @Test
    void logueaRequestConCamposEstructurados() throws Exception {
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RequestIdFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/purchases");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, requestIdCapturingChain(new AtomicReference<>()));

            ILoggingEvent event = appender.list.get(0);
            List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();

            assertEquals("http_request", event.getMessage());
            assertEquals("POST", valueFor(keyValuePairs, "method"));
            assertEquals("/api/purchases", valueFor(keyValuePairs, "path"));
            assertEquals(200, valueFor(keyValuePairs, "status"));
            assertNotNull(valueFor(keyValuePairs, "durationMs"));
        } finally {
            logger.detachAppender(appender);
        }
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
