package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.HealthyProductRecommendation;
import com.recred.backend.domain.models.HealthyRecommendationMessages;
import com.recred.backend.domain.models.HealthyRecommendationPattern;
import com.recred.backend.domain.models.NotificationType;
import com.recred.backend.domain.models.IaSuggestionType;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.NotificationEntity;
import com.recred.backend.infrastructure.entities.ProductEntity;
import com.recred.backend.infrastructure.entities.AISuggestionEntity;
import com.recred.backend.infrastructure.entities.UserEntity;
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
        StudentEntity student = new StudentEntity();
        UserEntity studentUser = new UserEntity();
        ProductEntity suggestedProduct = new ProductEntity();
        AIEventEntity event = new AIEventEntity();
        AISuggestionEntity suggestion = new AISuggestionEntity();
        student.setId(studentId);
        studentUser.setId(studentUserId);
        suggestedProduct.setId(suggestedProductId);
        suggestion.setId(UUID.randomUUID());
        when(entityManager.getReference(StudentEntity.class, studentId)).thenReturn(student);
        when(entityManager.getReference(UserEntity.class, studentUserId))
                .thenReturn(studentUser);
        when(entityManager.getReference(ProductEntity.class, suggestedProductId))
                .thenReturn(suggestedProduct);
        when(aiEventJpaRepository.save(any())).thenReturn(event);
        when(suggestionJpaRepository.save(any())).thenReturn(suggestion);
        when(notificationJpaRepository.save(any())).thenAnswer(invocation -> {
            NotificationEntity notification = invocation.getArgument(0);
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
        assertSame(student, savedEvent.getStudentEntity());
        assertSame(suggestedProduct, savedEvent.getProductEntity());
        assertEquals("BACKEND_RECOMENDACION_SALUDABLE", savedEvent.getInputOrigin());
        assertEquals("Coca Cola", savedEvent.getInputData().get("productoRepetido"));
        assertEquals("Agua mineral", savedEvent.getInputData().get("productoSugerido"));
        assertEquals("Elegir agua mineral.", savedEvent.getResultData()
                .get("accionSugerida"));
        assertEquals(suggestedProductId, savedEvent.getResultData()
                .get("productoIdSugerido"));

        ArgumentCaptor<AISuggestionEntity> suggestionCaptor = ArgumentCaptor.forClass(
                AISuggestionEntity.class);
        verify(suggestionJpaRepository).save(suggestionCaptor.capture());
        AISuggestionEntity savedSuggestion = suggestionCaptor.getValue();
        assertEquals(IaSuggestionType.RECOMENDACION_SALUDABLE, savedSuggestion.getType());
        assertSame(student, savedSuggestion.getStudentEntity());
        assertSame(suggestedProduct, savedSuggestion.getProductEntity());
        assertSame(event, savedSuggestion.getEventoIaEntity());
        assertEquals("Proba agua", savedSuggestion.getTittle());

        ArgumentCaptor<NotificationEntity> notificationCaptor = ArgumentCaptor.forClass(
                NotificationEntity.class);
        verify(notificationJpaRepository).save(notificationCaptor.capture());
        NotificationEntity notification = notificationCaptor.getValue();
        assertSame(studentUser, notification.getUserEntity());
        assertSame(student, notification.getStudentEntity());
        assertSame(suggestedProduct, notification.getProductEntity());
        assertSame(suggestion, notification.getAISuggestionEntity());
        assertEquals(NotificationType.SUGERENCIA_IA, notification.getType());

        ArgumentCaptor<Object> pushEventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(pushEventCaptor.capture());
        PushNotificationRequestedEvent pushEvent =
                (PushNotificationRequestedEvent) pushEventCaptor.getValue();
        assertEquals(studentUserId, pushEvent.notification().userId());
        assertEquals(notificationId.toString(), pushEvent.notification().data()
                .get("notificacionId"));
        assertEquals(suggestedProductId.toString(), pushEvent.notification().data()
                .get("productId"));
    }

    @Test
    void checkRecommendationCooldownUsingSuggestionRepository() {
        UUID studentId = UUID.randomUUID();
        LocalDateTime since = LocalDateTime.parse("2026-05-23T00:00:00");
        when(suggestionJpaRepository.existsForStudentAndTypeSince(
                studentId,
                IaSuggestionType.RECOMENDACION_SALUDABLE,
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
        StudentEntity student = new StudentEntity();
        UserEntity studentUser = new UserEntity();
        student.setId(studentId);
        studentUser.setId(studentUserId);
        when(entityManager.getReference(StudentEntity.class, studentId)).thenReturn(student);
        when(entityManager.getReference(UserEntity.class, studentUserId))
                .thenReturn(studentUser);
        when(aiEventJpaRepository.save(any())).thenReturn(null);
        when(suggestionJpaRepository.save(any())).thenReturn(null);
        when(notificationJpaRepository.save(any())).thenReturn(null);

        repository.save(recommendation(studentId, studentUserId, repeatedProductId, null));

        ArgumentCaptor<AIEventEntity> eventCaptor = ArgumentCaptor.forClass(
                AIEventEntity.class);
        ArgumentCaptor<AISuggestionEntity> suggestionCaptor = ArgumentCaptor.forClass(
                AISuggestionEntity.class);
        verify(aiEventJpaRepository).save(eventCaptor.capture());
        verify(suggestionJpaRepository).save(suggestionCaptor.capture());
        assertSame(eventCaptor.getValue(), suggestionCaptor.getValue().getEventoIaEntity());

        ArgumentCaptor<Object> pushEventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(pushEventCaptor.capture());
        PushNotificationRequestedEvent pushEvent =
                (PushNotificationRequestedEvent) pushEventCaptor.getValue();
        assertEquals(studentUserId, pushEvent.notification().userId());
        assertFalse(pushEvent.notification().data().containsKey("productId"));
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
