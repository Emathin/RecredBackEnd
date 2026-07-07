package com.recred.backend.application;

import com.recred.backend.domain.exceptions.NotificationNotFoundException;
import com.recred.backend.domain.repositories.NotificationRepository;

public class SetNotificationAsAlreadyRead {

    private final NotificationRepository notificationRepository;

    public SetNotificationAsAlreadyRead(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public SetNotificationAsAlreadyReadResponse execute(
            SetNotificationAsAlreadyReadRequest readRequest
    ) {
        try {
            notificationRepository.markAsRead(readRequest.notificationId());
            return new SetNotificationAsAlreadyReadResponse(readRequest.notificationId());
        } catch (RuntimeException e) {
            if (notificationDoesNotExist(e)) {
                throw new NotificationNotFoundException(
                        "La notificacion no existe: " + readRequest.notificationId());
            }
            throw e;
        }
    }

    private boolean notificationDoesNotExist(RuntimeException exception) {
        return "Notification not found".equals(exception.getMessage());
    }
}
