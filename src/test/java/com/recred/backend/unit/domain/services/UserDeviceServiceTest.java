package com.recred.backend.unit.domain.services;

import com.recred.backend.domain.services.UserDeviceService;
import com.recred.backend.infrastructure.entities.DispositivoUsuarioEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.SpringDataUsuarioRepository;
import com.recred.backend.infrastructure.persistence.UserDeviceTokenJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDeviceServiceTest {

    @Mock
    private UserDeviceTokenJpaRepository deviceTokenRepository;

    @Mock
    private SpringDataUsuarioRepository usuarioRepository;

    @InjectMocks
    private UserDeviceService userDeviceService;

    @Captor
    private ArgumentCaptor<DispositivoUsuarioEntity> deviceCaptor;

    private UsuarioEntity user;
    private final String cognitoId = "cognito-123";
    private final String fcmToken = "token-fcm-123";
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new UsuarioEntity();
        user.setId(userId);
        user.setIdCognito(cognitoId);
    }

    @Test
    void dadoQueElUsuarioNoExisteLanzaExcepcion() {
        when(usuarioRepository.findByIdCognito(cognitoId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userDeviceService.registerDeviceToken(cognitoId, fcmToken);
        });

        assertEquals("Usuario no encontrado con cognitoId: " + cognitoId, exception.getMessage());
        verify(deviceTokenRepository, never()).save(any());
    }

    @Test
    void dadoQueElTokenExisteActualizaElRegistro() {
        UUID deviceId = UUID.randomUUID();
        DispositivoUsuarioEntity existingDevice = new DispositivoUsuarioEntity();
        existingDevice.setId(deviceId);
        existingDevice.setFcmToken(fcmToken);
        existingDevice.setActivo(false);

        when(usuarioRepository.findByIdCognito(cognitoId)).thenReturn(Optional.of(user));
        when(deviceTokenRepository.findByFcmToken(fcmToken)).thenReturn(Optional.of(existingDevice));

        userDeviceService.registerDeviceToken(cognitoId, fcmToken);

        verify(deviceTokenRepository).save(deviceCaptor.capture());
        DispositivoUsuarioEntity savedDevice = deviceCaptor.getValue();
        
        assertEquals(user, savedDevice.getUsuarioEntity());
        assertTrue(savedDevice.getActivo());
        assertNotNull(savedDevice.getUltimoAccesoEn());
        assertEquals(deviceId, savedDevice.getId()); 
    }

    @Test
    void dadoQueElTokenNoExisteCreaUnNuevoRegistro() {
        when(usuarioRepository.findByIdCognito(cognitoId)).thenReturn(Optional.of(user));
        when(deviceTokenRepository.findByFcmToken(fcmToken)).thenReturn(Optional.empty());

        userDeviceService.registerDeviceToken(cognitoId, fcmToken);

        verify(deviceTokenRepository).save(deviceCaptor.capture());
        DispositivoUsuarioEntity savedDevice = deviceCaptor.getValue();

        assertEquals(user, savedDevice.getUsuarioEntity());
        assertEquals(fcmToken, savedDevice.getFcmToken());
        assertTrue(savedDevice.getActivo());
        assertNotNull(savedDevice.getUltimoAccesoEn());
        assertNull(savedDevice.getId()); 
    }
}
