package com.recred.backend.unit.infrastructure.config;

import com.recred.backend.config.CognitoAudienceValidator;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CognitoAudienceValidatorTest {

    @Test
    void validate_WhenAudienceContainsExpectedValue_ShouldSucceed() {
        CognitoAudienceValidator validator = new CognitoAudienceValidator("client-id");

        OAuth2TokenValidatorResult result = validator.validate(jwt(
                Map.of("aud", List.of("client-id"))
        ));

        assertFalse(result.hasErrors());
    }

    @Test
    void validate_WhenClientIdMatchesExpectedValue_ShouldSucceed() {
        CognitoAudienceValidator validator = new CognitoAudienceValidator("client-id");

        OAuth2TokenValidatorResult result = validator.validate(jwt(
                Map.of("client_id", "client-id")
        ));

        assertFalse(result.hasErrors());
    }

    @Test
    void validate_WhenAudienceAndClientIdDoNotMatch_ShouldFail() {
        CognitoAudienceValidator validator = new CognitoAudienceValidator("client-id");

        OAuth2TokenValidatorResult result = validator.validate(jwt(
                Map.of("aud", List.of("other"), "client_id", "other")
        ));

        assertTrue(result.hasErrors());
    }

    private Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-06-15T00:00:00Z"))
                .expiresAt(Instant.parse("2026-06-15T01:00:00Z"))
                .claims(existingClaims -> existingClaims.putAll(claims))
                .build();
    }
}

