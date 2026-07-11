package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.services.UserDeviceService;
import com.recred.backend.infrastructure.web.DeviceTokenRequestDTO;
import com.recred.backend.infrastructure.web.UserDeviceController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDeviceControllerTest {

    private static final String COGNITO_ID = "user-cognito-id-123";
    private static final String FCM_TOKEN = "test-fcm-token";

    private UserDeviceService userDeviceService;
    private UserDeviceController controller;
    private Jwt jwt;
    private ResponseEntity<Void> response;

    @BeforeEach
    void setUp() {
        userDeviceService = mock(UserDeviceService.class);
        controller = new UserDeviceController(userDeviceService);
        jwt = mock(Jwt.class);
    }

    @Test
    void givenValidRequestWhenRegisteringDeviceTokenThenReturnsCreated() {
        givenAuthenticatedUser();

        whenRegisteringDeviceToken();

        thenDeviceTokenWasRegistered();
        thenStatusIsCreated();
    }

    private void givenAuthenticatedUser() {
        when(jwt.getSubject()).thenReturn(COGNITO_ID);
    }

    private void whenRegisteringDeviceToken() {
        DeviceTokenRequestDTO request = new DeviceTokenRequestDTO(FCM_TOKEN);
        response = controller.registerDeviceToken(jwt, request);
    }

    private void thenDeviceTokenWasRegistered() {
        verify(userDeviceService).registerDeviceToken(COGNITO_ID, FCM_TOKEN);
    }

    private void thenStatusIsCreated() {
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }
}
