package com.recred.backend.infrastructure.push;

import com.recred.backend.domain.models.PushNotification;

public record PushNotificationRequestedEvent(PushNotification notification) {
}
