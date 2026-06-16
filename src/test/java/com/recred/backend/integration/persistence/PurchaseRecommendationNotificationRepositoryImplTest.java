package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PurchasePatternAnalysis;
import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.push.PushNotificationRequestedEvent;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseRecommendationNotificationRepositoryImplTest {

    @Mock
    private NotificacionJpaRepository notificationJpaRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void save_ShouldPersistRecommendationNotificationAndPublishPushEvent() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UsuarioEntity userEntity = userEntity(userId);
        ProductoEntity productEntity = productEntity(productId);
        AlumnoEntity studentEntity = studentEntity(studentId);
        SugerenciaIAEntity suggestionReference = suggestionEntity(suggestionId);
        SugerenciaIAEntity suggestion = suggestionEntity(suggestionId);
        when(entityManager.getReference(UsuarioEntity.class, userId)).thenReturn(userEntity);
        when(entityManager.getReference(ProductoEntity.class, productId))
                .thenReturn(productEntity);
        when(entityManager.getReference(AlumnoEntity.class, studentId))
                .thenReturn(studentEntity);
        when(entityManager.getReference(SugerenciaIAEntity.class, suggestionId))
                .thenReturn(suggestionReference);
        when(notificationJpaRepository.save(any(NotificacionEntity.class)))
                .thenAnswer(invocation -> {
                    NotificacionEntity notification = invocation.getArgument(0);
                    notification.setId(notificationId);
                    return notification;
                });
        PurchaseRecommendationNotificationRepositoryImpl repository =
                new PurchaseRecommendationNotificationRepositoryImpl(
                        notificationJpaRepository,
                        entityManager,
                        eventPublisher
                );

        repository.save(recommendation(studentId, productId), suggestion, userId);

        ArgumentCaptor<NotificacionEntity> notificationCaptor =
                ArgumentCaptor.forClass(NotificacionEntity.class);
        verify(notificationJpaRepository).save(notificationCaptor.capture());
        NotificacionEntity notification = notificationCaptor.getValue();
        assertEquals(userEntity, notification.getUsuarioEntity());
        assertEquals(productEntity, notification.getProductoEntity());
        assertEquals(studentEntity, notification.getAlumnoEntity());
        assertEquals(suggestionReference, notification.getSugerenciaIaEntity());
        assertEquals(TipoNotificacion.SUGERENCIA_IA, notification.getTipo());
        assertEquals("Compra tu Yogur", notification.getTitulo());
        assertEquals("Compra habitual.", notification.getMensaje());
        assertFalse(notification.getLeida());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PushNotificationRequestedEvent event =
                (PushNotificationRequestedEvent) eventCaptor.getValue();
        PushNotification push = event.notification();
        assertEquals(userId, push.userId());
        assertEquals(notificationId.toString(), push.data().get("notificacionId"));
        assertEquals(studentId.toString(), push.data().get("alumnoId"));
        assertEquals(productId.toString(), push.data().get("productoId"));
        assertEquals(suggestionId.toString(), push.data().get("sugerenciaIaId"));
    }

    private PurchaseRecommendationResult recommendation(UUID studentId, UUID productId) {
        return new PurchaseRecommendationResult(
                studentId,
                List.of(),
                Optional.of(new PurchasePatternAnalysis(
                        "Compra habitual.",
                        productId,
                        "Yogur",
                        "lunes"
                ))
        );
    }

    private UsuarioEntity userEntity(UUID userId) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(userId);
        return entity;
    }

    private ProductoEntity productEntity(UUID productId) {
        ProductoEntity entity = new ProductoEntity();
        entity.setId(productId);
        return entity;
    }

    private AlumnoEntity studentEntity(UUID studentId) {
        AlumnoEntity entity = new AlumnoEntity();
        entity.setId(studentId);
        return entity;
    }

    private SugerenciaIAEntity suggestionEntity(UUID suggestionId) {
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setId(suggestionId);
        return entity;
    }
}
