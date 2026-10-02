package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.CompraEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificacionJpaRepository notificationJpaRepository;
    private final SpringDataUsuarioRepository usuarioRepository;
    private final EntityManager entityManager;

    public NotificationRepositoryImpl(NotificacionJpaRepository notificationJpaRepository,
                                      SpringDataUsuarioRepository usuarioRepository,
                                      EntityManager entityManager) {
        this.notificationJpaRepository = notificationJpaRepository;
        this.usuarioRepository = usuarioRepository;
        this.entityManager = entityManager;
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
        
        if (notification.studentId() != null) {
            entity.setAlumnoEntity(entityManager.getReference(
                    AlumnoEntity.class, notification.studentId()));
        }
        if (notification.productId() != null) {
            entity.setProductoEntity(entityManager.getReference(
                    ProductoEntity.class, notification.productId()));
        }
        if (notification.purchaseId() != null) {
            entity.setCompraEntity(entityManager.getReference(
                    CompraEntity.class, notification.purchaseId()));
        }
        if (notification.suggestionId() != null) {
            entity.setSugerenciaIaEntity(entityManager.getReference(
                    SugerenciaIAEntity.class, notification.suggestionId()));
        }
        
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
                entity.getLeidaEn(),
                entity.getAlumnoEntity() != null
                        ? entity.getAlumnoEntity().getId() : null,
                entity.getProductoEntity() != null
                        ? entity.getProductoEntity().getId() : null,
                entity.getCompraEntity() != null
                        ? entity.getCompraEntity().getId() : null,
                entity.getSugerenciaIaEntity() != null
                        ? entity.getSugerenciaIaEntity().getId() : null
        );
    }
}
