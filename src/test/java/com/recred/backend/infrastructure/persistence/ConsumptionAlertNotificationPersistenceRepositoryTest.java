package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.models.ConsumptionAlert;
import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.ConsumptionAlertMessages;
import com.recred.backend.domain.models.ConsumptionAlertPattern;
import com.recred.backend.domain.models.ConsumptionAlertTutor;
import com.recred.backend.domain.models.ConsumptionAlertType;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumptionAlertNotificationPersistenceRepositoryTest {

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

    private ConsumptionAlertNotificationPersistenceRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ConsumptionAlertNotificationPersistenceRepository(
                suggestionJpaRepository,
                notificationJpaRepository,
                aiEventJpaRepository,
                entityManager,
                eventPublisher
        );
    }

    @Test
    void savePatternAlertAsEventSuggestionAndNotifications() {
        UUID studentId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        UUID tutorUserId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        StudentEntity student = new StudentEntity();
        UserEntity studentUser = new UserEntity();
        UserEntity tutorUser = new UserEntity();
        ProductEntity product = new ProductEntity();
        AIEventEntity event = new AIEventEntity();
        AISuggestionEntity suggestion = new AISuggestionEntity();
        studentUser.setId(studentUserId);
        tutorUser.setId(tutorUserId);
        product.setId(productId);
        suggestion.setId(UUID.randomUUID());
        when(entityManager.getReference(StudentEntity.class, studentId)).thenReturn(student);
        when(entityManager.getReference(UserEntity.class, studentUserId))
                .thenReturn(studentUser);
        when(entityManager.getReference(UserEntity.class, tutorUserId)).thenReturn(tutorUser);
        when(entityManager.getReference(ProductEntity.class, productId)).thenReturn(product);
        when(aiEventJpaRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenReturn(event);
        when(suggestionJpaRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenReturn(suggestion);

        repository.save(alert(studentId, studentUserId, tutorUserId, productId));

        ArgumentCaptor<AIEventEntity> eventCaptor = ArgumentCaptor.forClass(
                AIEventEntity.class);
        verify(aiEventJpaRepository).save(eventCaptor.capture());
        AIEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AIEventType.ANALISIS_CONSUMO, savedEvent.getType());
        assertSame(student, savedEvent.getStudentEntity());
        assertSame(product, savedEvent.getProductEntity());
        assertEquals("BACKEND_ALERTA_ESTADISTICA_CONSUMO", savedEvent.getInputOrigin());
        assertEquals(new BigDecimal("0.8000"), savedEvent.getConfidence());
        assertEquals("STATISTICAL_PATTERN", savedEvent.getInputData().get("tipo"));
        assertEquals("Tomar mas agua.", savedEvent.getResultData().get("accionSugerida"));

        ArgumentCaptor<AISuggestionEntity> suggestionCaptor = ArgumentCaptor.forClass(
                AISuggestionEntity.class);
        verify(suggestionJpaRepository).save(suggestionCaptor.capture());
        AISuggestionEntity savedSuggestion = suggestionCaptor.getValue();
        assertEquals(IaSuggestionType.RESUMEN_CONSUMO, savedSuggestion.getType());
        assertSame(student, savedSuggestion.getStudentEntity());
        assertSame(product, savedSuggestion.getProductEntity());
        assertSame(event, savedSuggestion.getEventoIaEntity());
        assertEquals("Elegir agua", savedSuggestion.getTittle());
        assertEquals("Te recomendamos tomar mas agua.", savedSuggestion.getMessage());
        assertEquals("Bebidas", savedSuggestion.getMetadata().get("categoriaPrincipal"));

        ArgumentCaptor<NotificationEntity> notificationCaptor = ArgumentCaptor.forClass(
                NotificationEntity.class);
        verify(notificationJpaRepository, org.mockito.Mockito.times(2))
                .save(notificationCaptor.capture());
        List<NotificationEntity> notifications = notificationCaptor.getAllValues();
        assertSame(studentUser, notifications.get(0).getUserEntity());
        assertSame(tutorUser, notifications.get(1).getUserEntity());
        assertEquals(NotificationType.SUGERENCIA_IA, notifications.get(0).getType());
        assertSame(suggestion, notifications.get(0).getAISuggestionEntity());
        assertEquals("Sugerencia para Juan", notifications.get(1).getTittle());
        verify(eventPublisher, times(2)).publishEvent(
                org.mockito.ArgumentMatchers.any(Object.class)
        );
    }

    @Test
    void saveHealthyRecommendationWithoutProductReference() {
        UUID studentId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        StudentEntity student = new StudentEntity();
        UserEntity studentUser = new UserEntity();
        AIEventEntity event = new AIEventEntity();
        AISuggestionEntity suggestion = new AISuggestionEntity();
        studentUser.setId(studentUserId);
        when(entityManager.getReference(StudentEntity.class, studentId)).thenReturn(student);
        when(entityManager.getReference(UserEntity.class, studentUserId))
                .thenReturn(studentUser);
        when(aiEventJpaRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenReturn(event);
        when(suggestionJpaRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenReturn(suggestion);

        repository.save(healthyAlert(studentId, studentUserId));

        ArgumentCaptor<AISuggestionEntity> suggestionCaptor = ArgumentCaptor.forClass(
                AISuggestionEntity.class);
        verify(suggestionJpaRepository).save(suggestionCaptor.capture());
        assertEquals(IaSuggestionType.RECOMENDACION_SALUDABLE, suggestionCaptor.getValue()
                .getType());
        assertNull(suggestionCaptor.getValue().getProductEntity());
    }

    @Test
    void publishNotificationWithoutSuggestionWhenSuggestionSaveReturnsNull() {
        UUID studentId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        StudentEntity student = new StudentEntity();
        UserEntity studentUser = new UserEntity();
        AIEventEntity event = new AIEventEntity();
        student.setId(studentId);
        studentUser.setId(studentUserId);
        when(entityManager.getReference(StudentEntity.class, studentId)).thenReturn(student);
        when(entityManager.getReference(UserEntity.class, studentUserId))
                .thenReturn(studentUser);
        when(aiEventJpaRepository.save(any())).thenReturn(event);
        when(suggestionJpaRepository.save(any())).thenReturn(null);
        when(notificationJpaRepository.save(any())).thenAnswer(invocation -> {
            NotificationEntity notification = invocation.getArgument(0);
            notification.setId(notificationId);
            return notification;
        });

        repository.save(healthyAlert(studentId, studentUserId));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PushNotificationRequestedEvent eventPublished =
                (PushNotificationRequestedEvent) eventCaptor.getValue();
        assertEquals(studentUserId, eventPublished.notification().userId());
        assertEquals(
                notificationId.toString(),
                eventPublished.notification().data().get("notificacionId")
        );
        assertEquals(studentId.toString(), eventPublished.notification().data().get("studentId"));
        assertEquals(
                NotificationType.SUGERENCIA_IA.name(),
                eventPublished.notification().data().get("tipo")
        );
        assertFalse(eventPublished.notification().data().containsKey("sugerenciaIaId"));
    }

    @Test
    void linkSuggestionWithOriginalEventWhenSaveReturnsNull() {
        UUID studentId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        StudentEntity student = new StudentEntity();
        UserEntity studentUser = new UserEntity();
        AISuggestionEntity suggestion = new AISuggestionEntity();
        studentUser.setId(studentUserId);
        when(entityManager.getReference(StudentEntity.class, studentId)).thenReturn(student);
        when(entityManager.getReference(UserEntity.class, studentUserId))
                .thenReturn(studentUser);
        when(aiEventJpaRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenReturn(null);
        when(suggestionJpaRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenReturn(suggestion);

        repository.save(healthyAlert(studentId, studentUserId));

        ArgumentCaptor<AIEventEntity> eventCaptor = ArgumentCaptor.forClass(
                AIEventEntity.class);
        ArgumentCaptor<AISuggestionEntity> suggestionCaptor = ArgumentCaptor.forClass(
                AISuggestionEntity.class);
        verify(aiEventJpaRepository).save(eventCaptor.capture());
        verify(suggestionJpaRepository).save(suggestionCaptor.capture());
        assertSame(eventCaptor.getValue(), suggestionCaptor.getValue().getEventoIaEntity());
    }

    @Test
    void checkExistingAlertsUsingSuggestionRepository() {
        UUID studentId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.parse("2026-05-25T00:00:00");
        LocalDateTime end = LocalDateTime.parse("2026-05-25T23:59:00");
        when(suggestionJpaRepository.existsAnyForStudentBetween(studentId, start, end))
                .thenReturn(true);
        when(suggestionJpaRepository.existsForStudentAndTypeSince(
                studentId,
                IaSuggestionType.RESUMEN_CONSUMO,
                start
        )).thenReturn(true);

        boolean hasAny = repository.hasAnyAlertBetween(studentId, start, end);
        boolean hasPattern = repository.hasAlertSince(
                studentId,
                ConsumptionAlertType.STATISTICAL_PATTERN,
                start
        );

        assertEquals(true, hasAny);
        assertEquals(true, hasPattern);
    }

    private ConsumptionAlert alert(
            UUID studentId,
            UUID studentUserId,
            UUID tutorUserId,
            UUID productId
    ) {
        return new ConsumptionAlert(
                new ConsumptionAlertCandidate(
                        studentId,
                        studentUserId,
                        "Juan Perez",
                        List.of(new ConsumptionAlertTutor(tutorUserId, "Tutor Perez"))
                ),
                pattern(studentId, productId, ConsumptionAlertType.STATISTICAL_PATTERN),
                messages()
        );
    }

    private ConsumptionAlert healthyAlert(UUID studentId, UUID studentUserId) {
        return new ConsumptionAlert(
                new ConsumptionAlertCandidate(studentId, studentUserId, "Juan Perez", List.of()),
                pattern(studentId, null, ConsumptionAlertType.HEALTHY_RECOMMENDATION),
                messages()
        );
    }

    private ConsumptionAlertPattern pattern(
            UUID studentId,
            UUID productId,
            ConsumptionAlertType type
    ) {
        return new ConsumptionAlertPattern(
                type,
                studentId,
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 4, 26),
                LocalDate.of(2026, 5, 25),
                4,
                new BigDecimal("100.00"),
                "Bebidas",
                new BigDecimal("80.00"),
                productId,
                productId == null ? null : "Agua"
        );
    }

    private ConsumptionAlertMessages messages() {
        return new ConsumptionAlertMessages(
                "Elegir agua",
                "Sugerencia para Juan",
                "Te recomendamos tomar mas agua.",
                "Recomendamos sugerir a Juan que tome mas agua.",
                "Tomar mas agua.",
                new BigDecimal("0.8000"),
                "gemini-test"
        );
    }
}
