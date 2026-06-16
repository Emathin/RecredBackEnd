package com.recred.backend.unit.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.NotifyPurchaseSuggestions;
import com.recred.backend.application.PurchaseSuggestIA;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotifyPurchaseSuggestions useCase;

    @Test
    @DisplayName("execute_CuandoHaySugerencia_EnviaNotificacionConMetadatos")
    void execute_CuandoHaySugerencia_EnviaNotificacionConMetadatos() {
        String cognitoId = "cognito-id-test";
        UUID userId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        SugerenciaIA sugerencia = SugerenciaIA.builder()
                .id(suggestionId)
                .alumnoId(alumnoId)
                .productoId(productId)
                .mensaje("Mensaje test")
                .build();

        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setRol(RolUsuario.ALUMNO);

        when(purchaseSuggestIA.execute(cognitoId)).thenReturn(Optional.of(sugerencia));
        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(usuario);


        useCase.execute(cognitoId);


        ArgumentCaptor<PushNotification> captor = ArgumentCaptor.forClass(PushNotification.class);
        verify(pushNotificationSender, times(1)).send(captor.capture());

        PushNotification sentNotification = captor.getValue();
        assertEquals(userId, sentNotification.userId());
        assertNull(sentNotification.title());
        assertNull(sentNotification.message());
        assertEquals("PURCHASE_SUGGESTION", sentNotification.data().get("type"));
        assertEquals(suggestionId.toString(), sentNotification.data().get("sugerenciaId"));
        assertEquals(alumnoId.toString(), sentNotification.data().get("alumnoId"));
        assertNotNull(sentNotification.data().get("titulo"));
        assertNotNull(sentNotification.data().get("mensaje"));
        assertNotNull(sentNotification.data().get("producto"));
    }

    @Test
    @DisplayName("execute_CuandoNoHaySugerencia_NoEnviaNada")
    void execute_CuandoNoHaySugerencia_NoEnviaNada() {

        String cognitoId = "cognito-id-test";
        when(purchaseSuggestIA.execute(cognitoId)).thenReturn(Optional.empty());

        useCase.execute(cognitoId);

        verifyNoInteractions(pushNotificationSender);
    }
}
