package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.push.PushNotificationRequestedEvent;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductAdditionNotificationRepositoryImplTest {

    @Mock
    private NotificacionJpaRepository notificationJpaRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void save_ShouldPersistNotificationAndPublishPushEvent() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UsuarioEntity userEntity = new UsuarioEntity();
        userEntity.setId(userId);
        ProductoEntity productEntity = new ProductoEntity();
        productEntity.setId(productId);
        when(entityManager.getReference(UsuarioEntity.class, userId)).thenReturn(userEntity);
        when(entityManager.getReference(ProductoEntity.class, productId))
                .thenReturn(productEntity);
        when(notificationJpaRepository.save(any(NotificacionEntity.class)))
                .thenAnswer(invocation -> {
                    NotificacionEntity notification = invocation.getArgument(0);
                    notification.setId(notificationId);
                    return notification;
                });
        ProductAdditionNotificationRepositoryImpl repository =
                new ProductAdditionNotificationRepositoryImpl(
                        notificationJpaRepository,
                        entityManager,
                        eventPublisher
                );

        repository.save(product(productId), kiosquero(userId));

        ArgumentCaptor<NotificacionEntity> notificationCaptor =
                ArgumentCaptor.forClass(NotificacionEntity.class);
        verify(notificationJpaRepository).save(notificationCaptor.capture());
        NotificacionEntity notification = notificationCaptor.getValue();
        assertEquals(userEntity, notification.getUsuarioEntity());
        assertEquals(productEntity, notification.getProductoEntity());
        assertEquals(TipoNotificacion.SUGERENCIA_IA, notification.getTipo());
        assertEquals("Sugerencia de producto", notification.getTitulo());
        assertFalse(notification.getLeida());
        assertTrue(notification.getMensaje().contains("Barrita"));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PushNotificationRequestedEvent event =
                (PushNotificationRequestedEvent) eventCaptor.getValue();
        PushNotification push = event.notification();
        assertEquals(userId, push.userId());
        assertEquals(notificationId.toString(), push.data().get("notificacionId"));
        assertEquals(productId.toString(), push.data().get("productoId"));
        assertEquals(TipoNotificacion.SUGERENCIA_IA.name(), push.data().get("tipo"));
    }

    private Producto product(UUID productId) {
        Producto product = new Producto();
        product.setId(productId);
        product.setNombre("Barrita");
        return product;
    }

    private Kiosquero kiosquero(UUID userId) {
        Usuario user = new Usuario();
        user.setId(userId);
        Kiosquero kiosquero = new Kiosquero();
        kiosquero.setUsuario(user);
        return kiosquero;
    }
}
