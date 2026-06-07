package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.services.UserDeviceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDeviceControllerTest {

    @Mock
    private UserDeviceService userDeviceService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private UserDeviceController userDeviceController;

    @Test
    void dadoQueSeRecibeUnPeticionValidaRegistraElTokenFcmYRetornaCreated() {
        String cognitoId = "user-cognito-id-123";
        String fcmToken = "test-fcm-token";
        
        DeviceTokenRequestDTO request = new DeviceTokenRequestDTO(fcmToken);
        
        when(jwt.getSubject()).thenReturn(cognitoId);

        ResponseEntity<Void> response = userDeviceController.registerDeviceToken(jwt, request);

        verify(userDeviceService).registerDeviceToken(cognitoId, fcmToken);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }
}
