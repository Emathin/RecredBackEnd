package com.recred.backend.application;

import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.repositories.NotificationRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.recred.backend.domain.models.TipoNotificacion.RESUMEN_SEMANAL;

public class GenerateSummaryNotification {

    private final NotificationRepository notificationRepository;

    public GenerateSummaryNotification(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public GenerateSummaryNotificationResponse execute(GenerateSummaryNotificationRequest request) {
        Notification notification = createNotification();
        saveNotification(notification, request.userId());
        return createResponse(request.userId(), notification);
    }

    private Notification createNotification() {
        return new Notification(
                null,
                RESUMEN_SEMANAL,
                "Resumen Semanal",
                "Tu resumen semanal de gastos ya está disponible.",
                false,
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null
        );
    }

    private void saveNotification(Notification notification, UUID userId) {
        notificationRepository.save(notification, userId);
    }

    private GenerateSummaryNotificationResponse createResponse(UUID userId,
                                                               Notification notification) {
        return new GenerateSummaryNotificationResponse(
                userId,
                notification.title(),
                notification.message(),
                "RESUMEN_SEMANAL"
        );
    }
}
