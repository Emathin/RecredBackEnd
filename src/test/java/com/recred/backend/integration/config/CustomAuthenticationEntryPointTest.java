package com.recred.backend.integration.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.config.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomAuthenticationEntryPointTest {

    private final CustomAuthenticationEntryPoint entryPoint =
            new CustomAuthenticationEntryPoint();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void commence_ShouldWriteUnauthorizedJsonResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/private");
        request.setServletPath("/api/private");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new BadCredentialsException("bad token")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.get("status").asInt());
        assertEquals("UNAUTHORIZED", body.get("code").asText());
        assertEquals("No autorizado", body.get("error").asText());
        assertEquals("Token inv\u00e1lido, expirado o ausente", body.get("mensaje").asText());
        assertEquals("/api/private", body.get("path").asText());
    }
}
