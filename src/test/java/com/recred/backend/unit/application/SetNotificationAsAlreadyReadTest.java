package com.recred.backend.unit.application;

import com.recred.backend.application.SetNotificationAsAlreadyRead;
import com.recred.backend.application.SetNotificationAsAlreadyReadRequest;

import com.recred.backend.domain.exceptions.NotificationNotFoundException;
import com.recred.backend.domain.repositories.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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
        SetNotificationAsAlreadyReadRequest request = new SetNotificationAsAlreadyReadRequest(id);

        setNotificationAsAlreadyRead.execute(request);

        verify(notificationRepository).markAsRead(id);
    }

    @Test
    void execute_WhenNotificationNotFound_ShouldThrowDomainException() {
        UUID id = UUID.randomUUID();
        SetNotificationAsAlreadyReadRequest request = new SetNotificationAsAlreadyReadRequest(id);
        doThrow(new RuntimeException("Notification not found"))
                .when(notificationRepository).markAsRead(id);

        Executable execution = () -> setNotificationAsAlreadyRead.execute(request);

        NotificationNotFoundException ex = assertThrows(NotificationNotFoundException.class, execution);
        assertEquals("La notificacion no existe: " + id, ex.getMessage());
    }

    @Test
    void execute_WhenOtherException_ShouldRethrow() {
        UUID id = UUID.randomUUID();
        SetNotificationAsAlreadyReadRequest request = new SetNotificationAsAlreadyReadRequest(id);
        doThrow(new RuntimeException("Database error"))
                .when(notificationRepository).markAsRead(id);

        Executable execution = () -> setNotificationAsAlreadyRead.execute(request);

        assertThrows(RuntimeException.class, execution);
    }
}
