package com.recred.backend.unit.infrastructure.web;

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
    void givenAllowedOriginsWhenCheckingCorsThenAllowsFrontendAndLocalhost() {
        CorsConfiguration corsConfiguration = corsConfiguration();

        thenOriginIsAllowed(corsConfiguration, "https://recred.netlify.app");
        thenOriginIsAllowed(corsConfiguration, "http://localhost:5173");
        thenOriginIsAllowed(corsConfiguration, "http://127.0.0.1:3000");
    }

    @Test
    void givenUnknownOriginWhenCheckingCorsThenRejectsOrigin() {
        CorsConfiguration corsConfiguration = corsConfiguration();
        
        thenOriginIsRejected(corsConfiguration, "https://example.com");
    }

    @Test
    void givenCorsConfigurationWhenCheckingMethodsAndHeadersThenAllowsApiDefaults() {
        CorsConfiguration corsConfiguration = corsConfiguration();

        thenCorsAllowsApiMethodsAndHeaders(corsConfiguration);
    }

    @Test
    void givenIssuerAndClientIdWhenConfiguringJwtDecoderThenSetsValidators() {
        String issuerUri = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test";
        NimbusJwtDecoder decoder = Mockito.mock(NimbusJwtDecoder.class);
        givenSecurityConfigProperties(issuerUri, "client-id");

        try (MockedStatic<JwtDecoders> jwtDecoders = Mockito.mockStatic(JwtDecoders.class)) {
            givenJwtDecoderMock(jwtDecoders, issuerUri, decoder);
            
            NimbusJwtDecoder actualDecoder = (NimbusJwtDecoder) config.jwtDecoder();
            
            thenDecoderMatchesExpected(decoder, actualDecoder);
            thenValidatorsAreConfigured(decoder);
        }
    }

    private void givenSecurityConfigProperties(String issuerUri, String clientId) {
        ReflectionTestUtils.setField(config, "issuerUri", issuerUri);
        ReflectionTestUtils.setField(config, "clientId", clientId);
    }

    private void givenJwtDecoderMock(MockedStatic<JwtDecoders> jwtDecoders, String issuerUri, NimbusJwtDecoder decoder) {
        jwtDecoders.when(() -> JwtDecoders.fromIssuerLocation(issuerUri)).thenReturn(decoder);
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

    private void thenOriginIsAllowed(CorsConfiguration config, String origin) {
        assertEquals(origin, config.checkOrigin(origin));
    }

    private void thenOriginIsRejected(CorsConfiguration config, String origin) {
        assertNull(config.checkOrigin(origin));
    }

    private void thenCorsAllowsApiMethodsAndHeaders(CorsConfiguration config) {
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
        assertTrue(config.getAllowedMethods().contains("PUT"));
        assertTrue(config.getAllowedMethods().contains("PATCH"));
        assertTrue(config.getAllowedMethods().contains("DELETE"));
        assertTrue(config.getAllowedMethods().contains("OPTIONS"));
        assertEquals(List.of(CorsConfiguration.ALL), config.getAllowedHeaders());
        assertFalse(config.getAllowCredentials());
        assertEquals(3600L, config.getMaxAge());
    }

    private void thenDecoderMatchesExpected(NimbusJwtDecoder expected, NimbusJwtDecoder actual) {
        assertSame(expected, actual);
    }

    private void thenValidatorsAreConfigured(NimbusJwtDecoder decoder) {
        verify(decoder).setJwtValidator(any(OAuth2TokenValidator.class));
    }
}

