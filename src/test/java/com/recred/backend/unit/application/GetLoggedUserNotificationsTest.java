package com.recred.backend.unit.application;

import com.recred.backend.application.GetLoggedUserNotifications;
import com.recred.backend.application.GetLoggedUserNotificationsRequest;
import com.recred.backend.application.GetLoggedUserNotificationsResponse;
import com.recred.backend.application.UserNotificationResponse;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLoggedUserNotificationsTest {

    private static final String COGNITO_SUBJECT = "cognito-sub";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NotificationRepository notificationRepository;

    private GetLoggedUserNotifications useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetLoggedUserNotifications(usuarioRepository, notificationRepository);
    }

    @Test
    void executeListsAuthenticatedUserNotifications() {
        UUID userId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        List<Notification> notifications = List.of(notification());
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(notificationRepository.findByUserId(userId, true, 1, 5))
                .thenReturn(notifications);

        GetLoggedUserNotificationsResponse result = useCase.execute(request(true, 1, 5));

        assertEquals(notifications.get(0).id(), result.notifications().get(0).id());
        verify(notificationRepository).findByUserId(userId, true, 1, 5);
    }

    @Test
    void executeRejectsUnknownAuthenticatedUser() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());

        assertThrows(
                UsuarioAutenticadoNoEncontradoException.class,
                () -> useCase.execute(request(false, 0, 20))
        );

        verify(notificationRepository, never()).findByUserId(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void executeRejectsInvalidPagination() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(request(false, -1, 20))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(request(false, 0, 101))
        );

        verify(usuarioRepository, never()).findByIdCognito(COGNITO_SUBJECT);
    }

    private Notification notification() {
        return new Notification(
                UUID.randomUUID(),
                TipoNotificacion.SISTEMA,
                "Title",
                "Message",
                false,
                LocalDateTime.of(2026, 6, 2, 14, 30),
                null
        );
    }

    private GetLoggedUserNotificationsRequest request(
            boolean unreadOnly,
            int page,
            int size
    ) {
        return new GetLoggedUserNotificationsRequest(
                COGNITO_SUBJECT,
                unreadOnly,
                page,
                size
        );
    }
}
