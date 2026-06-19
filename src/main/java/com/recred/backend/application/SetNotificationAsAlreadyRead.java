package com.recred.backend.application;

import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.exceptions.NotificationNotFoundException;

import java.util.UUID;

public class SetNotificationAsAlreadyRead {

    private final NotificationRepository notificationRepository;

    public SetNotificationAsAlreadyRead(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void execute(UUID notificationId) {
        try {
            notificationRepository.markAsRead(notificationId);
        } catch (RuntimeException e) {
            if ("Notification not found".equals(e.getMessage())) {
                throw new NotificationNotFoundException(
                        "La notificación no existe: " + notificationId);
            }
            throw e;
        }
    }
}
