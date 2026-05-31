package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPredictionResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SpendingPredictionResponse(
        @JsonProperty("studentId")
        UUID studentId,
        @JsonProperty("periodo")
        BudgetPeriod period,
        @JsonProperty("fechaCalculo")
        LocalDate calculationDate,
        @JsonProperty("fechaInicio")
        LocalDate startDate,
        @JsonProperty("fechaFin")
        LocalDate endDate,
        @JsonProperty("gastoActual")
        BigDecimal currentSpending,
        @JsonProperty("gastoPredicho")
        BigDecimal predictedSpending,
        @JsonProperty("promedioGastoDiario")
        BigDecimal averageDailySpending,
        @JsonProperty("montoLimite")
        BigDecimal limitAmount,
        @JsonProperty("porcentajePresupuesto")
        BigDecimal budgetPercentage,
        @JsonProperty("confianza")
        BigDecimal confidence,
        @JsonProperty("diasHistoricosUsados")
        int historicalDaysUsed,
        @JsonProperty("diasRestantes")
        int remainingDays,
        @JsonProperty("categoriasMasConsumidas")
        List<ConsumptionCategoryResponse> mostConsumedCategories,
        @JsonProperty("analisisIa")
        SpendingPredictionAnalysisResponse aiAnalysis
) {

    public static SpendingPredictionResponse fromDomain(SpendingPredictionResult result) {
        SpendingPrediction spendingPrediction = result.spendingPrediction();
        return new SpendingPredictionResponse(
                spendingPrediction.studentId(),
                spendingPrediction.period(),
                spendingPrediction.calculationDate(),
                spendingPrediction.startDate(),
                spendingPrediction.endDate(),
                spendingPrediction.currentSpending(),
                spendingPrediction.predictedSpending(),
                spendingPrediction.averageDailySpending(),
                spendingPrediction.limitAmount(),
                spendingPrediction.budgetPercentage(),
                spendingPrediction.confidence(),
                spendingPrediction.historicalDaysUsed(),
                spendingPrediction.remainingDays(),
                spendingPrediction.mostConsumedCategories().stream()
                        .map(ConsumptionCategoryResponse::fromDomain)
                        .toList(),
                result.aiAnalysis()
                        .map(SpendingPredictionAnalysisResponse::fromDomain)
                        .orElse(null)
        );
    }
}
