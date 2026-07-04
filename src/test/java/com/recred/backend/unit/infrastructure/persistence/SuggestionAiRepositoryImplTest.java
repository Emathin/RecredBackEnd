package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.PurchasePatternAnalysis;
import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.persistence.NotificacionJpaRepository;
import com.recred.backend.infrastructure.persistence.SpringDataSuggestionAiRepository;
import com.recred.backend.infrastructure.persistence.SuggestionAiRepositoryImpl;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuggestionAiRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private SpringDataSuggestionAiRepository repository;

    @Mock
    private NotificacionJpaRepository notificacionRepository;

    @Test
    void saveShouldBuildSuggestionEntityFromPurchaseRecommendation() {
        SuggestionAiRepositoryImpl adapter = adapter();
        UUID eventId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        PurchaseRecommendationResult result = new PurchaseRecommendationResult(
                studentId,
                List.of(),
                Optional.of(new PurchasePatternAnalysis(
                        "Comprar yogur",
                        productId,
                        "Yogur",
                        "MONDAY"
                ))
        );
        AIEventEntity event = new AIEventEntity();
        AlumnoEntity student = new AlumnoEntity();
        student.setId(studentId);
        ProductoEntity product = new ProductoEntity();
        product.setId(productId);
        ArgumentCaptor<SugerenciaIAEntity> captor =
                ArgumentCaptor.forClass(SugerenciaIAEntity.class);
        when(entityManager.getReference(AIEventEntity.class, eventId)).thenReturn(event);
        when(entityManager.getReference(AlumnoEntity.class, studentId)).thenReturn(student);
        when(entityManager.getReference(ProductoEntity.class, productId)).thenReturn(product);
        when(repository.save(any(SugerenciaIAEntity.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        SugerenciaIAEntity saved = adapter.save(eventId, result);

        verify(repository).save(captor.capture());
        assertEquals(false, captor.getValue().getAceptada());
        assertEquals(TipoSugerenciaIA.MONDAY, saved.getTipo());
        assertEquals(studentId, saved.getAlumnoEntity().getId());
        assertEquals(productId, saved.getProductoEntity().getId());
        assertEquals("Comprar yogur", saved.getTitulo());
        assertEquals("Ma\u00f1ana es MONDAY Comprate tu Yogur", saved.getMensaje());
    }

    @Test
    void findByTipoAndStudentShouldDelegateToRepository() {
        SuggestionAiRepositoryImpl adapter = adapter();
        UUID studentId = UUID.randomUUID();
        NotificacionEntity notification = new NotificacionEntity();
        when(repository.findByTipo("MONDAY")).thenReturn(List.of(notification));
        when(repository.findByTipoAndStudentId("MONDAY", studentId)).thenReturn(notification);

        assertEquals(List.of(notification), adapter.findByTipo("MONDAY"));
        assertEquals(notification, adapter.findByTipoAndStudentId("MONDAY", studentId));
    }

    @Test
    void findTop3ByBuffetIdAndTipoShouldFilterNullProductsDeduplicateAndLimit() {
        SuggestionAiRepositoryImpl adapter = adapter();
        UUID buffetId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        UUID thirdProductId = UUID.randomUUID();
        UUID fourthProductId = UUID.randomUUID();
        SugerenciaIAEntity first = suggestion(
                buffetId,
                firstProductId,
                LocalDateTime.of(2026, 7, 4, 10, 0)
        );
        SugerenciaIAEntity duplicateFirst = suggestion(
                buffetId,
                firstProductId,
                LocalDateTime.of(2026, 7, 4, 8, 0)
        );
        SugerenciaIAEntity withoutProduct = suggestion(
                buffetId,
                null,
                LocalDateTime.of(2026, 7, 4, 11, 0)
        );
        SugerenciaIAEntity second = suggestion(
                buffetId,
                secondProductId,
                LocalDateTime.of(2026, 7, 4, 9, 0)
        );
        SugerenciaIAEntity third = suggestion(
                buffetId,
                thirdProductId,
                LocalDateTime.of(2026, 7, 4, 7, 0)
        );
        SugerenciaIAEntity fourth = suggestion(
                buffetId,
                fourthProductId,
                LocalDateTime.of(2026, 7, 4, 6, 0)
        );
        when(repository.findDistinctByBuffetEntityIdAndTipoOrderByCreadoEnDesc(
                buffetId,
                TipoSugerenciaIA.MONDAY
        )).thenReturn(List.of(
                first,
                withoutProduct,
                second,
                duplicateFirst,
                third,
                fourth
        ));

        List<SugerenciaIA> result = adapter.findTop3ByBuffetIdAndTipo(
                buffetId,
                TipoSugerenciaIA.MONDAY
        );

        assertEquals(3, result.size());
        assertEquals(firstProductId, result.get(0).getProductoId());
        assertEquals(secondProductId, result.get(1).getProductoId());
        assertEquals(thirdProductId, result.get(2).getProductoId());
    }

    @Test
    void getSuggestionByNotificationIdShouldMapSuggestionOrFail() {
        SuggestionAiRepositoryImpl adapter = adapter();
        UUID notificationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        NotificacionEntity notification = new NotificacionEntity();
        notification.setSugerenciaIaEntity(suggestion(
                UUID.randomUUID(),
                productId,
                LocalDateTime.of(2026, 7, 4, 10, 0)
        ));
        when(notificacionRepository.findByIdWithSuggestion(notificationId))
                .thenReturn(Optional.of(notification), Optional.empty());

        SugerenciaIA found = adapter.getSuggestionByNotificationId(notificationId);
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.getSuggestionByNotificationId(notificationId)
        );

        assertEquals(productId, found.getProductoId());
        assertEquals("Sugerencia no encontrada para la notificaci\u00f3n: " + notificationId,
                exception.getMessage());
    }

    private SuggestionAiRepositoryImpl adapter() {
        return new SuggestionAiRepositoryImpl(
                entityManager,
                repository,
                notificacionRepository
        );
    }

    private SugerenciaIAEntity suggestion(
            UUID buffetId,
            UUID productId,
            LocalDateTime createdAt
    ) {
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setId(UUID.randomUUID());
        suggestion.setTipo(TipoSugerenciaIA.MONDAY);
        suggestion.setTitulo("Titulo");
        suggestion.setMensaje("Mensaje");
        suggestion.setCreadoEn(createdAt);
        BuffetEntity buffet = new BuffetEntity();
        buffet.setId(buffetId);
        suggestion.setBuffetEntity(buffet);
        if (productId != null) {
            ProductoEntity product = new ProductoEntity();
            product.setId(productId);
            suggestion.setProductoEntity(product);
        }
        return suggestion;
    }
}
