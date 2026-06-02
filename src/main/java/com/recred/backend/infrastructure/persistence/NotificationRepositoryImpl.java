package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificacionJpaRepository notificationJpaRepository;

    public NotificationRepositoryImpl(NotificacionJpaRepository notificationJpaRepository) {
        this.notificationJpaRepository = notificationJpaRepository;
    }

    @Override
    public List<Notification> findByUserId(UUID userId, boolean unreadOnly, int page, int size) {
        return notificationJpaRepository
                .findByUserId(userId, unreadOnly, PageRequest.of(page, size))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Notification toDomain(NotificacionEntity entity) {
        return new Notification(
                entity.getId(),
                entity.getTipo(),
                entity.getTitulo(),
                entity.getMensaje(),
                entity.getLeida(),
                entity.getCreadoEn(),
                entity.getLeidaEn()
        );
    }
}
