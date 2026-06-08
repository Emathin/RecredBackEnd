package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GeneratePurchaseRecommendation;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


@RestController

public class PurchaseRecommendationController {

    private GeneratePurchaseRecommendation generatePurchaseRecommendation;

    public PurchaseRecommendationController(
            GeneratePurchaseRecommendation generatePurchaseRecommendation) {
        this.generatePurchaseRecommendation = generatePurchaseRecommendation;
    }

    @RequestMapping("/api/v1/ia/alumnos/{idAlumno}/obtenerSugerenciaDelDia")
    @GetMapping
    public NotificacionEntity getTodayPurchaseRecommendation(
            @PathVariable("idAlumno") UUID studentID
    ) {
        return generatePurchaseRecommendation.findSuggestionForToday(studentID);
    }

    @RequestMapping("/api/v1/ia/alumnos/{idAlumno}/obtenerSugerenciaManiana")
    @GetMapping
    public NotificacionEntity getTomorrowPurchaseRecommendation(
            @PathVariable("idAlumno") UUID studentID
    ) {
        return generatePurchaseRecommendation.findSuggestionForToday(studentID);
    }

}
