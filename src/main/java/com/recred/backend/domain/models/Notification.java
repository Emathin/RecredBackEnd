package com.recred.backend.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record Notification(
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
