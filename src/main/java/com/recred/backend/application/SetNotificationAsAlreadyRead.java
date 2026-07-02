package com.recred.backend.application;

import com.recred.backend.domain.exceptions.NotificationNotFoundException;
import com.recred.backend.domain.repositories.NotificationRepository;

public class SetNotificationAsAlreadyRead {

    private final NotificationRepository notificationRepository;

    public SetNotificationAsAlreadyRead(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public SetNotificationAsAlreadyReadResponse execute(
            SetNotificationAsAlreadyReadRequest request
    ) {
        try {
            notificationRepository.markAsRead(request.notificationId());
            return new SetNotificationAsAlreadyReadResponse(request.notificationId());
        } catch (RuntimeException e) {
            if ("Notification not found".equals(e.getMessage())) {
                throw new NotificationNotFoundException(
                        "La notificacion no existe: " + request.notificationId());
            }
            throw e;
        }
    }
}
