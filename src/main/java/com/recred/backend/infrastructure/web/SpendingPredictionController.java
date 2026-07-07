package com.recred.backend.infrastructure.web;

import com.recred.backend.application.PredictStudentSpending;
import com.recred.backend.application.PredictStudentSpendingRequest;
import com.recred.backend.application.PredictStudentSpendingResponse;
import com.recred.backend.domain.models.BudgetPeriod;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ia/alumnos/{alumnoId}/prediccion-gasto")
public class SpendingPredictionController {

    private final PredictStudentSpending predictStudentSpending;
    private final PlanAccessGuard planAccessGuard;

    public SpendingPredictionController(
            PredictStudentSpending predictStudentSpending,
            PlanAccessGuard planAccessGuard
    ) {
        this.predictStudentSpending = predictStudentSpending;
        this.planAccessGuard = planAccessGuard;
    }

    @GetMapping
    public PredictStudentSpendingResponse predict(
            @PathVariable("alumnoId") UUID studentId,
            @RequestParam(name = "periodo", required = false) BudgetPeriod period,
            Authentication authentication
    ) {
        planAccessGuard.requireTutorOrStudentIntermediateForStudent(authentication, studentId);
        return predictStudentSpending.execute(new PredictStudentSpendingRequest(
                studentId,
                period
        ));
    }
}
