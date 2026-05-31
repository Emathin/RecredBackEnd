package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.repositories.PurchaseRecommendationNotificationRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.push.PushNotificationRequestedEvent;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class PurchaseRecommendationNotificationRepositoryImpl
        implements PurchaseRecommendationNotificationRepository {

    private final NotificacionJpaRepository notificationJpaRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    public PurchaseRecommendationNotificationRepositoryImpl(
            NotificacionJpaRepository notificationJpaRepository,
            EntityManager entityManager, ApplicationEventPublisher eventPublisher) {
        this.notificationJpaRepository = notificationJpaRepository;
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void save(PurchaseRecommendationResult recommendation,
                     SugerenciaIAEntity suggestion, UUID userId) {

        NotificacionEntity savedNotification =
                notificationJpaRepository.save(
                        notification(recommendation, suggestion,userId));

        eventPublisher.publishEvent(new PushNotificationRequestedEvent(new PushNotification(
                savedNotification.getUsuarioEntity().getId(),
                savedNotification.getTitulo(),
                savedNotification.getMensaje(),
                pushData(savedNotification)
        )));
    }

    private NotificacionEntity notification(
            PurchaseRecommendationResult recommendation,
            SugerenciaIAEntity suggestion,
            UUID userId
    ) {
        NotificacionEntity notification = new NotificacionEntity();

        notification.setUsuarioEntity(entityManager.getReference(
                UsuarioEntity.class,
                userId
        ));
        notification.setProductoEntity(entityManager.getReference(
                ProductoEntity.class,
                recommendation.getAiAnalysis().get().product_id()
        ));
        notification.setAlumnoEntity(entityManager.getReference(
                AlumnoEntity.class,
                recommendation.getAlumnoId()
        ));
        notification.setSugerenciaIaEntity(entityManager.getReference(
                SugerenciaIAEntity.class,
                suggestion.getId()
        ));
        notification.setTipo(TipoNotificacion.SUGERENCIA_IA);
        notification.setTitulo(
                "Compra tu "+ recommendation.getAiAnalysis().get().productName());
        notification.setMensaje(recommendation.getAiAnalysis().get().summary());
        notification.setLeida(false);
        return notification;
    }

    private Map<String, String> pushData(NotificacionEntity notification) {
        Map<String, String> data = new LinkedHashMap<>();
        putIfPresent(data, "notificacionId", notification.getId());
        putIfPresent(data, "alumnoId", notification.getAlumnoEntity().getId());
        putIfPresent(data, "productoId", notification.getProductoEntity().getId());
        putIfPresent(data, "sugerenciaIaId", notification.getSugerenciaIaEntity().getId());
        data.put("tipo", notification.getTipo().name());
        return data;
    }

    private void putIfPresent(Map<String, String> data, String key, UUID value) {
        if (value != null) {
            data.put(key, value.toString());
        }
    }
}
