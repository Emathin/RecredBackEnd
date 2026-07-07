package com.recred.backend.unit.application;

import com.recred.backend.application.SetNotificationAsAlreadyRead;
import com.recred.backend.application.SetNotificationAsAlreadyReadRequest;
import com.recred.backend.application.SetNotificationAsAlreadyReadResponse;
import com.recred.backend.domain.exceptions.NotificationNotFoundException;
import com.recred.backend.domain.repositories.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SetNotificationAsAlreadyReadTest {

    @Mock
    private NotificationRepository notificationRepository;

    private SetNotificationAsAlreadyRead useCase;

    private UUID notificationId;
    private SetNotificationAsAlreadyReadResponse response;
    private RuntimeException thrownException;
    private RuntimeException repositoryException;

    @BeforeEach
    void setUp() {
        useCase = new SetNotificationAsAlreadyRead(notificationRepository);
    }

    @Test
    void givenNotificationExists_whenSettingAsAlreadyRead_thenMarksNotificationAsRead() {
        givenNotificationScenario();

        whenSettingNotificationAsAlreadyRead();

        thenNotificationWasMarkedAsRead();
        thenNotificationIdIsReturned();
    }

    @Test
    void givenNotificationDoesNotExist_whenSettingAsAlreadyRead_thenThrowsDomainException() {
        givenNotificationScenario();
        givenRepositoryReportsNotificationNotFound();

        whenSettingNotificationAsAlreadyReadFails();

        thenNotificationNotFoundWasThrown();
    }

    @Test
    void givenRepositoryFails_whenSettingAsAlreadyRead_thenRethrowsRepositoryException() {
        givenNotificationScenario();
        givenRepositoryFails();

        whenSettingNotificationAsAlreadyReadFails();

        thenRepositoryExceptionWasRethrown();
    }

    private void givenNotificationScenario() {
        notificationId = UUID.randomUUID();
    }

    private void givenRepositoryReportsNotificationNotFound() {
        repositoryException = new RuntimeException("Notification not found");
        doThrow(repositoryException).when(notificationRepository).markAsRead(notificationId);
    }

    private void givenRepositoryFails() {
        repositoryException = new RuntimeException("Database error");
        doThrow(repositoryException).when(notificationRepository).markAsRead(notificationId);
    }

    private void whenSettingNotificationAsAlreadyRead() {
        response = useCase.execute(request());
    }

    private void whenSettingNotificationAsAlreadyReadFails() {
        thrownException = assertThrows(
                RuntimeException.class,
                this::whenSettingNotificationAsAlreadyRead);
    }

    private void thenNotificationWasMarkedAsRead() {
        verify(notificationRepository).markAsRead(notificationId);
    }

    private void thenNotificationIdIsReturned() {
        assertEquals(notificationId, response.notificationId());
    }

    private void thenNotificationNotFoundWasThrown() {
        assertTrue(thrownException instanceof NotificationNotFoundException);
        assertEquals("La notificacion no existe: " + notificationId, thrownException.getMessage());
    }

    private void thenRepositoryExceptionWasRethrown() {
        assertSame(repositoryException, thrownException);
    }

    private SetNotificationAsAlreadyReadRequest request() {
        return new SetNotificationAsAlreadyReadRequest(notificationId);
    }
}
