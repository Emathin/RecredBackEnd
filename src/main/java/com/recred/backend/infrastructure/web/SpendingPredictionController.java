package com.recred.backend.infrastructure.web;

import com.recred.backend.application.PredictStudentSpending;
import com.recred.backend.domain.models.BudgetPeriod;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ia/alumnos/{alumnoId}/prediccion-gasto")
public class SpendingPredictionController {

    private final PredictStudentSpending predictStudentSpending;

    public SpendingPredictionController(PredictStudentSpending predictStudentSpending) {
        this.predictStudentSpending = predictStudentSpending;
    }

    @GetMapping
    public SpendingPredictionResponse predict(
            @PathVariable("alumnoId") UUID studentId,
            @RequestParam(name = "periodo", required = false) BudgetPeriod period
    ) {
        return SpendingPredictionResponse.fromDomain(predictStudentSpending.execute(
                studentId,
                period
        ));
    }
}
