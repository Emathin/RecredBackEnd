package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateSummaryNotification;
import com.recred.backend.application.GenerateSummaryNotificationRequest;
import com.recred.backend.application.GenerateSummaryNotificationResponse;
import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.repositories.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static com.recred.backend.domain.models.TipoNotificacion.RESUMEN_SEMANAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GenerateSummaryNotificationTest {

    private static final String SUMMARY_TITLE = "Resumen Semanal";
    private static final String SUMMARY_MESSAGE = "Tu resumen semanal de gastos ya está disponible.";
    private static final String SUMMARY_TYPE = "RESUMEN_SEMANAL";

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final GenerateSummaryNotification useCase = new GenerateSummaryNotification(notificationRepository);

    @Test
    void createAndSaveSummaryNotification() {
        UUID userId = UUID.randomUUID();

        GenerateSummaryNotificationResponse response = whenExecute(userId);

        thenResponseIsCorrect(response, userId);
        thenNotificationIsSaved(userId);
    }

    private GenerateSummaryNotificationResponse whenExecute(UUID userId) {
        return useCase.execute(new GenerateSummaryNotificationRequest(userId));
    }

    private void thenResponseIsCorrect(GenerateSummaryNotificationResponse response, UUID userId) {
        assertEquals(userId, response.userId());
        assertEquals(SUMMARY_TITLE, response.title());
        assertEquals(SUMMARY_MESSAGE, response.message());
        assertEquals(SUMMARY_TYPE, response.type());
    }

    private void thenNotificationIsSaved(UUID userId) {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture(), eq(userId));

        Notification saved = captor.getValue();
        assertEquals(RESUMEN_SEMANAL, saved.type());
        assertEquals(SUMMARY_TITLE, saved.title());
        assertEquals(SUMMARY_MESSAGE, saved.message());
        assertFalse(saved.read());
    }
}
