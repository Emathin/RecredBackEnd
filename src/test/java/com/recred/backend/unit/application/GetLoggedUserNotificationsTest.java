package com.recred.backend.unit.application;

import com.recred.backend.application.GetLoggedUserNotifications;
import com.recred.backend.application.GetLoggedUserNotificationsRequest;
import com.recred.backend.application.GetLoggedUserNotificationsResponse;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
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
    private UUID userId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        useCase = new GetLoggedUserNotifications(usuarioRepository, notificationRepository);
    }

    @Test
    void givenAuthenticatedUserHasNotifications_whenGettingNotifications_thenReturnsNotifications() {
        givenAuthenticatedUserExists();
        givenUserHasNotifications();

        final GetLoggedUserNotificationsResponse response = whenGetLoggedUserNotifications();

        thenNotificationsWereReturned(response);
        thenNotificationsWereRequested();
    }

    @Test
    void givenAuthenticatedUserDoesNotExist_whenGettingNotifications_thenThrowsException() {
        givenAuthenticatedUserDoesNotExist();

        final RuntimeException exception = whenGetLoggedUserNotificationsFails(defaultRequest());

        thenAuthenticatedUserNotFoundWasThrown(exception);
        thenNotificationsWereNotRequested();
    }

    @Test
    void givenNegativePage_whenGettingNotifications_thenThrowsExceptionBeforeFindingUser() {
        final RuntimeException exception = whenGetLoggedUserNotificationsFails(
                request(false, -1, 20));

        thenErrorMessageIs("Page cannot be negative", exception);
        thenAuthenticatedUserWasNotRequested();
    }

    @Test
    void givenInvalidSize_whenGettingNotifications_thenThrowsExceptionBeforeFindingUser() {
        final RuntimeException exception = whenGetLoggedUserNotificationsFails(
                request(false, 0, 101));

        thenErrorMessageIs("Size must be between 1 and 100", exception);
        thenAuthenticatedUserWasNotRequested();
    }

    private void givenAuthenticatedUserExists() {
        userId = randomUUID();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT))
                .thenReturn(Optional.of(user(userId)));
    }

    private void givenAuthenticatedUserDoesNotExist() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
    }

    private void givenUserHasNotifications() {
        notificationId = randomUUID();
        when(notificationRepository.findByUserId(userId, true, 1, 5))
                .thenReturn(List.of(notification(notificationId)));
    }

    private GetLoggedUserNotificationsResponse whenGetLoggedUserNotifications() {
        return useCase.execute(request(true, 1, 5));
    }

    private RuntimeException whenGetLoggedUserNotificationsFails(
            GetLoggedUserNotificationsRequest notificationRequest
    ) {
        return assertThrows(RuntimeException.class, executeUseCase(notificationRequest));
    }

    private void thenNotificationsWereReturned(GetLoggedUserNotificationsResponse response) {
        assertEquals(1, response.notifications().size());
        assertEquals(notificationId, response.notifications().get(0).id());
        assertEquals("Title", response.notifications().get(0).title());
        assertEquals("Message", response.notifications().get(0).message());
    }

    private void thenAuthenticatedUserNotFoundWasThrown(RuntimeException exception) {
        assertEquals(
                UsuarioAutenticadoNoEncontradoException.class,
                exception.getClass()
        );
    }

    private void thenErrorMessageIs(String message, RuntimeException exception) {
        assertEquals(message, exception.getMessage());
    }

    private void thenNotificationsWereRequested() {
        verify(notificationRepository).findByUserId(userId, true, 1, 5);
    }

    private void thenNotificationsWereNotRequested() {
        verify(notificationRepository, never()).findByUserId(
                any(),
                anyBoolean(),
                anyInt(),
                anyInt()
        );
    }

    private void thenAuthenticatedUserWasNotRequested() {
        verify(usuarioRepository, never()).findByIdCognito(COGNITO_SUBJECT);
    }

    private Executable executeUseCase(GetLoggedUserNotificationsRequest notificationRequest) {
        return () -> useCase.execute(notificationRequest);
    }

    private GetLoggedUserNotificationsRequest defaultRequest() {
        return request(false, 0, 20);
    }

    private GetLoggedUserNotificationsRequest request(boolean unreadOnly, int page, int size) {
        return new GetLoggedUserNotificationsRequest(
                COGNITO_SUBJECT,
                unreadOnly,
                page,
                size
        );
    }

    private Usuario user(UUID requestedUserId) {
        Usuario user = new Usuario();
        user.setId(requestedUserId);
        return user;
    }

    private Notification notification(UUID requestedNotificationId) {
        return new Notification(
                requestedNotificationId,
                TipoNotificacion.SISTEMA,
                "Title",
                "Message",
                false,
                LocalDateTime.of(2026, 6, 2, 14, 30),
                null,
                null,
                null,
                null,
                null
        );
    }
}
