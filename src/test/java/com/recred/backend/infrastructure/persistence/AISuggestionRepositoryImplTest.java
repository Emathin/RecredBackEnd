package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.models.PriceAlertSuggestion;
import com.recred.backend.domain.models.NotificationType;
import com.recred.backend.domain.models.IaSuggestionType;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.NotificationEntity;
import com.recred.backend.infrastructure.entities.ProductEntity;
import com.recred.backend.infrastructure.entities.AISuggestionEntity;
import com.recred.backend.infrastructure.entities.UserEntity;
import com.recred.backend.infrastructure.push.PushNotificationRequestedEvent;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AISuggestionRepositoryImplTest {

    @Mock
    private SugerenciaIAJpaRepository springDataRepository;
    @Mock
    private AIEventJpaRepository aiEventJpaRepository;
    @Mock
    private NotificacionJpaRepository notificationJpaRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AISuggestionRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new AISuggestionRepositoryImpl(
                springDataRepository,
                aiEventJpaRepository,
                notificationJpaRepository,
                entityManager,
                eventPublisher
        );
    }

    @Test
    void delegateSaveToSpringDataRepository() {
        AISuggestionEntity entity = new AISuggestionEntity();
        repository.save(entity);
        verify(springDataRepository).save(entity);
    }

    @Test
    void delegateFindByAlumnoIdToSpringDataRepository() {
        UUID alumnoId = UUID.randomUUID();
        List<AISuggestionEntity> list = List.of(new AISuggestionEntity());
        when(springDataRepository.findAllByAlumnoEntityIdOrderByCreadoEnDesc(alumnoId))
                .thenReturn(list);

        List<AISuggestionEntity> result = repository.findByAlumnoId(alumnoId);

        assertEquals(list, result);
    }

    @Test
    void savePriceAlertPersistsAiEvent() {
        PriceAlertContext context = savePriceAlert();
        ArgumentCaptor<AIEventEntity> eventCaptor = ArgumentCaptor.forClass(AIEventEntity.class);

        verify(aiEventJpaRepository).save(eventCaptor.capture());
        AIEventEntity event = eventCaptor.getValue();

        assertEquals(AIEventType.ALERTA_PRECIO, event.getType());
        assertSame(context.student(), event.getStudentEntity());
        assertSame(context.product(), event.getProductEntity());
        assertSame(context.buffet(), event.getBuffetEntity());
        assertEquals("100.00", event.getInputData().get("precioAnterior"));
        assertEquals("Cambio del 20%", event.getResultData().get("razonIA"));
    }

    @Test
    void savePriceAlertPersistsSuggestion() {
        PriceAlertContext context = savePriceAlert();

        ArgumentCaptor<AISuggestionEntity> suggestionCaptor =
                ArgumentCaptor.forClass(AISuggestionEntity.class);
        verify(springDataRepository).save(suggestionCaptor.capture());
        AISuggestionEntity saved = suggestionCaptor.getValue();

        assertEquals(IaSuggestionType.ALERTA_PRECIO, saved.getType());
        assertEquals("Ajuste de price", saved.getTittle());
        assertEquals("El producto cambio de price", saved.getMessage());
        assertSame(context.student(), saved.getStudentEntity());
        assertSame(context.product(), saved.getProductEntity());
        assertSame(context.buffet(), saved.getBuffetEntity());
        assertEquals(AIEventType.ALERTA_PRECIO, saved.getEventoIaEntity().getType());
        assertEquals(Boolean.FALSE, saved.getAccepted());
        assertEquals("120.00", saved.getMetadata().get("precioNuevo"));
    }

    @Test
    void savePriceAlertPersistsTutorNotification() {
        PriceAlertContext context = savePriceAlert();

        ArgumentCaptor<NotificationEntity> notificationCaptor =
                ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationJpaRepository).save(notificationCaptor.capture());
        NotificationEntity notification = notificationCaptor.getValue();

        assertSame(context.tutorUser(), notification.getUserEntity());
        assertEquals(IaSuggestionType.ALERTA_PRECIO,
                notification.getAISuggestionEntity().getType());
        assertEquals(NotificationType.SUGERENCIA_IA, notification.getType());
    }

    @Test
    void savePriceAlertPublishesPushNotification() {
        PriceAlertContext context = savePriceAlert();

        ArgumentCaptor<PushNotificationRequestedEvent> pushCaptor =
                ArgumentCaptor.forClass(PushNotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(pushCaptor.capture());
        assertEquals(context.tutorUserId(), pushCaptor.getValue().notification().userId());
    }

    @Test
    void delegateRecentPriceAlertLookupToSpringDataRepository() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDateTime since = LocalDateTime.now();
        when(springDataRepository
                .existsByAlumnoEntityIdAndProductoEntityIdAndTipoAndCreadoEnGreaterThanEqual(
                        studentId,
                        productId,
                        IaSuggestionType.ALERTA_PRECIO,
                        since
                ))
                .thenReturn(true);

        boolean result = repository.hasRecentPriceAlert(studentId, productId, since);

        assertTrue(result);
    }

    @Test
    void delegateFindByIdToSpringDataRepository() {
        UUID id = UUID.randomUUID();
        AISuggestionEntity entity = new AISuggestionEntity();
        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<AISuggestionEntity> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
    }

    private PriceAlertContext savePriceAlert() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID tutorUserId = UUID.randomUUID();
        StudentEntity student = new StudentEntity();
        ProductEntity product = new ProductEntity();
        BuffetEntity buffet = new BuffetEntity();
        UserEntity tutorUser = new UserEntity();
        tutorUser.setId(tutorUserId);

        when(entityManager.getReference(StudentEntity.class, studentId)).thenReturn(student);
        when(entityManager.getReference(ProductEntity.class, productId)).thenReturn(product);
        when(entityManager.getReference(BuffetEntity.class, buffetId)).thenReturn(buffet);
        when(entityManager.getReference(UserEntity.class, tutorUserId)).thenReturn(tutorUser);

        repository.savePriceAlert(suggestion(studentId, productId, buffetId, tutorUserId));

        return new PriceAlertContext(student, product, buffet, tutorUser, tutorUserId);
    }

    private PriceAlertSuggestion suggestion(
            UUID studentId,
            UUID productId,
            UUID buffetId,
            UUID tutorUserId
    ) {
        return new PriceAlertSuggestion(
                studentId,
                productId,
                buffetId,
                Set.of(tutorUserId),
                "Alfajor",
                "Ajuste de price",
                "El producto cambio de price",
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                "Cambio del 20%"
        );
    }

    private record PriceAlertContext(
            StudentEntity student,
            ProductEntity product,
            BuffetEntity buffet,
            UserEntity tutorUser,
            UUID tutorUserId
    ) {
    }
}
