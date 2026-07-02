package com.recred.backend.unit.application;

import com.recred.backend.application.SetNotificationAsAlreadyRead;
import com.recred.backend.application.SetNotificationAsAlreadyReadRequest;

import com.recred.backend.domain.exceptions.NotificationNotFoundException;
import com.recred.backend.domain.repositories.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SetNotificationAsAlreadyReadTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private SetNotificationAsAlreadyRead setNotificationAsAlreadyRead;

    @Test
    void execute_ShouldCallRepository() {
        UUID id = UUID.randomUUID();
        setNotificationAsAlreadyRead.execute(new SetNotificationAsAlreadyReadRequest(id));
        verify(notificationRepository).markAsRead(id);
    }

    @Test
    void execute_WhenNotificationNotFound_ShouldThrowDomainException() {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("Notification not found"))
                .when(notificationRepository).markAsRead(id);

        NotificationNotFoundException ex = assertThrows(
                NotificationNotFoundException.class,
                () -> setNotificationAsAlreadyRead.execute(
                        new SetNotificationAsAlreadyReadRequest(id))
        );
        assertEquals("La notificacion no existe: " + id, ex.getMessage());
    }

    @Test
    void execute_WhenOtherException_ShouldRethrow() {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("Database error"))
                .when(notificationRepository).markAsRead(id);

        assertThrows(RuntimeException.class,
                () -> setNotificationAsAlreadyRead.execute(
                        new SetNotificationAsAlreadyReadRequest(id)));
    }
}
