package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.models.PriceAlertSuggestion;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
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
public class SugerenciaIARepositoryImpl implements SugerenciaIARepository {

    private static final String INPUT_ORIGIN = "BACKEND_ALERTA_PRECIO";

    private final SpringDataSugerenciaIARepository springDataRepository;
    private final AIEventJpaRepository aiEventJpaRepository;
    private final NotificacionJpaRepository notificationJpaRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    public SugerenciaIARepositoryImpl(
            SpringDataSugerenciaIARepository springDataRepository,
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
    public void guardar(SugerenciaIAEntity sugerencia) {
        springDataRepository.save(sugerencia);
    }

    @Override
    public void savePriceAlert(PriceAlertSuggestion suggestion) {
        AIEventEntity event = saveEvent(suggestion);
        SugerenciaIAEntity savedSuggestion = saveSuggestion(suggestion, event);
        suggestion.tutorUserIds().forEach(tutorUserId ->
                saveAndPublishNotification(suggestion, savedSuggestion, tutorUserId));
    }

    @Override
    public boolean hasRecentPriceAlert(UUID studentId, UUID productId, LocalDateTime since) {
        return springDataRepository
                .existsByAlumnoEntityIdAndProductoEntityIdAndTipoAndCreadoEnGreaterThanEqual(
                        studentId,
                        productId,
                        TipoSugerenciaIA.ALERTA_PRECIO,
                        since
                );
    }

    private AIEventEntity saveEvent(PriceAlertSuggestion suggestion) {
        AIEventEntity event = new AIEventEntity();
        event.setType(AIEventType.ALERTA_PRECIO);
        event.setAlumnoEntity(reference(AlumnoEntity.class, suggestion.studentId()));
        event.setProductoEntity(reference(ProductoEntity.class, suggestion.productId()));
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

    private SugerenciaIAEntity saveSuggestion(
            PriceAlertSuggestion suggestion,
            AIEventEntity event
    ) {
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setTipo(TipoSugerenciaIA.ALERTA_PRECIO);
        entity.setTitulo(suggestion.title());
        entity.setMensaje(suggestion.message());
        entity.setAlumnoEntity(reference(AlumnoEntity.class, suggestion.studentId()));
        entity.setProductoEntity(reference(ProductoEntity.class, suggestion.productId()));
        entity.setBuffetEntity(buffetReference(suggestion.buffetId()));
        entity.setMetadata(priceAlertMetadata(suggestion));
        entity.setAceptada(false);
        entity.setEventoIaEntity(event);
        SugerenciaIAEntity savedEntity = springDataRepository.save(entity);
        if (savedEntity == null) {
            return entity;
        }
        return savedEntity;
    }

    private void saveAndPublishNotification(
            PriceAlertSuggestion suggestion,
            SugerenciaIAEntity savedSuggestion,
            UUID tutorUserId
    ) {
        NotificacionEntity notification = new NotificacionEntity();
        notification.setUsuarioEntity(reference(UsuarioEntity.class, tutorUserId));
        notification.setAlumnoEntity(reference(AlumnoEntity.class, suggestion.studentId()));
        notification.setProductoEntity(reference(ProductoEntity.class, suggestion.productId()));
        notification.setSugerenciaIaEntity(savedSuggestion);
        notification.setTipo(TipoNotificacion.SUGERENCIA_IA);
        notification.setTitulo(suggestion.title());
        notification.setMensaje(suggestion.message());
        notification.setLeida(false);

        NotificacionEntity savedNotification = notificationJpaRepository.save(notification);
        if (savedNotification == null) {
            savedNotification = notification;
        }
        publishPushNotification(savedNotification);
    }

    @Override
    public List<SugerenciaIAEntity> findByAlumnoId(UUID alumnoId) {
        return springDataRepository.findAllByAlumnoEntityIdOrderByCreadoEnDesc(alumnoId);
    }

    @Override
    public List<SugerenciaIAEntity> findByAlumnoIdAndTipo(UUID alumnoId, TipoSugerenciaIA tipo) {
        return springDataRepository.findAllByAlumnoEntityIdAndTipoOrderByCreadoEnDesc(
                alumnoId, tipo);
    }

    @Override
    public java.util.Optional<SugerenciaIAEntity> findById(UUID id) {
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

    private <T> T reference(Class<T> type, UUID id) {
        return entityManager.getReference(type, id);
    }
}
