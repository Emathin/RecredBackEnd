package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.*;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import jakarta.persistence.EntityManager;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AIEventPersistenceRepository implements AIEventRepository {

    private static final String INPUT_ORIGIN = "BACKEND_PREDICCION_GASTO";

    private final AIEventJpaRepository aiEventJpaRepository;
    private final EntityManager entityManager;

    public AIEventPersistenceRepository(
            AIEventJpaRepository aiEventJpaRepository,
            EntityManager entityManager
    ) {
        this.aiEventJpaRepository = aiEventJpaRepository;
        this.entityManager = entityManager;
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
    public void registerProductChangeRecommendation(ProductRecommendationResult result) {
        AIEventEntity aiEventEntity = new AIEventEntity();
        aiEventEntity.setType(AIEventType.PREDICCION_GASTO);
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
        inputData.put("nombre", result.getProductChangeRecommendation().getNombre());
        inputData.put("precio", result.getProductChangeRecommendation().getPrecio());

        aiEventEntity.setInputData(inputData);

        Map<String, Object> outputData = new LinkedHashMap<>();
        result.getAiAnalysis().ifPresent(analysis -> outputData.put("analisisIa", mapProductAnalysis(
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
}
