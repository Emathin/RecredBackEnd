package com.recred.backend.infrastructure.cognito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.config.CognitoAdminProperties;
import com.recred.backend.domain.services.CognitoTutorIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class CognitoTutorIdentityServiceImpl extends BaseCognitoIdentityService
        implements CognitoTutorIdentityService {

    public CognitoTutorIdentityServiceImpl(CognitoAdminProperties properties,
                                           ObjectMapper objectMapper) {
        super(properties, objectMapper);
    }

    @Override
    public CreatedCognitoUser createTutorUser(String username, String email,
                                              String firstName, String lastName) {
        String userPoolId = required(properties.getUserPoolId(), "userPoolId");
        String region = required(properties.getRegion(), "region");
        String cognitoUsername = username != null && !username.isBlank()
                ? username.trim()
                : "tutor-" + UUID.randomUUID();

        Map<String, Object> response = invoke(region, CREATE_USER_TARGET, Map.of(
                "UserPoolId", userPoolId,
                "Username", cognitoUsername,
                "DesiredDeliveryMediums", List.of("EMAIL"),
                "UserAttributes", List.of(
                        attribute("email", email),
                        attribute("given_name", firstName),
                        attribute("family_name", lastName)
                )
        ));

        Map<String, Object> user = castMap(response.get("User"));
        String createdUsername = stringValue(user.get("Username"));
        String sub = extractSub(user);

        return new CreatedCognitoUser(sub, createdUsername);
    }
}
