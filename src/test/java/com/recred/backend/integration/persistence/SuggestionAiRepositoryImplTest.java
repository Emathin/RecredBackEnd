package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PurchasePatternAnalysis;
import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuggestionAiRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private SpringDataSuggestionAiRepository springDataRepository;

    private SuggestionAiRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new SuggestionAiRepositoryImpl(entityManager, springDataRepository);
    }

    @Test
    void saveShouldMapRecommendationToSuggestionEntity() {
        UUID eventId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        AIEventEntity event = new AIEventEntity();
        AlumnoEntity student = new AlumnoEntity();
        ProductoEntity product = new ProductoEntity();
        PurchaseRecommendationResult result = new PurchaseRecommendationResult(
                studentId,
                List.of(),
                Optional.of(new PurchasePatternAnalysis(
                        "Compra frecuente",
                        productId,
                        "Alfajor",
                        "MONDAY"
                ))
        );
        when(entityManager.getReference(AIEventEntity.class, eventId)).thenReturn(event);
        when(entityManager.getReference(AlumnoEntity.class, studentId)).thenReturn(student);
        when(entityManager.getReference(ProductoEntity.class, productId)).thenReturn(product);
        when(springDataRepository.save(org.mockito.ArgumentMatchers.any(SugerenciaIAEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SugerenciaIAEntity saved = repository.save(eventId, result);

        ArgumentCaptor<SugerenciaIAEntity> entityCaptor =
                ArgumentCaptor.forClass(SugerenciaIAEntity.class);
        verify(springDataRepository).save(entityCaptor.capture());
        SugerenciaIAEntity entity = entityCaptor.getValue();
        assertSame(entity, saved);
        assertEquals(Boolean.FALSE, entity.getAceptada());
        assertEquals(TipoSugerenciaIA.MONDAY, entity.getTipo());
        assertSame(event, entity.getEventoIaEntity());
        assertSame(student, entity.getAlumnoEntity());
        assertSame(product, entity.getProductoEntity());
        assertEquals("Compra frecuente", entity.getTitulo());
        assertTrue(entity.getMensaje().contains("MONDAY"));
        assertTrue(entity.getMensaje().contains("Alfajor"));
    }

    @Test
    void findByTipoShouldDelegateToSpringDataRepository() {
        List<NotificacionEntity> notifications = List.of(new NotificacionEntity());
        when(springDataRepository.findByTipo("MONDAY")).thenReturn(notifications);

        List<NotificacionEntity> result = repository.findByTipo("MONDAY");

        assertEquals(notifications, result);
        verify(springDataRepository).findByTipo("MONDAY");
    }

    @Test
    void findByTipoAndStudentIdShouldDelegateToSpringDataRepository() {
        UUID studentId = UUID.randomUUID();
        NotificacionEntity notification = new NotificacionEntity();
        when(springDataRepository.findByTipoAndStudentId("MONDAY", studentId))
                .thenReturn(notification);

        NotificacionEntity result = repository.findByTipoAndStudentId("MONDAY", studentId);

        assertSame(notification, result);
        verify(springDataRepository).findByTipoAndStudentId("MONDAY", studentId);
    }
}
