package com.recred.backend.application;

import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import com.recred.backend.domain.services.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotifyPurchaseSuggestionsTest {

    @Mock
    private PurchaseSuggestIA purchaseSuggestIA;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private PushNotificationSender pushNotificationSender;
    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private NotifyPurchaseSuggestions useCase;

    @Test
    @DisplayName("execute_CuandoHaySugerencia_EnviaNotificacionConMetadatos")
    void execute_CuandoHaySugerencia_EnviaNotificacionConMetadatos() {
        // Arrange
        String cognitoId = "cognito-id-test";
        UUID userId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        SugerenciaIA sugerencia = SugerenciaIA.builder()
                .id(suggestionId)
                .productoId(productId)
                .mensaje("Mensaje test")
                .build();

        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setRol(RolUsuario.ALUMNO);

        when(purchaseSuggestIA.execute(cognitoId)).thenReturn(Optional.of(sugerencia));
        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(usuario);

        // Act
        useCase.execute(cognitoId);

        // Assert
        ArgumentCaptor<PushNotification> captor = ArgumentCaptor.forClass(PushNotification.class);
        verify(pushNotificationSender, times(1)).send(captor.capture());

        PushNotification sentNotification = captor.getValue();
        assertEquals(userId, sentNotification.userId());
        assertEquals("PURCHASE_SUGGESTION", sentNotification.data().get("type"));
        assertEquals(suggestionId.toString(), sentNotification.data().get("suggestionId"));
        assertEquals(productId.toString(), sentNotification.data().get("productId"));
        assertNotNull(sentNotification.message());
    }

    @Test
    @DisplayName("execute_CuandoNoHaySugerencia_NoEnviaNada")
    void execute_CuandoNoHaySugerencia_NoEnviaNada() {
        // Arrange
        String cognitoId = "cognito-id-test";
        when(purchaseSuggestIA.execute(cognitoId)).thenReturn(Optional.empty());

        // Act
        useCase.execute(cognitoId);

        // Assert
        verifyNoInteractions(pushNotificationSender);
    }
}
