package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateSummaryNotification;
import com.recred.backend.application.GenerateSummaryNotificationRequest;
import com.recred.backend.application.GenerateSummaryNotificationResponse;
import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.repositories.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GenerateSummaryNotificationTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private GenerateSummaryNotification generateSummaryNotification;

    @Test
    void executeCreatesAndSavesNotification() {
        UUID userId = UUID.randomUUID();

        GenerateSummaryNotificationResponse response = generateSummaryNotification
                .execute(new GenerateSummaryNotificationRequest(userId));

        assertEquals(userId, response.userId());
        assertEquals("Resumen Semanal", response.title());
        assertEquals("Tu resumen semanal de gastos ya está disponible.", response.message());
        assertEquals("RESUMEN_SEMANAL", response.type());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture(), eq(userId));

        Notification saved = captor.getValue();
        assertEquals(TipoNotificacion.RESUMEN_SEMANAL, saved.type());
        assertEquals("Resumen Semanal", saved.title());
        assertEquals("Tu resumen semanal de gastos ya está disponible.", saved.message());
        assertEquals(false, saved.read());
    }
}
