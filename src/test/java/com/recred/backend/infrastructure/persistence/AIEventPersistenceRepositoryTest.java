package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPredictionResult;
import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIEventPersistenceRepositoryTest {

    @Mock
    private AIEventJpaRepository aiEventJpaRepository;

    @Mock
    private EntityManager entityManager;

    @Test
    void registerPredictionEventWithInputAndResult() {
        UUID studentId = UUID.randomUUID();
        AlumnoEntity studentEntity = new AlumnoEntity();
        when(entityManager.getReference(AlumnoEntity.class, studentId)).thenReturn(studentEntity);
        AIEventPersistenceRepository repository = new AIEventPersistenceRepository(
                aiEventJpaRepository,
                entityManager
        );

        repository.registerPrediction(result(studentId));

        ArgumentCaptor<AIEventEntity> captor = ArgumentCaptor.forClass(AIEventEntity.class);
        verify(aiEventJpaRepository).save(captor.capture());
        AIEventEntity event = captor.getValue();
        assertEquals(AIEventType.PREDICCION_GASTO, event.getType());
        assertSame(studentEntity, event.getAlumnoEntity());
        assertEquals("BACKEND_PREDICCION_GASTO", event.getInputOrigin());
        assertEquals(new BigDecimal("0.8000"), event.getConfidence());
        assertEquals("MENSUAL", event.getInputData().get("periodo"));
        assertEquals(new BigDecimal("120.00"), event.getResultData().get("gastoPredicho"));
        assertEquals(
                new BigDecimal("80.00"),
                event.getResultData().get("porcentajePresupuesto")
        );

        List<Map<String, Object>> categories = categories(event);
        assertEquals("Bebidas", categories.get(0).get("descripcion"));
        assertEquals(new BigDecimal("40.00"), categories.get(0).get("montoTotal"));

        Map<String, Object> aiAnalysis = aiAnalysis(event);
        assertEquals("Sube el gasto.", aiAnalysis.get("resumen"));
        assertEquals("gemini-test", aiAnalysis.get("modelo"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> categories(AIEventEntity event) {
        return (List<Map<String, Object>>) event.getResultData()
                .get("categoriasMasConsumidas");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> aiAnalysis(AIEventEntity event) {
        return (Map<String, Object>) event.getResultData().get("analisisIa");
    }

    private SpendingPredictionResult result(UUID studentId) {
        return new SpendingPredictionResult(
                prediction(studentId),
                Optional.of(new SpendingPredictionAnalysis(
                        "Sube el gasto.",
                        List.of("Alerta."),
                        List.of("Revisar presupuesto."),
                        "gemini-test"
                ))
        );
    }

    private SpendingPrediction prediction(UUID studentId) {
        return new SpendingPrediction(
                studentId,
                BudgetPeriod.MENSUAL,
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("50.00"),
                new BigDecimal("120.00"),
                new BigDecimal("10.00"),
                new BigDecimal("150.00"),
                new BigDecimal("80.00"),
                new BigDecimal("0.8000"),
                60,
                7,
                List.of(new ConsumptionCategory("Bebidas", new BigDecimal("40.00")))
        );
    }
}
