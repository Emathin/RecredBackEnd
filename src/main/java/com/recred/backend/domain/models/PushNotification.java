package com.recred.backend.domain.models;

import java.util.Map;
import java.util.UUID;

public record PushNotification(
        UUID userId,
        String title,
        String message,
        Map<String, String> data
) {
    public static PushNotification dataOnly(UUID userId, Map<String, String> data) {
        return new PushNotification(userId, null, null, data);
    }
}
