package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.repositories.ProductAdditionNotificationRepository;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.push.PushNotificationRequestedEvent;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ProductAdditionNotificationRepositoryImpl
        implements ProductAdditionNotificationRepository {

    private final NotificacionJpaRepository notificationJpaRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    public ProductAdditionNotificationRepositoryImpl(
            NotificacionJpaRepository notificationJpaRepository,
            EntityManager entityManager,
            ApplicationEventPublisher eventPublisher
    ) {
        this.notificationJpaRepository = notificationJpaRepository;
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void save(Producto product, Kiosquero kiosquero, SugerenciaIA suggestion) {
        NotificacionEntity notification = new NotificacionEntity();
        notification.setUsuarioEntity(entityManager.getReference(
                UsuarioEntity.class,
                kiosquero.getUsuario().getId()
        ));
        notification.setProductoEntity(entityManager.getReference(
                ProductoEntity.class,
                product.getId()
        ));
        notification.setSugerenciaIaEntity(entityManager.getReference(
                SugerenciaIAEntity.class,
                suggestion.getId()
        ));
        notification.setTipo(TipoNotificacion.SUGERENCIA_IA);
        notification.setTitulo("Sugerencia de producto");
        notification.setMensaje("Te recomendamos agregar " + product.getNombre() +
                " a tu stock" );
        notification.setLeida(false);

        saveAndPublishNotification(notification);
    }

    private void saveAndPublishNotification(NotificacionEntity notification) {
        NotificacionEntity savedNotification = notificationJpaRepository.save(notification);
        if (savedNotification == null) {
            savedNotification = notification;
        }
        publishPushNotification(savedNotification);
    }

    private void publishPushNotification(NotificacionEntity notification) {
        eventPublisher.publishEvent(new PushNotificationRequestedEvent(new PushNotification(
                notification.getUsuarioEntity().getId(),
                notification.getTitulo(),
                notification.getMensaje(),
                pushData(notification)
        )));
    }

    private Map<String, String> pushData(NotificacionEntity notification) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("notificacionId", notification.getId().toString());
        if (notification.getProductoEntity() != null) {
            data.put("productoId", notification.getProductoEntity().getId().toString());
        }
        data.put("tipo", notification.getTipo().name());
        return data;
    }
}
