package com.recred.backend.infrastructure.cognito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.config.CognitoAdminProperties;
import com.recred.backend.domain.services.CognitoStudentIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class CognitoStudentIdentityServiceImpl extends BaseCognitoIdentityService 
        implements CognitoStudentIdentityService {

    public CognitoStudentIdentityServiceImpl(CognitoAdminProperties properties,
                                             ObjectMapper objectMapper) {
        super(properties, objectMapper);
    }

    @Override
    public CreatedCognitoUser createStudentUser(String username, String email,
                                                String nombre, String apellido) {
        String userPoolId = required(properties.getUserPoolId(), "userPoolId");
        String region = required(properties.getRegion(), "region");
        String cognitoUsername = username != null && !username.isBlank()
                ? username.trim()
                : "student-" + UUID.randomUUID();

        Map<String, Object> response = invoke(region, CREATE_USER_TARGET, Map.of(
                "UserPoolId", userPoolId,
                "Username", cognitoUsername,
                "DesiredDeliveryMediums", List.of("EMAIL"),
                "UserAttributes", List.of(
                        attribute("email", email),
                        attribute("given_name", nombre),
                        attribute("family_name", apellido)
                )
        ));

        Map<String, Object> user = castMap(response.get("User"));
        String createdUsername = stringValue(user.get("Username"));
        String sub = extractSub(user);

        addToStudentGroupIfConfigured(region, userPoolId, createdUsername);

        return new CreatedCognitoUser(sub, createdUsername);
    }

    private void addToStudentGroupIfConfigured(String region, String userPoolId, String username) {
        String group = properties.getStudentGroup();
        if (group == null || group.isBlank()) {
            return;
        }

        invoke(region, ADD_TO_GROUP_TARGET, Map.of(
                "UserPoolId", userPoolId,
                "Username", username,
                "GroupName", group.trim()
        ));
    }
}
