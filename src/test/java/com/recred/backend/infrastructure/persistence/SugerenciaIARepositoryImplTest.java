package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.models.PriceAlertSuggestion;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SugerenciaIARepositoryImplTest {

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

    private SugerenciaIARepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new SugerenciaIARepositoryImpl(
                springDataRepository,
                aiEventJpaRepository,
                notificationJpaRepository,
                entityManager,
                eventPublisher
        );
    }

    @Test
    void delegateSaveToSpringDataRepository() {
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        repository.guardar(entity);
        verify(springDataRepository).save(entity);
    }

    @Test
    void delegateFindByAlumnoIdToSpringDataRepository() {
        UUID alumnoId = UUID.randomUUID();
        List<SugerenciaIAEntity> list = List.of(new SugerenciaIAEntity());
        when(springDataRepository.findAllByAlumnoEntityIdOrderByCreadoEnDesc(alumnoId))
                .thenReturn(list);

        List<SugerenciaIAEntity> result = repository.findByAlumnoId(alumnoId);

        assertEquals(list, result);
    }

    @Test
    void savePriceAlertPersistsAiEvent() {
        PriceAlertContext context = savePriceAlert();
        ArgumentCaptor<AIEventEntity> eventCaptor = ArgumentCaptor.forClass(AIEventEntity.class);

        verify(aiEventJpaRepository).save(eventCaptor.capture());
        AIEventEntity event = eventCaptor.getValue();

        assertEquals(AIEventType.ALERTA_PRECIO, event.getType());
        assertSame(context.student(), event.getAlumnoEntity());
        assertSame(context.product(), event.getProductoEntity());
        assertSame(context.buffet(), event.getBuffetEntity());
        assertEquals("100.00", event.getInputData().get("precioAnterior"));
        assertEquals("Cambio del 20%", event.getResultData().get("razonIA"));
    }

    @Test
    void savePriceAlertPersistsSuggestion() {
        PriceAlertContext context = savePriceAlert();

        ArgumentCaptor<SugerenciaIAEntity> suggestionCaptor =
                ArgumentCaptor.forClass(SugerenciaIAEntity.class);
        verify(springDataRepository).save(suggestionCaptor.capture());
        SugerenciaIAEntity saved = suggestionCaptor.getValue();

        assertEquals(TipoSugerenciaIA.ALERTA_PRECIO, saved.getTipo());
        assertEquals("Ajuste de precio", saved.getTitulo());
        assertEquals("El producto cambio de precio", saved.getMensaje());
        assertSame(context.student(), saved.getAlumnoEntity());
        assertSame(context.product(), saved.getProductoEntity());
        assertSame(context.buffet(), saved.getBuffetEntity());
        assertEquals(AIEventType.ALERTA_PRECIO, saved.getEventoIaEntity().getType());
        assertEquals(Boolean.FALSE, saved.getAceptada());
        assertEquals("120.00", saved.getMetadata().get("precioNuevo"));
        assertEquals(context.studentUserId().toString(), saved.getMetadata().get("alumnoUsuarioId"));
    }

    @Test
    void savePriceAlertPersistsTutorNotification() {
        PriceAlertContext context = savePriceAlert();

        ArgumentCaptor<NotificacionEntity> notificationCaptor =
                ArgumentCaptor.forClass(NotificacionEntity.class);
        verify(notificationJpaRepository).save(notificationCaptor.capture());
        NotificacionEntity notification = notificationCaptor.getValue();

        assertSame(context.tutorUser(), notification.getUsuarioEntity());
        assertEquals(TipoSugerenciaIA.ALERTA_PRECIO,
                notification.getSugerenciaIaEntity().getTipo());
        assertEquals(TipoNotificacion.SUGERENCIA_IA, notification.getTipo());
    }

    @Test
    void savePriceAlertPublishesPushNotification() {
        PriceAlertContext context = savePriceAlert();

        ArgumentCaptor<PushNotificationRequestedEvent> pushCaptor =
                ArgumentCaptor.forClass(PushNotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(pushCaptor.capture());
        assertEquals(context.tutorUserId(), pushCaptor.getValue().notification().userId());
        assertEquals(
                context.studentUserId().toString(),
                pushCaptor.getValue().notification().data().get("alumnoUsuarioId")
        );
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
                        TipoSugerenciaIA.ALERTA_PRECIO,
                        since
                ))
                .thenReturn(true);

        boolean result = repository.hasRecentPriceAlert(studentId, productId, since);

        assertTrue(result);
    }

    @Test
    void delegateFindByIdToSpringDataRepository() {
        UUID id = UUID.randomUUID();
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<SugerenciaIAEntity> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
    }

    @Test
    void savePriceAlert_WhenRepositoriesReturnNull_ShouldUseCreatedEntities() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID tutorUserId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        AlumnoEntity student = new AlumnoEntity();
        student.setId(studentId);
        ProductoEntity product = new ProductoEntity();
        product.setId(productId);
        UsuarioEntity tutorUser = new UsuarioEntity();
        tutorUser.setId(tutorUserId);
        when(entityManager.getReference(AlumnoEntity.class, studentId)).thenReturn(student);
        when(entityManager.getReference(ProductoEntity.class, productId)).thenReturn(product);
        when(entityManager.getReference(UsuarioEntity.class, tutorUserId)).thenReturn(tutorUser);
        when(aiEventJpaRepository.save(any(AIEventEntity.class)))
                .thenReturn(null);
        when(springDataRepository.save(any(SugerenciaIAEntity.class)))
                .thenReturn(null);
        when(notificationJpaRepository.save(any(NotificacionEntity.class)))
                .thenReturn(null);

        repository.savePriceAlert(suggestion(
                studentId,
                studentUserId,
                productId,
                null,
                tutorUserId
        ));

        ArgumentCaptor<PushNotificationRequestedEvent> pushCaptor =
                ArgumentCaptor.forClass(PushNotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(pushCaptor.capture());
        assertEquals(tutorUserId, pushCaptor.getValue().notification().userId());
        assertEquals(studentId.toString(),
                pushCaptor.getValue().notification().data().get("alumnoId"));
    }

    @Test
    void savePriceAlert_WhenRepositoriesReturnSavedEntities_ShouldUseThem() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID tutorUserId = UUID.randomUUID();
        AlumnoEntity student = new AlumnoEntity();
        student.setId(studentId);
        ProductoEntity product = new ProductoEntity();
        product.setId(productId);
        UsuarioEntity tutorUser = new UsuarioEntity();
        tutorUser.setId(tutorUserId);
        AIEventEntity savedEvent = new AIEventEntity();
        SugerenciaIAEntity savedSuggestion = new SugerenciaIAEntity();
        savedSuggestion.setId(UUID.randomUUID());
        when(entityManager.getReference(AlumnoEntity.class, studentId)).thenReturn(student);
        when(entityManager.getReference(ProductoEntity.class, productId)).thenReturn(product);
        when(entityManager.getReference(UsuarioEntity.class, tutorUserId)).thenReturn(tutorUser);
        when(aiEventJpaRepository.save(any(AIEventEntity.class))).thenReturn(savedEvent);
        when(springDataRepository.save(any(SugerenciaIAEntity.class))).thenReturn(savedSuggestion);
        when(notificationJpaRepository.save(any(NotificacionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        repository.savePriceAlert(suggestion(
                studentId,
                null,
                productId,
                null,
                tutorUserId
        ));

        ArgumentCaptor<NotificacionEntity> notificationCaptor =
                ArgumentCaptor.forClass(NotificacionEntity.class);
        verify(notificationJpaRepository).save(notificationCaptor.capture());
        assertSame(savedSuggestion, notificationCaptor.getValue().getSugerenciaIaEntity());
    }

    @Test
    void delegateTypedAndPendingFindersToSpringDataRepository() {
        UUID studentId = UUID.randomUUID();
        List<SugerenciaIAEntity> suggestions = List.of(new SugerenciaIAEntity());
        when(springDataRepository.findAllByAlumnoEntityIdAndTipoOrderByCreadoEnDesc(
                studentId,
                TipoSugerenciaIA.ALERTA_PRECIO
        )).thenReturn(suggestions);
        when(springDataRepository.findPendingParentNotificationsByAlumnoId(
                studentId,
                TipoSugerenciaIA.ALERTA_PRECIO
        )).thenReturn(suggestions);
        when(springDataRepository.findPendingStudentNotificationsByAlumnoId(
                studentId,
                TipoSugerenciaIA.ALERTA_PRECIO
        )).thenReturn(suggestions);

        assertEquals(suggestions, repository.findByAlumnoIdAndTipo(
                studentId,
                TipoSugerenciaIA.ALERTA_PRECIO
        ));
        assertEquals(suggestions, repository.findPendingParentNotificationsByAlumnoId(
                studentId,
                TipoSugerenciaIA.ALERTA_PRECIO
        ));
        assertEquals(suggestions, repository.findPendingStudentNotificationsByAlumnoId(
                studentId,
                TipoSugerenciaIA.ALERTA_PRECIO
        ));
    }

    @Test
    void findProductIdsByAlumnoIdAndTipo_ShouldReadProductEntityAndMetadata() {
        UUID studentId = UUID.randomUUID();
        UUID entityProductId = UUID.randomUUID();
        UUID metadataProductId = UUID.randomUUID();
        SugerenciaIAEntity withEntity = new SugerenciaIAEntity();
        ProductoEntity productEntity = new ProductoEntity();
        productEntity.setId(entityProductId);
        withEntity.setProductoEntity(productEntity);
        SugerenciaIAEntity withMetadata = new SugerenciaIAEntity();
        withMetadata.setMetadata(Map.of("productoId", metadataProductId.toString()));
        SugerenciaIAEntity invalidMetadata = new SugerenciaIAEntity();
        invalidMetadata.setMetadata(Map.of("productoId", "invalid"));
        SugerenciaIAEntity blankMetadata = new SugerenciaIAEntity();
        blankMetadata.setMetadata(Map.of("productoId", " "));
        SugerenciaIAEntity noMetadata = new SugerenciaIAEntity();
        when(springDataRepository.findAllByAlumnoEntityIdAndTipoOrderByCreadoEnDesc(
                studentId,
                TipoSugerenciaIA.ALERTA_PRECIO
        )).thenReturn(List.of(
                withEntity,
                withMetadata,
                invalidMetadata,
                blankMetadata,
                noMetadata
        ));

        List<UUID> result = repository.findProductIdsByAlumnoIdAndTipo(
                studentId,
                TipoSugerenciaIA.ALERTA_PRECIO
        );

        assertEquals(List.of(entityProductId, metadataProductId), result);
    }

    private PriceAlertContext savePriceAlert() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID tutorUserId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        AlumnoEntity student = new AlumnoEntity();
        ProductoEntity product = new ProductoEntity();
        BuffetEntity buffet = new BuffetEntity();
        UsuarioEntity tutorUser = new UsuarioEntity();
        tutorUser.setId(tutorUserId);

        when(entityManager.getReference(AlumnoEntity.class, studentId)).thenReturn(student);
        when(entityManager.getReference(ProductoEntity.class, productId)).thenReturn(product);
        when(entityManager.getReference(BuffetEntity.class, buffetId)).thenReturn(buffet);
        when(entityManager.getReference(UsuarioEntity.class, tutorUserId)).thenReturn(tutorUser);

        repository.savePriceAlert(suggestion(
                studentId,
                studentUserId,
                productId,
                buffetId,
                tutorUserId));

        return new PriceAlertContext(student, product, buffet, tutorUser, studentUserId, tutorUserId);
    }

    private PriceAlertSuggestion suggestion(
            UUID studentId,
            UUID studentUserId,
            UUID productId,
            UUID buffetId,
            UUID tutorUserId
    ) {
        return new PriceAlertSuggestion(
                studentId,
                studentUserId,
                productId,
                buffetId,
                Set.of(tutorUserId),
                "Alfajor",
                "Ajuste de precio",
                "El producto cambio de precio",
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                "Cambio del 20%"
        );
    }

    private record PriceAlertContext(
            AlumnoEntity student,
            ProductoEntity product,
            BuffetEntity buffet,
            UsuarioEntity tutorUser,
            UUID studentUserId,
            UUID tutorUserId
    ) {
    }
}
