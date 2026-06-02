package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Notification;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository {
    List<Notification> findByUserId(UUID userId, boolean unreadOnly, int page, int size);
}
