package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PushNotification;

public interface PushNotificationSender {
    void send(PushNotification notification);
}
