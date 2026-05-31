package com.recred.backend.config;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    private static final List<String> ALLOWED_ORIGIN_PATTERNS = List.of(
            "https://recred.netlify.app",
            "http://localhost:*",
            "http://127.0.0.1:*"
    );

    private final CustomAuthenticationEntryPoint entryPointMock = Mockito.mock(CustomAuthenticationEntryPoint.class);
    private final CustomJwtAuthenticationConverter converterMock = Mockito.mock(CustomJwtAuthenticationConverter.class);

    private final SecurityConfig config = new SecurityConfig(entryPointMock, converterMock);

    @Test
    void corsConfigurationAllowsFrontendAndLocalhostOrigins() {
        CorsConfiguration corsConfiguration = corsConfiguration();

        assertEquals(
                "https://recred.netlify.app",
                corsConfiguration.checkOrigin("https://recred.netlify.app")
        );
        assertEquals(
                "http://localhost:5173",
                corsConfiguration.checkOrigin("http://localhost:5173")
        );
        assertEquals(
                "http://127.0.0.1:3000",
                corsConfiguration.checkOrigin("http://127.0.0.1:3000")
        );
    }

    @Test
    void corsConfigurationRejectsUnknownOrigin() {
        assertNull(corsConfiguration().checkOrigin("https://example.com"));
    }

    @Test
    void corsConfigurationAllowsApiMethodsAndHeaders() {
        CorsConfiguration corsConfiguration = corsConfiguration();

        assertTrue(corsConfiguration.getAllowedMethods().contains("GET"));
        assertTrue(corsConfiguration.getAllowedMethods().contains("POST"));
        assertTrue(corsConfiguration.getAllowedMethods().contains("PUT"));
        assertTrue(corsConfiguration.getAllowedMethods().contains("PATCH"));
        assertTrue(corsConfiguration.getAllowedMethods().contains("DELETE"));
        assertTrue(corsConfiguration.getAllowedMethods().contains("OPTIONS"));
        assertEquals(List.of(CorsConfiguration.ALL), corsConfiguration.getAllowedHeaders());
        assertFalse(corsConfiguration.getAllowCredentials());
        assertEquals(3600L, corsConfiguration.getMaxAge());
    }

    private CorsConfiguration corsConfiguration() {
        CorsConfigurationSource source = config.corsConfigurationSource(
                new CorsProperties(ALLOWED_ORIGIN_PATTERNS)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api");
        request.addHeader("Origin", "https://recred.netlify.app");
        request.addHeader("Access-Control-Request-Method", "GET");

        return source.getCorsConfiguration(request);
    }
}