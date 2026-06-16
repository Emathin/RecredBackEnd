package com.recred.backend.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPredictionResult;
import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.models.ProductSaleStatistics;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.PurchasePatternAnalysis;
import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.domain.models.PurchaseResume;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIEventPersistenceRepositoryTest {

    @Mock
    private AIEventJpaRepository aiEventJpaRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void registerPredictionEventWithInputAndResult() {
        UUID studentId = UUID.randomUUID();
        AlumnoEntity studentEntity = new AlumnoEntity();
        when(entityManager.getReference(AlumnoEntity.class, studentId)).thenReturn(studentEntity);
        AIEventPersistenceRepository repository = new AIEventPersistenceRepository(
                aiEventJpaRepository,
                entityManager,
                objectMapper
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

    @Test
    void findAllProductChangeRecommendations() {
        UUID buffetId = UUID.randomUUID();
        AIEventPersistenceRepository repository = new AIEventPersistenceRepository(
                aiEventJpaRepository,
                entityManager,
                objectMapper
        );

        AIEventEntity event1 = new AIEventEntity();
        event1.setResultData(Map.of());
        event1.setInputData(Map.of("nombre", "Producto 1", "precio", 10.0));
        ProductoEntity p1 = new ProductoEntity();
        p1.setId(UUID.randomUUID());
        event1.setProductoEntity(p1);
        BuffetEntity b1 = new BuffetEntity();
        b1.setId(buffetId);
        event1.setBuffetEntity(b1);

        when(aiEventJpaRepository.findAllByTypeAndBuffetEntityIdOrderByCreatedAtDesc(
                AIEventType.PRODUCTO_CAMBIO, buffetId))
                .thenReturn(List.of(event1));

        List<ProductRecommendationResult> results = repository.findAllProductChangeRecommendations(buffetId);
        assertEquals(1, results.size());
        assertEquals("Producto 1", results.get(0).getProductChangeRecommendation().getNombre());
    }

    @Test
    void findLatestProductChangeRecommendation_ShouldMapAnalysis() {
        UUID buffetId = UUID.randomUUID();
        AIEventPersistenceRepository repository = new AIEventPersistenceRepository(
                aiEventJpaRepository,
                entityManager,
                objectMapper
        );
        AIEventEntity event = productChangeEvent(buffetId);
        Map<String, Object> statsMap = Map.of("ventasPeriodo", 5);
        ProductSaleStatistics statistics = new ProductSaleStatistics();
        statistics.setVentasPeriodo(5L);
        event.setInputData(Map.of(
                "nombre", "Producto 1",
                "precio", 10.0,
                "saleStatistics", statsMap
        ));
        event.setResultData(Map.of("analisisIa", Map.of(
                "resumen", "Conviene ajustar stock.",
                "alertas", List.of("Baja rotacion"),
                "recomendaciones", List.of("Promocionar"),
                "modelo", "gemini-test"
        )));
        when(objectMapper.convertValue(statsMap, ProductSaleStatistics.class))
                .thenReturn(statistics);
        when(aiEventJpaRepository.findFirstByTypeAndBuffetEntityIdOrderByCreatedAtDesc(
                AIEventType.PRODUCTO_CAMBIO,
                buffetId
        )).thenReturn(Optional.of(event));

        Optional<ProductRecommendationResult> result =
                repository.findLatestProductChangeRecommendation(buffetId);

        assertTrue(result.isPresent());
        assertTrue(result.orElseThrow().getAiAnalysis().isPresent());
        assertEquals("Conviene ajustar stock.",
                result.orElseThrow().getAiAnalysis().orElseThrow().summary());
        assertEquals(5L, result.orElseThrow()
                .getProductChangeRecommendation()
                .getSaleStatistics()
                .getVentasPeriodo());
    }

    @Test
    void registerProductChangeRecommendation_ShouldPersistInputAndAnalysis() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        BuffetEntity buffetEntity = new BuffetEntity();
        ProductoEntity productEntity = new ProductoEntity();
        when(entityManager.getReference(BuffetEntity.class, buffetId)).thenReturn(buffetEntity);
        when(entityManager.getReference(ProductoEntity.class, productId))
                .thenReturn(productEntity);
        AIEventPersistenceRepository repository = new AIEventPersistenceRepository(
                aiEventJpaRepository,
                entityManager,
                objectMapper
        );

        repository.registerProductChangeRecommendation(productChangeResult(
                buffetId,
                productId,
                Optional.of(new ProductChangeAnalysis(
                        "Subio la demanda.",
                        List.of("Poco stock"),
                        List.of("Reponer"),
                        "gemini-test"
                ))
        ));

        ArgumentCaptor<AIEventEntity> captor = ArgumentCaptor.forClass(AIEventEntity.class);
        verify(aiEventJpaRepository).save(captor.capture());
        AIEventEntity event = captor.getValue();
        assertEquals(AIEventType.PRODUCTO_CAMBIO, event.getType());
        assertSame(buffetEntity, event.getBuffetEntity());
        assertSame(productEntity, event.getProductoEntity());
        assertEquals("Alfajor", event.getInputData().get("nombre"));
        assertEquals("Subio la demanda.",
                aiAnalysis(event).get("resumen"));
    }

    @Test
    void registerProductChangeRecommendation_WhenStatisticsExist_ShouldPersistThem() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductSaleStatistics statistics = new ProductSaleStatistics();
        statistics.setVentasPeriodo(7L);
        Map<String, Object> statisticsMap = Map.of("ventasPeriodo", 7L);
        ProductChangeRecommendation recommendation = new ProductChangeRecommendation();
        recommendation.setId(productId);
        recommendation.setIdBuffet(buffetId);
        recommendation.setNombre("Alfajor");
        recommendation.setPrecio(new BigDecimal("10.00"));
        recommendation.setSaleStatistics(statistics);
        when(entityManager.getReference(BuffetEntity.class, buffetId))
                .thenReturn(new BuffetEntity());
        when(entityManager.getReference(ProductoEntity.class, productId))
                .thenReturn(new ProductoEntity());
        when(objectMapper.convertValue(statistics, Map.class)).thenReturn(statisticsMap);
        AIEventPersistenceRepository repository = new AIEventPersistenceRepository(
                aiEventJpaRepository,
                entityManager,
                objectMapper
        );

        repository.registerProductChangeRecommendation(
                new ProductRecommendationResult(recommendation, Optional.empty())
        );

        ArgumentCaptor<AIEventEntity> captor = ArgumentCaptor.forClass(AIEventEntity.class);
        verify(aiEventJpaRepository).save(captor.capture());
        assertEquals(statisticsMap, captor.getValue().getInputData().get("saleStatistics"));
    }

    @Test
    void registerProductAdditionRecommendation_ShouldPersistProductSuggestion() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Producto product = new Producto();
        product.setId(productId);
        product.setNombre("Barrita");
        when(entityManager.getReference(BuffetEntity.class, buffetId))
                .thenReturn(new BuffetEntity());
        when(entityManager.getReference(ProductoEntity.class, productId))
                .thenReturn(new ProductoEntity());
        AIEventPersistenceRepository repository = new AIEventPersistenceRepository(
                aiEventJpaRepository,
                entityManager,
                objectMapper
        );

        repository.registerProductAdditionRecommendation(product, new Kiosquero(), buffetId);

        ArgumentCaptor<AIEventEntity> captor = ArgumentCaptor.forClass(AIEventEntity.class);
        verify(aiEventJpaRepository).save(captor.capture());
        assertEquals(AIEventType.AGREGAR_PRODUCTO, captor.getValue().getType());
        assertTrue(captor.getValue().getResultData().get("resumen")
                .toString()
                .contains("Barrita"));
    }

    @Test
    void registerPurchaseRecommendation_ShouldPersistAnalysisAndReturnSavedId() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        AlumnoEntity studentEntity = new AlumnoEntity();
        ProductoEntity productEntity = new ProductoEntity();
        when(entityManager.getReference(AlumnoEntity.class, studentId))
                .thenReturn(studentEntity);
        when(entityManager.getReference(ProductoEntity.class, productId))
                .thenReturn(productEntity);
        when(aiEventJpaRepository.save(org.mockito.ArgumentMatchers.any(AIEventEntity.class)))
                .thenAnswer(invocation -> {
                    AIEventEntity event = invocation.getArgument(0);
                    event.setId(eventId);
                    return event;
                });
        AIEventPersistenceRepository repository = new AIEventPersistenceRepository(
                aiEventJpaRepository,
                entityManager,
                objectMapper
        );

        UUID result = repository.registerPurchaseRecommendation(new PurchaseRecommendationResult(
                studentId,
                List.of(new PurchaseResume()),
                Optional.of(new PurchasePatternAnalysis(
                        "Compra habitual.",
                        productId,
                        "Yogur",
                        "lunes"
                ))
        ));

        assertEquals(eventId, result);
        ArgumentCaptor<AIEventEntity> captor = ArgumentCaptor.forClass(AIEventEntity.class);
        verify(aiEventJpaRepository).save(captor.capture());
        AIEventEntity event = captor.getValue();
        assertEquals(AIEventType.ANALISIS_CONSUMO, event.getType());
        assertSame(studentEntity, event.getAlumnoEntity());
        assertSame(productEntity, event.getProductoEntity());
        assertEquals("Compra habitual.", aiAnalysis(event).get("resumen"));
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

    private AIEventEntity productChangeEvent(UUID buffetId) {
        AIEventEntity event = new AIEventEntity();
        ProductoEntity productEntity = new ProductoEntity();
        productEntity.setId(UUID.randomUUID());
        BuffetEntity buffetEntity = new BuffetEntity();
        buffetEntity.setId(buffetId);
        event.setProductoEntity(productEntity);
        event.setBuffetEntity(buffetEntity);
        event.setInputData(Map.of(
                "nombre", "Producto 1",
                "precio", 10.0
        ));
        event.setResultData(Map.of());
        return event;
    }

    private ProductRecommendationResult productChangeResult(
            UUID buffetId,
            UUID productId,
            Optional<ProductChangeAnalysis> analysis
    ) {
        ProductChangeRecommendation recommendation = new ProductChangeRecommendation();
        recommendation.setId(productId);
        recommendation.setIdBuffet(buffetId);
        recommendation.setNombre("Alfajor");
        recommendation.setPrecio(new BigDecimal("10.00"));
        return new ProductRecommendationResult(recommendation, analysis);
    }
}
