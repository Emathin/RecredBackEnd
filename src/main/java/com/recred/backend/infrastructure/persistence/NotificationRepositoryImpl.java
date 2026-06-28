package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.persistence.SpringDataUsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificacionJpaRepository notificationJpaRepository;
    private final SpringDataUsuarioRepository usuarioRepository;

    public NotificationRepositoryImpl(NotificacionJpaRepository notificationJpaRepository,
                                      SpringDataUsuarioRepository usuarioRepository) {
        this.notificationJpaRepository = notificationJpaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<Notification> findByUserId(UUID userId, boolean unreadOnly, int page, int size) {
        return notificationJpaRepository
                .findByUserId(userId, unreadOnly, PageRequest.of(page, size))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void markAsRead(UUID notificationId) {
        NotificacionEntity entity = notificationJpaRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        entity.setLeida(true);
        entity.setLeidaEn(java.time.LocalDateTime.now());
        notificationJpaRepository.save(entity);
    }

    @Override
    public void save(Notification notification, UUID userId) {
        com.recred.backend.infrastructure.entities.UsuarioEntity usuario =
                usuarioRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        NotificacionEntity entity = new NotificacionEntity();
        if (notification.id() != null) {
            entity.setId(notification.id());
        }
        entity.setUsuarioEntity(usuario);
        entity.setTipo(notification.type());
        entity.setTitulo(notification.title());
        entity.setMensaje(notification.message());
        entity.setLeida(notification.read() != null ? notification.read() : false);
        entity.setCreadoEn(notification.createdAt() !=
                null ? notification.createdAt() : java.time.LocalDateTime.now());
        entity.setLeidaEn(notification.readAt());
        
        notificationJpaRepository.save(entity);
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
