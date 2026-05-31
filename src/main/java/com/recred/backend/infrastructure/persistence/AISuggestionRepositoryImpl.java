package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.models.PriceAlertSuggestion;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.NotificationType;
import com.recred.backend.domain.models.IaSuggestionType;
import com.recred.backend.domain.repositories.AISuggestionRepository;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.NotificationEntity;
import com.recred.backend.infrastructure.entities.ProductEntity;
import com.recred.backend.infrastructure.entities.AISuggestionEntity;
import com.recred.backend.infrastructure.entities.UserEntity;
import com.recred.backend.infrastructure.push.PushNotificationRequestedEvent;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AISuggestionRepositoryImpl implements AISuggestionRepository {

    private static final String INPUT_ORIGIN = "BACKEND_ALERTA_PRECIO";

    private final SugerenciaIAJpaRepository springDataRepository;
    private final AIEventJpaRepository aiEventJpaRepository;
    private final NotificacionJpaRepository notificationJpaRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    public AISuggestionRepositoryImpl(
            SugerenciaIAJpaRepository springDataRepository,
            AIEventJpaRepository aiEventJpaRepository,
            NotificacionJpaRepository notificationJpaRepository,
            EntityManager entityManager,
            ApplicationEventPublisher eventPublisher) {
        this.springDataRepository = springDataRepository;
        this.aiEventJpaRepository = aiEventJpaRepository;
        this.notificationJpaRepository = notificationJpaRepository;
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void save(AISuggestionEntity sugerencia) {
        springDataRepository.save(sugerencia);
    }

    @Override
    public void savePriceAlert(PriceAlertSuggestion suggestion) {
        AIEventEntity event = saveEvent(suggestion);
        AISuggestionEntity savedSuggestion = saveSuggestion(suggestion, event);
        suggestion.tutorUserIds().forEach(tutorUserId ->
                saveAndPublishNotification(suggestion, savedSuggestion, tutorUserId));
    }

    @Override
    public boolean hasRecentPriceAlert(UUID studentId, UUID productId, LocalDateTime since) {
        return springDataRepository
                .existsByAlumnoEntityIdAndProductoEntityIdAndTipoAndCreadoEnGreaterThanEqual(
                        studentId,
                        productId,
                        IaSuggestionType.ALERTA_PRECIO,
                        since
                );
    }

    private AIEventEntity saveEvent(PriceAlertSuggestion suggestion) {
        AIEventEntity event = new AIEventEntity();
        event.setType(AIEventType.ALERTA_PRECIO);
        event.setStudentEntity(reference(StudentEntity.class, suggestion.studentId()));
        event.setProductEntity(reference(ProductEntity.class, suggestion.productId()));
        event.setBuffetEntity(buffetReference(suggestion.buffetId()));
        event.setInputOrigin(INPUT_ORIGIN);
        event.setInputData(inputData(suggestion));
        event.setResultData(resultData(suggestion));

        AIEventEntity savedEvent = aiEventJpaRepository.save(event);
        if (savedEvent == null) {
            return event;
        }
        return savedEvent;
    }

    private AISuggestionEntity saveSuggestion(
            PriceAlertSuggestion suggestion,
            AIEventEntity event
    ) {
        AISuggestionEntity entity = new AISuggestionEntity();
        entity.setType(IaSuggestionType.ALERTA_PRECIO);
        entity.setTittle(suggestion.title());
        entity.setMessage(suggestion.message());
        entity.setStudentEntity(reference(StudentEntity.class, suggestion.studentId()));
        entity.setProductEntity(reference(ProductEntity.class, suggestion.productId()));
        entity.setBuffetEntity(buffetReference(suggestion.buffetId()));
        entity.setMetadata(priceAlertMetadata(suggestion));
        entity.setAccepted(false);
        entity.setEventoIaEntity(event);
        AISuggestionEntity savedEntity = springDataRepository.save(entity);
        if (savedEntity == null) {
            return entity;
        }
        return savedEntity;
    }

    private void saveAndPublishNotification(
            PriceAlertSuggestion suggestion,
            AISuggestionEntity savedSuggestion,
            UUID tutorUserId
    ) {
        NotificationEntity notification = new NotificationEntity();
        notification.setUserEntity(reference(UserEntity.class, tutorUserId));
        notification.setStudentEntity(reference(StudentEntity.class, suggestion.studentId()));
        notification.setProductEntity(reference(ProductEntity.class, suggestion.productId()));
        notification.setAISuggestionEntity(savedSuggestion);
        notification.setType(NotificationType.SUGERENCIA_IA);
        notification.setTittle(suggestion.title());
        notification.setMessage(suggestion.message());
        notification.setSeen(false);

        NotificationEntity savedNotification = notificationJpaRepository.save(notification);
        if (savedNotification == null) {
            savedNotification = notification;
        }
        publishPushNotification(savedNotification);
    }

    @Override
    public List<AISuggestionEntity> findByAlumnoId(UUID alumnoId) {
        return springDataRepository.findAllByAlumnoEntityIdOrderByCreadoEnDesc(alumnoId);
    }

    @Override
    public List<AISuggestionEntity> findByAlumnoIdAndTipo(UUID alumnoId, IaSuggestionType tipo) {
        return springDataRepository.findAllByAlumnoEntityIdAndTipoOrderByCreadoEnDesc(
                alumnoId, tipo);
    }

    @Override
    public java.util.Optional<AISuggestionEntity> findById(UUID id) {
        return springDataRepository.findById(id);
    }

    private BuffetEntity buffetReference(UUID buffetId) {
        if (buffetId == null) {
            return null;
        }
        return reference(BuffetEntity.class, buffetId);
    }

    private Map<String, Object> priceAlertMetadata(PriceAlertSuggestion suggestion) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.putAll(inputData(suggestion));
        metadata.putAll(resultData(suggestion));
        return metadata;
    }

    private Map<String, Object> inputData(PriceAlertSuggestion suggestion) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productoId", suggestion.productId().toString());
        data.put("productoNombre", suggestion.productName());
        data.put("precioAnterior", suggestion.oldPrice().toString());
        data.put("precioNuevo", suggestion.newPrice().toString());
        data.put("buffetId", suggestion.buffetId() == null
                ? null
                : suggestion.buffetId().toString());
        return data;
    }

    private Map<String, Object> resultData(PriceAlertSuggestion suggestion) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("titulo", suggestion.title());
        data.put("mensaje", suggestion.message());
        data.put("razonIA", suggestion.aiReason());
        return data;
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
        putIfPresent(data, "productId", notification.getProductEntity().getId());
        putIfPresent(data, "sugerenciaIaId", notification.getAISuggestionEntity().getId());
        data.put("tipo", notification.getType().name());
        return data;
    }

    private void putIfPresent(Map<String, String> data, String key, UUID value) {
        if (value != null) {
            data.put(key, value.toString());
        }
    }

    private <T> T reference(Class<T> type, UUID id) {
        return entityManager.getReference(type, id);
    }
}
