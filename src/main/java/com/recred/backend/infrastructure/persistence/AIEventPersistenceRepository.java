package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.PurchasePatternAnalysis;
import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.models.SpendingPredictionResult;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class AIEventPersistenceRepository implements AIEventRepository {

    private static final String INPUT_ORIGIN = "BACKEND_PREDICCION_GASTO";

    private final AIEventJpaRepository aiEventJpaRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public AIEventPersistenceRepository(
            AIEventJpaRepository aiEventJpaRepository,
            EntityManager entityManager,
            ObjectMapper objectMapper
    ) {
        this.aiEventJpaRepository = aiEventJpaRepository;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ProductRecommendationResult> findLatestProductChangeRecommendation(
            UUID buffetId
    ) {
        return aiEventJpaRepository.findFirstByTypeAndBuffetEntityIdOrderByCreatedAtDesc(
                AIEventType.PRODUCTO_CAMBIO,
                buffetId
        ).map(this::mapToResult);
    }

    @Override
    public List<ProductRecommendationResult> findAllProductChangeRecommendations(
            UUID buffetId
    ) {
        return aiEventJpaRepository.findAllByTypeAndBuffetEntityIdOrderByCreatedAtDesc(
                AIEventType.PRODUCTO_CAMBIO,
                buffetId
        ).stream().map(this::mapToResult).toList();
    }

    @SuppressWarnings("unchecked")
    private ProductRecommendationResult mapToResult(AIEventEntity entity) {
        ProductChangeRecommendation recommendation = new ProductChangeRecommendation();
        recommendation.setId(entity.getProductoEntity().getId());
        recommendation.setIdBuffet(entity.getBuffetEntity().getId());
        recommendation.setNombre((String) entity.getInputData().get("nombre"));
        Object precio = entity.getInputData().get("precio");
        if (precio instanceof Number) {
            recommendation.setPrecio(BigDecimal.valueOf(((Number) precio).doubleValue()));
        }
        
        Object statsObj = entity.getInputData().get("saleStatistics");
        if (statsObj instanceof Map) {
            recommendation.setSaleStatistics(objectMapper.convertValue(statsObj,
                    com.recred.backend.domain.models.ProductSaleStatistics.class));
        }

        Map<String, Object> analysisMap = (Map<String, Object>) entity.getResultData()
                .get("analisisIa");
        ProductChangeAnalysis analysis = null;
        if (analysisMap != null) {
            analysis = new ProductChangeAnalysis(
                    (String) analysisMap.get("resumen"),
                    (List<String>) analysisMap.getOrDefault("alertas", new ArrayList<>()),
                    (List<String>) analysisMap.getOrDefault("recomendaciones", new ArrayList<>()),
                    (String) analysisMap.get("modelo")
            );
        }

        return new ProductRecommendationResult(recommendation, Optional.ofNullable(analysis));
    }

    @Override
    public void registerPrediction(SpendingPredictionResult result) {
        SpendingPrediction spendingPrediction = result.spendingPrediction();
        AIEventEntity aiEventEntity = new AIEventEntity();
        aiEventEntity.setType(AIEventType.PREDICCION_GASTO);
        aiEventEntity.setAlumnoEntity(entityManager.getReference(
                AlumnoEntity.class,
                spendingPrediction.studentId()
        ));
        aiEventEntity.setInputOrigin(INPUT_ORIGIN);
        aiEventEntity.setInputData(inputData(spendingPrediction));
        aiEventEntity.setResultData(resultData(result));
        aiEventEntity.setConfidence(spendingPrediction.confidence());

        aiEventJpaRepository.save(aiEventEntity);
    }

    private Map<String, Object> inputData(SpendingPrediction spendingPrediction) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("periodo", spendingPrediction.period().name());
        data.put("fechaCalculo", spendingPrediction.calculationDate().toString());
        data.put("fechaInicio", spendingPrediction.startDate().toString());
        data.put("fechaFin", spendingPrediction.endDate().toString());
        return data;
    }

    private Map<String, Object> resultData(SpendingPredictionResult result) {
        SpendingPrediction spendingPrediction = result.spendingPrediction();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("gastoActual", spendingPrediction.currentSpending());
        data.put("gastoPredicho", spendingPrediction.predictedSpending());
        data.put("promedioGastoDiario", spendingPrediction.averageDailySpending());
        data.put("montoLimite", spendingPrediction.limitAmount());
        data.put("porcentajePresupuesto", spendingPrediction.budgetPercentage());
        data.put("diasHistoricosUsados", spendingPrediction.historicalDaysUsed());
        data.put("diasRestantes", spendingPrediction.remainingDays());
        data.put("categoriasMasConsumidas", mapCategories(
                spendingPrediction.mostConsumedCategories()));
        result.aiAnalysis().ifPresent(analysis -> data.put("analisisIa", mapAnalysis(
                analysis)));
        return data;
    }

    private Map<String, Object> mapAnalysis(SpendingPredictionAnalysis analysis) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resumen", analysis.summary());
        data.put("alertas", analysis.alerts());
        data.put("recomendaciones", analysis.recommendations());
        data.put("modelo", analysis.model());
        return data;
    }

    private List<Map<String, Object>> mapCategories(List<ConsumptionCategory> categories) {
        return categories.stream()
                .map(this::mapCategory)
                .toList();
    }

    private Map<String, Object> mapCategory(ConsumptionCategory consumptionCategory) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("descripcion", consumptionCategory.description());
        data.put("montoTotal", consumptionCategory.totalAmount());
        return data;
    }
    @Override
    @SuppressWarnings("unchecked")
    public void registerProductChangeRecommendation(ProductRecommendationResult result) {
        AIEventEntity aiEventEntity = new AIEventEntity();
        aiEventEntity.setType(AIEventType.PRODUCTO_CAMBIO);
        aiEventEntity.setBuffetEntity(entityManager.getReference(
                BuffetEntity.class,
                result.getProductChangeRecommendation().getIdBuffet()
        ));
        aiEventEntity.setProductoEntity(entityManager.getReference(
                ProductoEntity.class,
                result.getProductChangeRecommendation().getId()
        ));
        aiEventEntity.setInputOrigin("Backend Product Reco");

        Map<String, Object> inputData = new LinkedHashMap<>();
        inputData.put("id", result.getProductChangeRecommendation().getId());
        inputData.put("idBuffet", result.getProductChangeRecommendation().getIdBuffet());
        inputData.put("nombre", result.getProductChangeRecommendation().getNombre());
        inputData.put("precio", result.getProductChangeRecommendation().getPrecio());
        
        if (result.getProductChangeRecommendation().getSaleStatistics() != null) {
            inputData.put("saleStatistics", objectMapper.convertValue(
                    result.getProductChangeRecommendation().getSaleStatistics(), Map.class));
        }

        aiEventEntity.setInputData(inputData);

        Map<String, Object> outputData = new LinkedHashMap<>();
        result.getAiAnalysis().ifPresent(analysis
                -> outputData.put("analisisIa", mapProductAnalysis(
                analysis)));
        aiEventEntity.setResultData(outputData);

        aiEventJpaRepository.save(aiEventEntity);

    }
    private Map<String, Object> mapProductAnalysis(ProductChangeAnalysis analysis) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resumen", analysis.summary());
        data.put("alertas", analysis.alerts());
        data.put("recomendaciones", analysis.recommendations());
        data.put("modelo", analysis.model());
        return data;
    }

    private Map<String, Object> mapPurchaseAnalysis(PurchasePatternAnalysis analysis) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resumen", analysis.summary());
        data.put("Id_Producto", analysis.product_id());
        data.put("Nombre Producto", analysis.productName());
        data.put("Dias de compra", analysis.purchaseDays());
        return data;
    }

    @Override
    public void registerProductAdditionRecommendation(
            Producto product, Kiosquero kiosquero, UUID buffetId) {
        AIEventEntity aiEventEntity = new AIEventEntity();
        aiEventEntity.setType(AIEventType.AGREGAR_PRODUCTO);
        aiEventEntity.setBuffetEntity(entityManager.getReference(
                BuffetEntity.class,
                buffetId
        ));
        aiEventEntity.setProductoEntity(entityManager.getReference(
                ProductoEntity.class,
                product.getId()
        ));
        aiEventEntity.setInputOrigin("BACKEND PRODUCT ADDITION RECO");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resumen", "Te recomendamos agregar " + product.getNombre() +
                " a tu stock") ;
        aiEventEntity.setResultData(data);
        aiEventJpaRepository.save(aiEventEntity);
    }

    @Override
    public UUID registerPurchaseRecommendation(PurchaseRecommendationResult result) {
        AIEventEntity aiEventEntity = new AIEventEntity();
        aiEventEntity.setType(AIEventType.ANALISIS_CONSUMO);
        aiEventEntity.setAlumnoEntity(entityManager.getReference(
                AlumnoEntity.class,
                result.getAlumnoId()
        ));

        aiEventEntity.setProductoEntity(entityManager.getReference(
                ProductoEntity.class,
                result.getAiAnalysis().get().product_id()
        ));

        aiEventEntity.setInputOrigin("Backend Product Reco");

        Map<String, Object> inputData = new LinkedHashMap<>();
        inputData.put("Lista de compras", result.getPurchases());


        aiEventEntity.setInputData(inputData);

        Map<String, Object> outputData = new LinkedHashMap<>();
        result.getAiAnalysis().ifPresent(analysis
                -> outputData.put("analisisIa", mapPurchaseAnalysis(
                analysis)));
        aiEventEntity.setResultData(outputData);

        AIEventEntity savedEvent = aiEventJpaRepository.save(aiEventEntity);
        return savedEvent.getId();
    }
}
