package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.models.HealthyProductRecommendation;
import com.recred.backend.domain.models.HealthyRecommendationMessages;
import com.recred.backend.domain.models.HealthyRecommendationPattern;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.HealthyRecommendationNotificationRepository;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.push.PushNotificationRequestedEvent;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class HealthyRecommendationNotificationPersistenceRepository
        implements HealthyRecommendationNotificationRepository {

    private static final String INPUT_ORIGIN = "BACKEND_RECOMENDACION_SALUDABLE";

    private final SugerenciaIAJpaRepository suggestionJpaRepository;
    private final NotificacionJpaRepository notificationJpaRepository;
    private final AIEventJpaRepository aiEventJpaRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    public HealthyRecommendationNotificationPersistenceRepository(
            SugerenciaIAJpaRepository suggestionJpaRepository,
            NotificacionJpaRepository notificationJpaRepository,
            AIEventJpaRepository aiEventJpaRepository,
            EntityManager entityManager,
            ApplicationEventPublisher eventPublisher
    ) {
        this.suggestionJpaRepository = suggestionJpaRepository;
        this.notificationJpaRepository = notificationJpaRepository;
        this.aiEventJpaRepository = aiEventJpaRepository;
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public boolean hasRecommendationSince(UUID studentId, LocalDateTime since) {
        return suggestionJpaRepository.existsForStudentAndTypeSince(
                studentId,
                TipoSugerenciaIA.RECOMENDACION_SALUDABLE,
                since
        );
    }

    @Override
    public void save(HealthyProductRecommendation recommendation) {
        AIEventEntity event = saveEvent(recommendation);
        SugerenciaIAEntity suggestion = saveSuggestion(recommendation, event);
        saveAndPublishNotification(notification(recommendation, suggestion));
    }

    private AIEventEntity saveEvent(HealthyProductRecommendation recommendation) {
        HealthyRecommendationPattern pattern = recommendation.pattern();
        AIEventEntity event = new AIEventEntity();
        event.setType(AIEventType.ANALISIS_CONSUMO);
        event.setAlumnoEntity(studentReference(pattern.studentId()));
        event.setProductoEntity(productReference(pattern.suggestedProductId()));
        event.setInputOrigin(INPUT_ORIGIN);
        event.setInputData(inputData(pattern));
        event.setResultData(resultData(recommendation.messages()));
        event.setConfidence(recommendation.messages().confidence());

        AIEventEntity savedEvent = aiEventJpaRepository.save(event);
        if (savedEvent == null) {
            return event;
        }

        return savedEvent;
    }

    private SugerenciaIAEntity saveSuggestion(
            HealthyProductRecommendation recommendation,
            AIEventEntity event
    ) {
        HealthyRecommendationPattern pattern = recommendation.pattern();
        HealthyRecommendationMessages messages = recommendation.messages();
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setTipo(TipoSugerenciaIA.RECOMENDACION_SALUDABLE);
        suggestion.setAlumnoEntity(studentReference(pattern.studentId()));
        suggestion.setProductoEntity(productReference(pattern.suggestedProductId()));
        suggestion.setTitulo(messages.title());
        suggestion.setMensaje(messages.studentMessage());
        suggestion.setMetadata(metadata(recommendation));
        suggestion.setAceptada(false);
        suggestion.setEventoIaEntity(event);
        return suggestionJpaRepository.save(suggestion);
    }

    private NotificacionEntity notification(
            HealthyProductRecommendation recommendation,
            SugerenciaIAEntity suggestion
    ) {
        NotificacionEntity notification = new NotificacionEntity();
        notification.setUsuarioEntity(reference(
                UsuarioEntity.class,
                recommendation.candidate().studentUserId()
        ));
        notification.setAlumnoEntity(studentReference(recommendation.pattern().studentId()));
        notification.setProductoEntity(productReference(
                recommendation.pattern().suggestedProductId()
        ));
        notification.setSugerenciaIaEntity(suggestion);
        notification.setTipo(TipoNotificacion.SUGERENCIA_IA);
        notification.setTitulo(recommendation.messages().title());
        notification.setMensaje(recommendation.messages().studentMessage());
        notification.setLeida(false);
        return notification;
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
        putIfPresent(data, "notificacionId", notification.getId());
        putIfPresent(data, "alumnoId", notification.getAlumnoEntity().getId());
        putIfPresent(data, "productoId", productId(notification));
        putIfPresent(data, "sugerenciaIaId", suggestionId(notification));
        data.put("tipo", notification.getTipo().name());
        return data;
    }

    private UUID productId(NotificacionEntity notification) {
        if (notification.getProductoEntity() == null) {
            return null;
        }

        return notification.getProductoEntity().getId();
    }

    private UUID suggestionId(NotificacionEntity notification) {
        if (notification.getSugerenciaIaEntity() == null) {
            return null;
        }

        return notification.getSugerenciaIaEntity().getId();
    }

    private void putIfPresent(Map<String, String> data, String key, UUID value) {
        if (value != null) {
            data.put(key, value.toString());
        }
    }

    private Map<String, Object> inputData(HealthyRecommendationPattern pattern) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fechaCalculo", pattern.calculationDate().toString());
        data.put("fechaInicio", pattern.startDate().toString());
        data.put("fechaFin", pattern.endDate().toString());
        data.put("productoRepetido", pattern.repeatedProductName());
        data.put("categoriaProductoRepetido", pattern.repeatedProductCategory());
        data.put("diasRepetidos", pattern.repeatedDays());
        data.put("cantidadTotal", pattern.totalQuantity());
        data.put("productoSugerido", pattern.suggestedProductName());
        data.put("categoriaProductoSugerido", pattern.suggestedProductCategory());
        return data;
    }

    private Map<String, Object> resultData(HealthyRecommendationMessages messages) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("titulo", messages.title());
        data.put("mensajeAlumno", messages.studentMessage());
        data.put("accionSugerida", messages.suggestedAction());
        data.put("productoIdSugerido", messages.suggestedProductId());
        data.put("modelo", messages.model());
        return data;
    }

    private Map<String, Object> metadata(HealthyProductRecommendation recommendation) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.putAll(inputData(recommendation.pattern()));
        data.putAll(resultData(recommendation.messages()));
        data.put("confianza", recommendation.messages().confidence());
        return data;
    }

    private AlumnoEntity studentReference(UUID studentId) {
        return reference(AlumnoEntity.class, studentId);
    }

    private ProductoEntity productReference(UUID productId) {
        if (productId == null) {
            return null;
        }

        return reference(ProductoEntity.class, productId);
    }

    private <T> T reference(Class<T> type, UUID id) {
        return entityManager.getReference(type, id);
    }
}
