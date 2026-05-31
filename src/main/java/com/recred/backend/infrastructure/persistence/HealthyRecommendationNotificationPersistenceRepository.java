package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.models.HealthyProductRecommendation;
import com.recred.backend.domain.models.HealthyRecommendationMessages;
import com.recred.backend.domain.models.HealthyRecommendationPattern;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.NotificationType;
import com.recred.backend.domain.models.IaSuggestionType;
import com.recred.backend.domain.repositories.HealthyRecommendationNotificationRepository;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.NotificationEntity;
import com.recred.backend.infrastructure.entities.ProductEntity;
import com.recred.backend.infrastructure.entities.AISuggestionEntity;
import com.recred.backend.infrastructure.entities.UserEntity;
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
                IaSuggestionType.RECOMENDACION_SALUDABLE,
                since
        );
    }

    @Override
    public void save(HealthyProductRecommendation recommendation) {
        AIEventEntity event = saveEvent(recommendation);
        AISuggestionEntity suggestion = saveSuggestion(recommendation, event);
        saveAndPublishNotification(notification(recommendation, suggestion));
    }

    private AIEventEntity saveEvent(HealthyProductRecommendation recommendation) {
        HealthyRecommendationPattern pattern = recommendation.pattern();
        AIEventEntity event = new AIEventEntity();
        event.setType(AIEventType.ANALISIS_CONSUMO);
        event.setStudentEntity(studentReference(pattern.studentId()));
        event.setProductEntity(productReference(pattern.suggestedProductId()));
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

    private AISuggestionEntity saveSuggestion(
            HealthyProductRecommendation recommendation,
            AIEventEntity event
    ) {
        HealthyRecommendationPattern pattern = recommendation.pattern();
        HealthyRecommendationMessages messages = recommendation.messages();
        AISuggestionEntity suggestion = new AISuggestionEntity();
        suggestion.setType(IaSuggestionType.RECOMENDACION_SALUDABLE);
        suggestion.setStudentEntity(studentReference(pattern.studentId()));
        suggestion.setProductEntity(productReference(pattern.suggestedProductId()));
        suggestion.setTittle(messages.title());
        suggestion.setMessage(messages.studentMessage());
        suggestion.setMetadata(metadata(recommendation));
        suggestion.setAccepted(false);
        suggestion.setEventoIaEntity(event);
        return suggestionJpaRepository.save(suggestion);
    }

    private NotificationEntity notification(
            HealthyProductRecommendation recommendation,
            AISuggestionEntity suggestion
    ) {
        NotificationEntity notification = new NotificationEntity();
        notification.setUserEntity(reference(
                UserEntity.class,
                recommendation.candidate().studentUserId()
        ));
        notification.setStudentEntity(studentReference(recommendation.pattern().studentId()));
        notification.setProductEntity(productReference(
                recommendation.pattern().suggestedProductId()
        ));
        notification.setAISuggestionEntity(suggestion);
        notification.setType(NotificationType.SUGERENCIA_IA);
        notification.setTittle(recommendation.messages().title());
        notification.setMessage(recommendation.messages().studentMessage());
        notification.setSeen(false);
        return notification;
    }

    private void saveAndPublishNotification(NotificationEntity notification) {
        NotificationEntity savedNotification = notificationJpaRepository.save(notification);
        if (savedNotification == null) {
            savedNotification = notification;
        }

        publishPushNotification(savedNotification);
    }

    private void publishPushNotification(NotificationEntity notification) {
        eventPublisher.publishEvent(new PushNotificationRequestedEvent(new PushNotification(
                notification.getUserEntity().getId(),
                notification.getTittle(),
                notification.getMessage(),
                pushData(notification)
        )));
    }

    private Map<String, String> pushData(NotificationEntity notification) {
        Map<String, String> data = new LinkedHashMap<>();
        putIfPresent(data, "notificacionId", notification.getId());
        putIfPresent(data, "studentId", notification.getStudentEntity().getId());
        putIfPresent(data, "productId", productId(notification));
        putIfPresent(data, "sugerenciaIaId", suggestionId(notification));
        data.put("tipo", notification.getType().name());
        return data;
    }

    private UUID productId(NotificationEntity notification) {
        if (notification.getProductEntity() == null) {
            return null;
        }

        return notification.getProductEntity().getId();
    }

    private UUID suggestionId(NotificationEntity notification) {
        if (notification.getAISuggestionEntity() == null) {
            return null;
        }

        return notification.getAISuggestionEntity().getId();
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

    private StudentEntity studentReference(UUID studentId) {
        return reference(StudentEntity.class, studentId);
    }

    private ProductEntity productReference(UUID productId) {
        if (productId == null) {
            return null;
        }

        return reference(ProductEntity.class, productId);
    }

    private <T> T reference(Class<T> type, UUID id) {
        return entityManager.getReference(type, id);
    }
}
