package com.recred.backend.application;

import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.TipoNotificacion;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserNotificationResponse(
        UUID id,
        TipoNotificacion type,
        String title,
        String message,
        Boolean read,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {

    public static UserNotificationResponse fromDomain(Notification notification) {
        return new UserNotificationResponse(
                notification.id(),
                notification.type(),
                notification.title(),
                notification.message(),
                notification.read(),
                notification.createdAt(),
                notification.readAt()
        );
    }
}
