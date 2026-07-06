package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AddProductRecommendation;
import com.recred.backend.application.AddProductRecommendationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/product-suggestions")
public class AddProductSuggestionController {

    private final AddProductRecommendation addProductRecommendation;

    public AddProductSuggestionController(AddProductRecommendation addProductRecommendation) {
        this.addProductRecommendation = addProductRecommendation;
    }

    @PostMapping("/crear")
    public ResponseEntity<Void> create() {
        addProductRecommendation.execute(new AddProductRecommendationRequest());
        return ResponseEntity.ok().build();
    }
}
