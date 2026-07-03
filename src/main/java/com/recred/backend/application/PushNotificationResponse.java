package com.recred.backend.application;

import com.recred.backend.domain.models.PushNotification;
import java.util.Map;
import java.util.UUID;

public record PushNotificationResponse(
        UUID userId,
        String title,
        String message,
        Map<String, String> data
) {

    public static PushNotificationResponse fromDomain(PushNotification notification) {
        if (notification == null) {
            return null;
        }

        return new PushNotificationResponse(
                notification.userId(),
                notification.title(),
                notification.message(),
                notification.data()
        );
    }
}
