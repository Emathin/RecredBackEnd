package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.CompraPresencialEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.NotificacionJpaRepository;
import com.recred.backend.infrastructure.persistence.NotificationRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataUsuarioRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRepositoryImplTest {

    @Mock
    private NotificacionJpaRepository notificationJpaRepository;

    @Mock
    private SpringDataUsuarioRepository usuarioRepository;

    @Mock
    private EntityManager entityManager;

    private NotificationRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new NotificationRepositoryImpl(
                notificationJpaRepository,
                usuarioRepository,
                entityManager
        );
    }

    @Test
    void findByUserIdShouldMapEntityRelationsToDomain() {
        UUID userId = UUID.randomUUID();
        NotificacionEntity entity = notificationEntity();
        when(notificationJpaRepository.findByUserId(userId, true, PageRequest.of(1, 5)))
                .thenReturn(List.of(entity));

        List<Notification> result = repository.findByUserId(userId, true, 1, 5);

        assertEquals(1, result.size());
        Notification notification = result.get(0);
        assertEquals(entity.getId(), notification.id());
        assertEquals(TipoNotificacion.SUGERENCIA_IA, notification.type());
        assertEquals(entity.getAlumnoEntity().getId(), notification.studentId());
        assertEquals(entity.getProductoEntity().getId(), notification.productId());
        assertEquals(entity.getCompraEntity().getId(), notification.purchaseId());
        assertEquals(entity.getSugerenciaIaEntity().getId(), notification.suggestionId());
    }

    @Test
    void markAsReadShouldUpdateEntityAndSave() {
        UUID notificationId = UUID.randomUUID();
        NotificacionEntity entity = notificationEntity();
        entity.setId(notificationId);
        entity.setLeida(false);
        when(notificationJpaRepository.findById(notificationId)).thenReturn(Optional.of(entity));

        repository.markAsRead(notificationId);

        assertTrue(entity.getLeida());
        assertNotNull(entity.getLeidaEn());
        verify(notificationJpaRepository).save(entity);
    }

    @Test
    void markAsReadShouldThrowWhenNotificationDoesNotExist() {
        UUID notificationId = UUID.randomUUID();
        when(notificationJpaRepository.findById(notificationId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> repository.markAsRead(notificationId));

        assertEquals("Notification not found", exception.getMessage());
    }

    @Test
    void saveShouldMapDefaultsAndPersistNotification() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UsuarioEntity user = new UsuarioEntity();
        user.setId(userId);
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(user));
        Notification notification = new Notification(
                notificationId,
                TipoNotificacion.SISTEMA,
                "Titulo",
                "Mensaje",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        repository.save(notification, userId);

        ArgumentCaptor<NotificacionEntity> captor =
                ArgumentCaptor.forClass(NotificacionEntity.class);
        verify(notificationJpaRepository).save(captor.capture());
        NotificacionEntity saved = captor.getValue();
        assertEquals(notificationId, saved.getId());
        assertEquals(userId, saved.getUsuarioEntity().getId());
        assertEquals(TipoNotificacion.SISTEMA, saved.getTipo());
        assertFalse(saved.getLeida());
        assertNotNull(saved.getCreadoEn());
    }

    @Test
    void saveShouldThrowWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> repository.save(notification(), userId));

        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    private Notification notification() {
        return new Notification(
                null,
                TipoNotificacion.SISTEMA,
                "Titulo",
                "Mensaje",
                true,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 11, 0),
                null,
                null,
                null,
                null
        );
    }

    private NotificacionEntity notificationEntity() {
        NotificacionEntity entity = new NotificacionEntity();
        entity.setId(UUID.randomUUID());
        entity.setTipo(TipoNotificacion.SUGERENCIA_IA);
        entity.setTitulo("Titulo");
        entity.setMensaje("Mensaje");
        entity.setLeida(false);
        entity.setCreadoEn(LocalDateTime.of(2026, 7, 1, 10, 0));
        entity.setAlumnoEntity(alumnoEntity());
        entity.setProductoEntity(productEntity());
        entity.setCompraEntity(purchaseEntity());
        entity.setSugerenciaIaEntity(suggestionEntity());
        return entity;
    }

    private AlumnoEntity alumnoEntity() {
        AlumnoEntity entity = new AlumnoEntity();
        entity.setId(UUID.randomUUID());
        return entity;
    }

    private ProductoEntity productEntity() {
        ProductoEntity entity = new ProductoEntity();
        entity.setId(UUID.randomUUID());
        return entity;
    }

    private CompraPresencialEntity purchaseEntity() {
        CompraPresencialEntity entity = new CompraPresencialEntity();
        entity.setId(UUID.randomUUID());
        return entity;
    }

    private SugerenciaIAEntity suggestionEntity() {
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setId(UUID.randomUUID());
        return entity;
    }
}
