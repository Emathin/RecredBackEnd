package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GeneratePurchaseRecommendation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/ia/alumnos/patron-dias-compra")
public class PurchaseRecommendationController {

    private GeneratePurchaseRecommendation generatePurchaseRecommendation;

    public PurchaseRecommendationController(
            GeneratePurchaseRecommendation generatePurchaseRecommendation) {
        this.generatePurchaseRecommendation = generatePurchaseRecommendation;
    }

    @GetMapping
    public void setGeneratePurchaseRecommendation(
    ) {
        generatePurchaseRecommendation.execute();
    }
}
