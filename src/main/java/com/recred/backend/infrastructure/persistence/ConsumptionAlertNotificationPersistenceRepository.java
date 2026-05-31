package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ConsumptionAlert;
import com.recred.backend.domain.models.ConsumptionAlertMessages;
import com.recred.backend.domain.models.ConsumptionAlertPattern;
import com.recred.backend.domain.models.ConsumptionAlertTutor;
import com.recred.backend.domain.models.ConsumptionAlertType;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.NotificationType;
import com.recred.backend.domain.models.IaSuggestionType;
import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.repositories.ConsumptionAlertNotificationRepository;
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
public class ConsumptionAlertNotificationPersistenceRepository
        implements ConsumptionAlertNotificationRepository {

    private static final String INPUT_ORIGIN = "BACKEND_ALERTA_ESTADISTICA_CONSUMO";

    private final SugerenciaIAJpaRepository suggestionJpaRepository;
    private final NotificacionJpaRepository notificationJpaRepository;
    private final AIEventJpaRepository aiEventJpaRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    public ConsumptionAlertNotificationPersistenceRepository(
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
    public boolean hasAnyAlertBetween(
            UUID studentId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        return suggestionJpaRepository.existsAnyForStudentBetween(
                studentId,
                startDateTime,
                endDateTime
        );
    }

    @Override
    public boolean hasAlertSince(
            UUID studentId,
            ConsumptionAlertType alertType,
            LocalDateTime since
    ) {
        return suggestionJpaRepository.existsForStudentAndTypeSince(
                studentId,
                suggestionType(alertType),
                since
        );
    }

    @Override
    public void save(ConsumptionAlert alert) {
        AIEventEntity event = saveEvent(alert);
        AISuggestionEntity suggestion = saveSuggestion(alert, event);
        saveStudentNotification(alert, suggestion);
        alert.candidate().tutors()
                .forEach(tutor -> saveTutorNotification(alert, tutor, suggestion));
    }

    private AIEventEntity saveEvent(ConsumptionAlert alert) {
        ConsumptionAlertPattern pattern = alert.pattern();
        AIEventEntity event = new AIEventEntity();
        event.setType(AIEventType.ANALISIS_CONSUMO);
        event.setStudentEntity(studentReference(pattern.studentId()));
        event.setProductEntity(productReference(pattern.suggestedProductId()));
        event.setInputOrigin(INPUT_ORIGIN);
        event.setInputData(inputData(pattern));
        event.setResultData(resultData(alert.messages()));
        event.setConfidence(alert.messages().confidence());

        AIEventEntity savedEvent = aiEventJpaRepository.save(event);
        if (savedEvent == null) {
            return event;
        }
        return savedEvent;
    }

    private AISuggestionEntity saveSuggestion(
            ConsumptionAlert alert,
            AIEventEntity event
    ) {
        ConsumptionAlertPattern pattern = alert.pattern();
        ConsumptionAlertMessages messages = alert.messages();
        AISuggestionEntity suggestion = new AISuggestionEntity();
        suggestion.setType(suggestionType(pattern.type()));
        suggestion.setStudentEntity(studentReference(pattern.studentId()));
        suggestion.setProductEntity(productReference(pattern.suggestedProductId()));
        suggestion.setTittle(messages.title());
        suggestion.setMessage(messages.studentMessage());
        suggestion.setMetadata(metadata(alert));
        suggestion.setAccepted(false);
        suggestion.setEventoIaEntity(event);
        return suggestionJpaRepository.save(suggestion);
    }

    private void saveStudentNotification(
            ConsumptionAlert alert,
            AISuggestionEntity suggestion
    ) {
        saveAndPublishNotification(notification(
                alert.candidate().studentUserId(),
                alert.pattern().studentId(),
                alert.pattern().suggestedProductId(),
                suggestion,
                alert.messages().title(),
                alert.messages().studentMessage()
        ));
    }

    private void saveTutorNotification(
            ConsumptionAlert alert,
            ConsumptionAlertTutor tutor,
            AISuggestionEntity suggestion
    ) {
        saveAndPublishNotification(notification(
                tutor.userId(),
                alert.pattern().studentId(),
                alert.pattern().suggestedProductId(),
                suggestion,
                alert.messages().tutorTitle(),
                alert.messages().tutorMessage()
        ));
    }

    private void saveAndPublishNotification(NotificationEntity notification) {
        NotificationEntity savedNotification = notificationJpaRepository.save(notification);
        if (savedNotification == null) {
            savedNotification = notification;
        }
        publishPushNotification(savedNotification);
    }

    private NotificationEntity notification(
            UUID userId,
            UUID studentId,
            UUID productId,
            AISuggestionEntity suggestion,
            String title,
            String message
    ) {
        NotificationEntity notification = new NotificationEntity();
        notification.setUserEntity(reference(UserEntity.class, userId));
        notification.setStudentEntity(studentReference(studentId));
        notification.setProductEntity(productReference(productId));
        notification.setAISuggestionEntity(suggestion);
        notification.setType(NotificationType.SUGERENCIA_IA);
        notification.setTittle(title);
        notification.setMessage(message);
        notification.setSeen(false);
        return notification;
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

    private Map<String, Object> inputData(ConsumptionAlertPattern pattern) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tipo", pattern.type().name());
        data.put("fechaCalculo", pattern.calculationDate().toString());
        data.put("fechaInicio", pattern.startDate().toString());
        data.put("fechaFin", pattern.endDate().toString());
        data.put("cantidadCompras", pattern.purchaseCount());
        data.put("montoTotal", pattern.totalAmount());
        data.put("categoriaPrincipal", pattern.mainCategory());
        data.put("porcentajeCategoriaPrincipal", pattern.mainCategoryPercentage());
        data.put("productoSugerido", pattern.suggestedProductName());
        return data;
    }

    private Map<String, Object> resultData(ConsumptionAlertMessages messages) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("titulo", messages.title());
        data.put("tituloTutor", messages.tutorTitle());
        data.put("mensajeAlumno", messages.studentMessage());
        data.put("mensajeTutor", messages.tutorMessage());
        data.put("accionSugerida", messages.suggestedAction());
        data.put("modelo", messages.model());
        return data;
    }

    private Map<String, Object> metadata(ConsumptionAlert alert) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.putAll(inputData(alert.pattern()));
        data.putAll(resultData(alert.messages()));
        data.put("confianza", alert.messages().confidence());
        return data;
    }

    private IaSuggestionType suggestionType(ConsumptionAlertType alertType) {
        if (alertType == ConsumptionAlertType.STATISTICAL_PATTERN) {
            return IaSuggestionType.RESUMEN_CONSUMO;
        }
        return IaSuggestionType.RECOMENDACION_SALUDABLE;
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
