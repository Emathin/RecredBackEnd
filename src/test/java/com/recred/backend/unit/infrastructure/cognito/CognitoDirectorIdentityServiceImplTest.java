package com.recred.backend.unit.infrastructure.cognito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.config.CognitoAdminProperties;
import com.recred.backend.domain.services.CreatedCognitoUser;
import com.recred.backend.infrastructure.cognito.CognitoDirectorIdentityServiceImpl;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CognitoDirectorIdentityServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-15T12:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void createDirectorUserShouldCreateCognitoUserWithTrimmedUsername() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> createResponse = response(
                200,
                """
                        {
                          "User": {
                            "Username": "director-user",
                            "Attributes": [
                              { "Name": "sub", "Value": "director-sub" }
                            ]
                          }
                        }
                        """
        );
        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(createResponse);
        CognitoDirectorIdentityServiceImpl service = service(properties(), httpClient);

        CreatedCognitoUser result = service.createDirectorUser(
                " director ",
                "director@test.com",
                "Ana",
                "Lopez"
        );

        assertEquals("director-sub", result.sub());
        assertEquals("director-user", result.username());
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(
                requestCaptor.capture(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        );
        assertEquals(
                "AWSCognitoIdentityProviderService.AdminCreateUser",
                requestCaptor.getValue().headers()
                        .firstValue("X-Amz-Target").orElseThrow()
        );
        assertTrue(requestCaptor.getValue().headers()
                .firstValue("Authorization").orElseThrow()
                .startsWith("AWS4-HMAC-SHA256 Credential=access/20260615/us-east-1/"));
    }

    @Test
    void createDirectorUserShouldGenerateUsernameWhenBlank() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> createResponse = response(
                200,
                """
                        {
                          "User": {
                            "Username": "generated-director",
                            "Attributes": [
                              { "Name": "sub", "Value": "generated-sub" }
                            ]
                          }
                        }
                        """
        );
        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(createResponse);
        CognitoDirectorIdentityServiceImpl service = service(properties(), httpClient);

        CreatedCognitoUser result = service.createDirectorUser(
                " ",
                "director@test.com",
                "Ana",
                "Lopez"
        );

        assertEquals("generated-sub", result.sub());
        assertEquals("generated-director", result.username());
    }

    @Test
    void createDirectorUserShouldFailWhenRequiredPropertiesAreMissing() {
        CognitoDirectorIdentityServiceImpl service = new CognitoDirectorIdentityServiceImpl(
                new CognitoAdminProperties(),
                new ObjectMapper()
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.createDirectorUser("director", "mail@test.com", "Ana", "Lopez")
        );

        assertEquals("Falta configurar recred.cognito.userPoolId", exception.getMessage());
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int statusCode, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }

    private CognitoAdminProperties properties() {
        CognitoAdminProperties properties = new CognitoAdminProperties();
        properties.setUserPoolId("pool");
        properties.setRegion("us-east-1");
        properties.setAwsAccessKeyId("access");
        properties.setAwsSecretAccessKey("secret");
        return properties;
    }

    private CognitoDirectorIdentityServiceImpl service(
            CognitoAdminProperties properties,
            HttpClient httpClient
    ) throws Exception {
        CognitoDirectorIdentityServiceImpl service = new CognitoDirectorIdentityServiceImpl(
                properties,
                new ObjectMapper()
        );
        setField(service, "httpClient", httpClient);
        setField(service, "clock", CLOCK);
        return service;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getSuperclass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
