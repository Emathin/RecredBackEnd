package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.PredictStudentSpending;
import com.recred.backend.application.PredictStudentSpendingRequest;
import com.recred.backend.application.PredictStudentSpendingResponse;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.SpendingPredictionController;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpendingPredictionControllerTest {

    @Test
    void givenPredictionRequestWhenPredictingThenDelegatesToUseCaseWithRequestedPeriod() {
        PredictStudentSpending predictStudentSpending = mock(PredictStudentSpending.class);
        PlanAccessGuard planAccessGuard = mock(PlanAccessGuard.class);
        SpendingPredictionController controller =
                new SpendingPredictionController(predictStudentSpending, planAccessGuard);
        UUID studentId = UUID.randomUUID();
        Authentication authentication = authentication();
        PredictStudentSpendingRequest request = request(studentId, BudgetPeriod.MENSUAL);
        PredictStudentSpendingResponse response = response(studentId, BudgetPeriod.MENSUAL);
        givenPrediction(predictStudentSpending, request, response);

        PredictStudentSpendingResponse result = whenPredictionIsRequested(
                controller,
                studentId,
                BudgetPeriod.MENSUAL,
                authentication
        );

        thenRequestedPeriodResponseMatches(result, studentId);
        verify(planAccessGuard).requireTutorOrStudentIntermediateForStudent(
                authentication,
                studentId
        );
        thenUseCaseWasCalledWith(predictStudentSpending, request);
    }

    @Test
    void givenPredictionRequestWhenPredictingThenDelegatesToUseCaseWithDefaultPeriod() {
        PredictStudentSpending predictStudentSpending = mock(PredictStudentSpending.class);
        PlanAccessGuard planAccessGuard = mock(PlanAccessGuard.class);
        SpendingPredictionController controller =
                new SpendingPredictionController(predictStudentSpending, planAccessGuard);
        UUID studentId = UUID.randomUUID();
        Authentication authentication = authentication();
        PredictStudentSpendingRequest request = request(studentId, null);
        PredictStudentSpendingResponse response = response(studentId, BudgetPeriod.QUINCENAL);
        givenPrediction(predictStudentSpending, request, response);

        PredictStudentSpendingResponse result = whenPredictionIsRequested(
                controller,
                studentId,
                null,
                authentication
        );

        thenDefaultPeriodResponseMatches(result);
        verify(planAccessGuard).requireTutorOrStudentIntermediateForStudent(
                authentication,
                studentId
        );
        thenUseCaseWasCalledWith(predictStudentSpending, request);
    }

    private void givenPrediction(
            PredictStudentSpending predictStudentSpending,
            PredictStudentSpendingRequest request,
            PredictStudentSpendingResponse response
    ) {
        when(predictStudentSpending.execute(request)).thenReturn(response);
    }

    private PredictStudentSpendingResponse whenPredictionIsRequested(
            SpendingPredictionController controller,
            UUID studentId,
            BudgetPeriod period,
            Authentication authentication
    ) {
        return controller.predict(studentId, period, authentication);
    }

    private void thenRequestedPeriodResponseMatches(
            PredictStudentSpendingResponse response,
            UUID studentId
    ) {
        assertEquals(studentId, response.studentId());
        assertEquals(BudgetPeriod.MENSUAL, response.period());
        assertEquals("Resumen IA.", response.aiAnalysis().summary());
    }

    private void thenDefaultPeriodResponseMatches(PredictStudentSpendingResponse response) {
        assertEquals(BudgetPeriod.QUINCENAL, response.period());
    }

    private void thenUseCaseWasCalledWith(
            PredictStudentSpending predictStudentSpending,
            PredictStudentSpendingRequest request
    ) {
        verify(predictStudentSpending).execute(request);
    }

    private PredictStudentSpendingRequest request(UUID studentId, BudgetPeriod period) {
        return new PredictStudentSpendingRequest(studentId, period);
    }

    private PredictStudentSpendingResponse response(UUID studentId, BudgetPeriod period) {
        return new PredictStudentSpendingResponse(
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
                List.of(),
                new PredictStudentSpendingResponse.SpendingPredictionAnalysisResponse(
                        "Resumen IA.",
                        List.of(),
                        List.of(),
                        "gemini-test"
                )
        );
    }

    private Authentication authentication() {
        return new TestingAuthenticationToken("tutor-sub", "");
    }
}
