package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.HealthyProductRecommendation;
import com.recred.backend.domain.models.HealthyRecommendationMessages;
import com.recred.backend.domain.models.HealthyRecommendationPattern;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.AIEventJpaRepository;
import com.recred.backend.infrastructure.persistence.HealthyRecommendationNotificationPersistenceRepository;
import com.recred.backend.infrastructure.persistence.NotificacionJpaRepository;
import com.recred.backend.infrastructure.persistence.SugerenciaIAJpaRepository;
import com.recred.backend.infrastructure.push.PushNotificationRequestedEvent;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthyRecommendationNotificationPersistenceRepositoryTest {

    @Mock
    private SugerenciaIAJpaRepository suggestionJpaRepository;

    @Mock
    private NotificacionJpaRepository notificationJpaRepository;

    @Mock
    private AIEventJpaRepository aiEventJpaRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private HealthyRecommendationNotificationPersistenceRepository repository;

    @BeforeEach
    void setUp() {
        repository = new HealthyRecommendationNotificationPersistenceRepository(
                suggestionJpaRepository,
                notificationJpaRepository,
                aiEventJpaRepository,
                entityManager,
                eventPublisher
        );
    }

    @Test
    void saveRecommendationAsEventSuggestionNotificationAndPushEvent() {
        UUID studentId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        UUID repeatedProductId = UUID.randomUUID();
        UUID suggestedProductId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        AlumnoEntity student = new AlumnoEntity();
        UsuarioEntity studentUser = new UsuarioEntity();
        ProductoEntity suggestedProduct = new ProductoEntity();
        AIEventEntity event = new AIEventEntity();
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        student.setId(studentId);
        studentUser.setId(studentUserId);
        suggestedProduct.setId(suggestedProductId);
        suggestion.setId(UUID.randomUUID());
        when(entityManager.getReference(AlumnoEntity.class, studentId)).thenReturn(student);
        when(entityManager.getReference(UsuarioEntity.class, studentUserId))
                .thenReturn(studentUser);
        when(entityManager.getReference(ProductoEntity.class, suggestedProductId))
                .thenReturn(suggestedProduct);
        when(aiEventJpaRepository.save(any())).thenReturn(event);
        when(suggestionJpaRepository.save(any())).thenReturn(suggestion);
        when(notificationJpaRepository.save(any())).thenAnswer(invocation -> {
            NotificacionEntity notification = invocation.getArgument(0);
            notification.setId(notificationId);
            return notification;
        });

        repository.save(recommendation(
                studentId,
                studentUserId,
                repeatedProductId,
                suggestedProductId
        ));

        ArgumentCaptor<AIEventEntity> eventCaptor = ArgumentCaptor.forClass(
                AIEventEntity.class);
        verify(aiEventJpaRepository).save(eventCaptor.capture());
        AIEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AIEventType.ANALISIS_CONSUMO, savedEvent.getType());
        assertSame(student, savedEvent.getAlumnoEntity());
        assertSame(suggestedProduct, savedEvent.getProductoEntity());
        assertEquals("BACKEND_RECOMENDACION_SALUDABLE", savedEvent.getInputOrigin());
        assertEquals("Coca Cola", savedEvent.getInputData().get("productoRepetido"));
        assertEquals("Agua mineral", savedEvent.getInputData().get("productoSugerido"));
        assertEquals("Elegir agua mineral.", savedEvent.getResultData()
                .get("accionSugerida"));
        assertEquals(suggestedProductId, savedEvent.getResultData()
                .get("productoIdSugerido"));

        ArgumentCaptor<SugerenciaIAEntity> suggestionCaptor = ArgumentCaptor.forClass(
                SugerenciaIAEntity.class);
        verify(suggestionJpaRepository).save(suggestionCaptor.capture());
        SugerenciaIAEntity savedSuggestion = suggestionCaptor.getValue();
        assertEquals(TipoSugerenciaIA.RECOMENDACION_SALUDABLE, savedSuggestion.getTipo());
        assertSame(student, savedSuggestion.getAlumnoEntity());
        assertSame(suggestedProduct, savedSuggestion.getProductoEntity());
        assertSame(event, savedSuggestion.getEventoIaEntity());
        assertEquals("Proba agua", savedSuggestion.getTitulo());

        ArgumentCaptor<NotificacionEntity> notificationCaptor = ArgumentCaptor.forClass(
                NotificacionEntity.class);
        verify(notificationJpaRepository).save(notificationCaptor.capture());
        NotificacionEntity notification = notificationCaptor.getValue();
        assertSame(studentUser, notification.getUsuarioEntity());
        assertSame(student, notification.getAlumnoEntity());
        assertSame(suggestedProduct, notification.getProductoEntity());
        assertSame(suggestion, notification.getSugerenciaIaEntity());
        assertEquals(TipoNotificacion.SUGERENCIA_IA, notification.getTipo());

        ArgumentCaptor<Object> pushEventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(pushEventCaptor.capture());
        PushNotificationRequestedEvent pushEvent =
                (PushNotificationRequestedEvent) pushEventCaptor.getValue();
        assertEquals(studentUserId, pushEvent.notification().userId());
        assertEquals(notificationId.toString(), pushEvent.notification().data()
                .get("notificacionId"));
        assertEquals(suggestedProductId.toString(), pushEvent.notification().data()
                .get("productoId"));
    }

    @Test
    void checkRecommendationCooldownUsingSuggestionRepository() {
        UUID studentId = UUID.randomUUID();
        LocalDateTime since = LocalDateTime.parse("2026-05-23T00:00:00");
        when(suggestionJpaRepository.existsForStudentAndTypeSince(
                studentId,
                TipoSugerenciaIA.RECOMENDACION_SALUDABLE,
                since
        )).thenReturn(true);

        boolean exists = repository.hasRecommendationSince(studentId, since);

        assertEquals(true, exists);
    }

    @Test
    void publishNotificationWithFallbacksWhenRepositoriesReturnNull() {
        UUID studentId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        UUID repeatedProductId = UUID.randomUUID();
        AlumnoEntity student = new AlumnoEntity();
        UsuarioEntity studentUser = new UsuarioEntity();
        student.setId(studentId);
        studentUser.setId(studentUserId);
        when(entityManager.getReference(AlumnoEntity.class, studentId)).thenReturn(student);
        when(entityManager.getReference(UsuarioEntity.class, studentUserId))
                .thenReturn(studentUser);
        when(aiEventJpaRepository.save(any())).thenReturn(null);
        when(suggestionJpaRepository.save(any())).thenReturn(null);
        when(notificationJpaRepository.save(any())).thenReturn(null);

        repository.save(recommendation(studentId, studentUserId, repeatedProductId, null));

        ArgumentCaptor<AIEventEntity> eventCaptor = ArgumentCaptor.forClass(
                AIEventEntity.class);
        ArgumentCaptor<SugerenciaIAEntity> suggestionCaptor = ArgumentCaptor.forClass(
                SugerenciaIAEntity.class);
        verify(aiEventJpaRepository).save(eventCaptor.capture());
        verify(suggestionJpaRepository).save(suggestionCaptor.capture());
        assertSame(eventCaptor.getValue(), suggestionCaptor.getValue().getEventoIaEntity());

        ArgumentCaptor<Object> pushEventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(pushEventCaptor.capture());
        PushNotificationRequestedEvent pushEvent =
                (PushNotificationRequestedEvent) pushEventCaptor.getValue();
        assertEquals(studentUserId, pushEvent.notification().userId());
        assertFalse(pushEvent.notification().data().containsKey("productoId"));
        assertFalse(pushEvent.notification().data().containsKey("sugerenciaIaId"));
    }

    private HealthyProductRecommendation recommendation(
            UUID studentId,
            UUID studentUserId,
            UUID repeatedProductId,
            UUID suggestedProductId
    ) {
        return new HealthyProductRecommendation(
                new ConsumptionAlertCandidate(studentId, studentUserId, "Juan Perez", List.of()),
                new HealthyRecommendationPattern(
                        studentId,
                        LocalDate.of(2026, 5, 26),
                        LocalDate.of(2026, 5, 24),
                        LocalDate.of(2026, 5, 25),
                        repeatedProductId,
                        "Coca Cola",
                        "Bebidas",
                        UUID.randomUUID(),
                        2,
                        2L,
                        suggestedProductId,
                        "Agua mineral",
                        "Bebidas"
                ),
                new HealthyRecommendationMessages(
                        "Proba agua",
                        "Elegiste gaseosa estos dias. Hoy podes probar agua mineral.",
                        "Elegir agua mineral.",
                        suggestedProductId,
                        new BigDecimal("0.8500"),
                        "gemini-test"
                )
        );
    }
}
