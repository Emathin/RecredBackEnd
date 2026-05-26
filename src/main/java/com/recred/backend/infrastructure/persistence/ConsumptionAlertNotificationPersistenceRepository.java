package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ConsumptionAlert;
import com.recred.backend.domain.models.ConsumptionAlertMessages;
import com.recred.backend.domain.models.ConsumptionAlertPattern;
import com.recred.backend.domain.models.ConsumptionAlertTutor;
import com.recred.backend.domain.models.ConsumptionAlertType;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.repositories.ConsumptionAlertNotificationRepository;
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
        SugerenciaIAEntity suggestion = saveSuggestion(alert, event);
        saveStudentNotification(alert, suggestion);
        alert.candidate().tutors()
                .forEach(tutor -> saveTutorNotification(alert, tutor, suggestion));
    }

    private AIEventEntity saveEvent(ConsumptionAlert alert) {
        ConsumptionAlertPattern pattern = alert.pattern();
        AIEventEntity event = new AIEventEntity();
        event.setType(AIEventType.ANALISIS_CONSUMO);
        event.setAlumnoEntity(studentReference(pattern.studentId()));
        event.setProductoEntity(productReference(pattern.suggestedProductId()));
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

    private SugerenciaIAEntity saveSuggestion(
            ConsumptionAlert alert,
            AIEventEntity event
    ) {
        ConsumptionAlertPattern pattern = alert.pattern();
        ConsumptionAlertMessages messages = alert.messages();
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setTipo(suggestionType(pattern.type()));
        suggestion.setAlumnoEntity(studentReference(pattern.studentId()));
        suggestion.setProductoEntity(productReference(pattern.suggestedProductId()));
        suggestion.setTitulo(messages.title());
        suggestion.setMensaje(messages.studentMessage());
        suggestion.setMetadata(metadata(alert));
        suggestion.setAceptada(false);
        suggestion.setEventoIaEntity(event);
        return suggestionJpaRepository.save(suggestion);
    }

    private void saveStudentNotification(
            ConsumptionAlert alert,
            SugerenciaIAEntity suggestion
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
            SugerenciaIAEntity suggestion
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

    private void saveAndPublishNotification(NotificacionEntity notification) {
        NotificacionEntity savedNotification = notificationJpaRepository.save(notification);
        if (savedNotification == null) {
            savedNotification = notification;
        }
        publishPushNotification(savedNotification);
    }

    private NotificacionEntity notification(
            UUID userId,
            UUID studentId,
            UUID productId,
            SugerenciaIAEntity suggestion,
            String title,
            String message
    ) {
        NotificacionEntity notification = new NotificacionEntity();
        notification.setUsuarioEntity(reference(UsuarioEntity.class, userId));
        notification.setAlumnoEntity(studentReference(studentId));
        notification.setProductoEntity(productReference(productId));
        notification.setSugerenciaIaEntity(suggestion);
        notification.setTipo(TipoNotificacion.SUGERENCIA_IA);
        notification.setTitulo(title);
        notification.setMensaje(message);
        notification.setLeida(false);
        return notification;
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

    private TipoSugerenciaIA suggestionType(ConsumptionAlertType alertType) {
        if (alertType == ConsumptionAlertType.STATISTICAL_PATTERN) {
            return TipoSugerenciaIA.RESUMEN_CONSUMO;
        }
        return TipoSugerenciaIA.RECOMENDACION_SALUDABLE;
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
