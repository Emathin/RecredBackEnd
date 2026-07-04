package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.PriceAlertSuggestion;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.AIEventJpaRepository;
import com.recred.backend.infrastructure.persistence.NotificacionJpaRepository;
import com.recred.backend.infrastructure.persistence.SugerenciaIAJpaRepository;
import com.recred.backend.infrastructure.persistence.SugerenciaIARepositoryImpl;
import com.recred.backend.infrastructure.push.PushNotificationRequestedEvent;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void saveShouldPersistDomainSuggestionWithReferences() {
        UUID suggestionId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(entityManager.getReference(AlumnoEntity.class, studentId))
                .thenReturn(student(studentId));
        when(entityManager.getReference(BuffetEntity.class, buffetId))
                .thenReturn(buffet(buffetId));
        when(entityManager.getReference(ProductoEntity.class, productId))
                .thenReturn(product(productId));
        when(springDataRepository.save(any(SugerenciaIAEntity.class)))
                .thenAnswer(invocation -> {
                    SugerenciaIAEntity entity = invocation.getArgument(0);
                    entity.setId(suggestionId);
                    return entity;
                });

        SugerenciaIA result = repository.save(SugerenciaIA.builder()
                .alumnoId(studentId)
                .buffetId(buffetId)
                .productoId(productId)
                .tipo(TipoSugerenciaIA.ALERTA_PRECIO)
                .titulo("Ajuste de precio")
                .mensaje("El producto cambio de precio")
                .metadata(Map.of("source", "test"))
                .build());

        ArgumentCaptor<SugerenciaIAEntity> captor =
                ArgumentCaptor.forClass(SugerenciaIAEntity.class);
        verify(springDataRepository).save(captor.capture());
        assertEquals(suggestionId, result.getId());
        assertEquals(studentId, captor.getValue().getAlumnoEntity().getId());
        assertEquals(buffetId, captor.getValue().getBuffetEntity().getId());
        assertEquals(productId, captor.getValue().getProductoEntity().getId());
        assertFalse(captor.getValue().getAceptada());
    }

    @Test
    void savePriceAlertShouldFallbackWhenRepositoriesReturnNull() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID tutorUserId = UUID.randomUUID();
        when(entityManager.getReference(AlumnoEntity.class, studentId))
                .thenReturn(student(studentId));
        when(entityManager.getReference(ProductoEntity.class, productId))
                .thenReturn(product(productId));
        when(entityManager.getReference(UsuarioEntity.class, tutorUserId))
                .thenReturn(user(tutorUserId));
        when(aiEventJpaRepository.save(any(AIEventEntity.class))).thenReturn(null);
        when(springDataRepository.save(any(SugerenciaIAEntity.class))).thenReturn(null);
        when(notificationJpaRepository.save(any(NotificacionEntity.class))).thenReturn(null);

        repository.savePriceAlert(new PriceAlertSuggestion(
                studentId,
                null,
                productId,
                null,
                Set.of(tutorUserId),
                "Alfajor",
                "Ajuste de precio",
                "El producto cambio de precio",
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                "Cambio del 20%"
        ));

        ArgumentCaptor<SugerenciaIAEntity> suggestionCaptor =
                ArgumentCaptor.forClass(SugerenciaIAEntity.class);
        ArgumentCaptor<NotificacionEntity> notificationCaptor =
                ArgumentCaptor.forClass(NotificacionEntity.class);
        ArgumentCaptor<PushNotificationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(PushNotificationRequestedEvent.class);
        verify(springDataRepository).save(suggestionCaptor.capture());
        verify(notificationJpaRepository).save(notificationCaptor.capture());
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertEquals(TipoSugerenciaIA.ALERTA_PRECIO, suggestionCaptor.getValue().getTipo());
        assertNull(suggestionCaptor.getValue().getBuffetEntity());
        assertNull(suggestionCaptor.getValue().getMetadata().get("buffetId"));
        assertEquals(TipoNotificacion.SUGERENCIA_IA, notificationCaptor.getValue().getTipo());
        assertEquals(tutorUserId, eventCaptor.getValue().notification().userId());
        assertEquals(studentId.toString(),
                eventCaptor.getValue().notification().data().get("alumnoId"));
        assertEquals(productId.toString(),
                eventCaptor.getValue().notification().data().get("productoId"));
        assertFalse(eventCaptor.getValue().notification().data().containsKey("alumnoUsuarioId"));
    }

    @Test
    void findProductIdsByAlumnoIdAndTipoShouldIgnoreInvalidMetadata() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID metadataProductId = UUID.randomUUID();
        when(springDataRepository.findAllByAlumnoEntityIdAndTipoOrderByCreadoEnDesc(
                studentId,
                TipoSugerenciaIA.ALERTA_PRECIO))
                .thenReturn(List.of(
                        suggestionWithProduct(productId),
                        suggestionWithProduct(null),
                        suggestionWithMetadata(null),
                        suggestionWithMetadata(" "),
                        suggestionWithMetadata("not-a-uuid"),
                        suggestionWithMetadata(123),
                        suggestionWithMetadata(metadataProductId.toString())
                ));

        List<UUID> result = repository.findProductIdsByAlumnoIdAndTipo(
                studentId,
                TipoSugerenciaIA.ALERTA_PRECIO
        );

        assertEquals(2, result.size());
        assertTrue(result.contains(productId));
        assertTrue(result.contains(metadataProductId));
    }

    private SugerenciaIAEntity suggestionWithProduct(UUID productId) {
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setProductoEntity(productId == null ? product(null) : product(productId));
        return suggestion;
    }

    private SugerenciaIAEntity suggestionWithMetadata(Object value) {
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("productoId", value);
        suggestion.setMetadata(metadata);
        return suggestion;
    }

    private AlumnoEntity student(UUID studentId) {
        AlumnoEntity entity = new AlumnoEntity();
        entity.setId(studentId);
        return entity;
    }

    private BuffetEntity buffet(UUID buffetId) {
        BuffetEntity entity = new BuffetEntity();
        entity.setId(buffetId);
        return entity;
    }

    private ProductoEntity product(UUID productId) {
        ProductoEntity entity = new ProductoEntity();
        entity.setId(productId);
        return entity;
    }

    private UsuarioEntity user(UUID userId) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(userId);
        return entity;
    }
}
