package com.recred.backend.application;

import com.recred.backend.domain.models.TipoNotificacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GetLoggedUserNotificationsResponse(
        List<NotificationDTO> notifications
) {
    public record NotificationDTO(
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
    }
}
