package com.recred.backend.unit.infrastructure.cognito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.config.CognitoAdminProperties;
import com.recred.backend.domain.services.CreatedCognitoUser;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.recred.backend.infrastructure.cognito.CognitoStudentIdentityServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CognitoStudentIdentityServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-15T12:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void createStudentUserCreatesUserAndAddsItToConfiguredGroup() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> createResponse = response(
                200,
                """
                        {
                          "User": {
                            "Username": "created-student",
                            "Attributes": [
                              { "Name": "sub", "Value": "cognito-sub" }
                            ]
                          }
                        }
                        """
        );
        HttpResponse<String> groupResponse = response(200, "");
        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(createResponse, groupResponse);
        CognitoStudentIdentityServiceImpl service =
                service(properties("alumnos", ""), httpClient);

        CreatedCognitoUser result = service.createStudentUser(
                " student ",
                "student@test.com",
                "Ana",
                "Lopez"
        );

        assertEquals("cognito-sub", result.sub());
        assertEquals("created-student", result.username());
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(
                requestCaptor.capture(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        );
        assertEquals(
                "AWSCognitoIdentityProviderService.AdminCreateUser",
                requestCaptor.getAllValues().get(0).headers()
                        .firstValue("X-Amz-Target").orElseThrow()
        );
        assertEquals(
                "AWSCognitoIdentityProviderService.AdminAddUserToGroup",
                requestCaptor.getAllValues().get(1).headers()
                        .firstValue("X-Amz-Target").orElseThrow()
        );
        assertFalse(requestCaptor.getAllValues().get(0).headers()
                .firstValue("X-Amz-Security-Token").isPresent());
        assertTrue(requestCaptor.getAllValues().get(0).headers()
                .firstValue("Authorization").orElseThrow()
                .startsWith("AWS4-HMAC-SHA256 Credential=access/20260615/us-east-1/"));
    }

    @Test
    void createStudentUserSkipsGroupWhenGroupIsBlankAndSendsSessionToken() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> createResponse = response(
                200,
                """
                        {
                          "User": {
                            "Username": "generated-student",
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
        CognitoAdminProperties properties = properties("   ", "session-token");
        CognitoStudentIdentityServiceImpl service = service(properties, httpClient);

        CreatedCognitoUser result = service.createStudentUser(
                null,
                "student@test.com",
                "Ana",
                "Lopez"
        );

        assertEquals("generated-sub", result.sub());
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(
                requestCaptor.capture(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        );
        assertEquals("session-token", requestCaptor.getValue().headers()
                .firstValue("X-Amz-Security-Token").orElseThrow());
        assertTrue(requestCaptor.getValue().headers()
                .firstValue("Authorization").orElseThrow()
                .contains("x-amz-security-token"));
    }

    @Test
    void createStudentUserThrowsWhenCognitoReturnsErrorStatus() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> errorResponse = response(400, "bad request");
        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(errorResponse);
        CognitoStudentIdentityServiceImpl service =
                service(properties(null, null), httpClient);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.createStudentUser("student", "student@test.com", "Ana", "Lopez")
        );

        assertEquals("No se pudo invocar Cognito", exception.getMessage());
        assertTrue(exception.getCause().getMessage().contains("Cognito respondio 400"));
    }

    @Test
    void createStudentUserWhenHttpClientIsInterruptedShouldRestoreInterruptFlag()
            throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenThrow(new InterruptedException("stop"));
        CognitoStudentIdentityServiceImpl service =
                service(properties(null, null), httpClient);

        try {
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> service.createStudentUser(
                            "student",
                            "student@test.com",
                            "Ana",
                            "Lopez"
                    )
            );

            assertEquals("Se interrumpio la llamada a Cognito", exception.getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void createStudentUserThrowsWhenRequiredPropertiesAreMissing() {
        CognitoStudentIdentityServiceImpl service = new CognitoStudentIdentityServiceImpl(
                new CognitoAdminProperties(),
                new ObjectMapper()
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.createStudentUser("student", "student@test.com", "Ana", "Lopez")
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

    private CognitoAdminProperties properties(String group, String sessionToken) {
        CognitoAdminProperties properties = new CognitoAdminProperties();
        properties.setUserPoolId("pool");
        properties.setRegion("us-east-1");
        properties.setStudentGroup(group);
        properties.setAwsAccessKeyId("access");
        properties.setAwsSecretAccessKey("secret");
        properties.setAwsSessionToken(sessionToken);
        return properties;
    }

    private CognitoStudentIdentityServiceImpl service(
            CognitoAdminProperties properties,
            HttpClient httpClient) throws Exception {
        CognitoStudentIdentityServiceImpl service = new CognitoStudentIdentityServiceImpl(
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

