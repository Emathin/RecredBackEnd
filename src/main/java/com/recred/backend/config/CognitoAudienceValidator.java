package com.recred.backend.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

class CognitoAudienceValidator implements OAuth2TokenValidator<Jwt> {
    private final String audience;

    public CognitoAudienceValidator(String audience) {
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audiences = jwt.getAudience();

        if (audiences != null && audiences.contains(audience)) {
            return OAuth2TokenValidatorResult.success();
        }

        String clientId = jwt.getClaimAsString("client_id");

        if (audience.equals(clientId)) {
            return OAuth2TokenValidatorResult.success();
        }

        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token",
                "El Client ID no coincide con aud ni con client_id",
                null
        ));
    }
}