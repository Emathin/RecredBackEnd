package com.recred.backend.web;

import java.util.List;

import com.recred.backend.config.CorsProperties;
import com.recred.backend.config.CustomAuthenticationEntryPoint;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

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

    @Test
    void jwtDecoder_ShouldConfigureIssuerAndAudienceValidators() {
        String issuerUri = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test";
        NimbusJwtDecoder decoder = Mockito.mock(NimbusJwtDecoder.class);
        ReflectionTestUtils.setField(config, "issuerUri", issuerUri);
        ReflectionTestUtils.setField(config, "clientId", "client-id");

        try (MockedStatic<JwtDecoders> jwtDecoders = Mockito.mockStatic(JwtDecoders.class)) {
            jwtDecoders.when(() -> JwtDecoders.fromIssuerLocation(issuerUri)).thenReturn(decoder);

            assertSame(decoder, config.jwtDecoder());
        }

        verify(decoder).setJwtValidator(any(OAuth2TokenValidator.class));
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
