package com.recred.backend.domain.models;

import java.util.Map;
import java.util.UUID;

public record PushNotification(
        UUID userId,
        String title,
        String message,
        Map<String, String> data
) {
}
