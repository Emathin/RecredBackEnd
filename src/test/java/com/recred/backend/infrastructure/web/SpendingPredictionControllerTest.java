package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.application.PredictStudentSpending;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPredictionResult;
import com.recred.backend.infrastructure.web.SpendingPredictionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpendingPredictionControllerTest {

    @Test
    void delegatePredictionWithRequestedPeriod() {
        PredictStudentSpending predictStudentSpending = mock(PredictStudentSpending.class);
        SpendingPredictionController controller = new SpendingPredictionController(predictStudentSpending);
        UUID studentId = UUID.randomUUID();
        SpendingPredictionResult result = result(studentId, BudgetPeriod.MENSUAL);
        when(predictStudentSpending.execute(studentId, BudgetPeriod.MENSUAL)).thenReturn(result);

        SpendingPredictionResponse obtenido = controller.predict(
                studentId,
                BudgetPeriod.MENSUAL
        );

        assertEquals(studentId, obtenido.studentId());
        assertEquals(BudgetPeriod.MENSUAL, obtenido.period());
        assertEquals("Resumen IA.", obtenido.aiAnalysis().summary());
        verify(predictStudentSpending).execute(studentId, BudgetPeriod.MENSUAL);
    }

    @Test
    void delegatePredictionWithoutRequestedPeriod() {
        PredictStudentSpending predictStudentSpending = mock(PredictStudentSpending.class);
        SpendingPredictionController controller = new SpendingPredictionController(predictStudentSpending);
        UUID studentId = UUID.randomUUID();
        SpendingPredictionResult result = result(studentId, BudgetPeriod.QUINCENAL);
        when(predictStudentSpending.execute(studentId, null)).thenReturn(result);

        SpendingPredictionResponse obtenido = controller.predict(studentId, null);

        assertEquals(BudgetPeriod.QUINCENAL, obtenido.period());
        verify(predictStudentSpending).execute(studentId, null);
    }

    private SpendingPredictionResult result(UUID studentId, BudgetPeriod period) {
        return new SpendingPredictionResult(
                prediction(studentId, period),
                Optional.of(new SpendingPredictionAnalysis(
                        "Resumen IA.",
                        List.of(),
                        List.of(),
                        "gemini-test"
                ))
        );
    }

    private SpendingPrediction prediction(UUID studentId, BudgetPeriod period) {
        return new SpendingPrediction(
                studentId,
                period,
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                BigDecimal.ONE,
                0,
                0,
                List.of()
        );
    }
}
