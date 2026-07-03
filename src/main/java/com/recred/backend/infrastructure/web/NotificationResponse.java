package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.TipoNotificacion;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        TipoNotificacion type,
        String title,
        String message,
        Boolean read,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        UUID studentId,
        UUID productId,
        UUID purchaseId,
        UUID suggestionId
) {
    public static NotificationResponse fromDomain(Notification notification) {
        return new NotificationResponse(
                notification.id(),
                notification.type(),
                notification.title(),
                notification.message(),
                notification.read(),
                notification.createdAt(),
                notification.readAt(),
                notification.studentId(),
                notification.productId(),
                notification.purchaseId(),
                notification.suggestionId()
        );
    }
}
