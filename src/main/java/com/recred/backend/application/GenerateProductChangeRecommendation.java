package com.recred.backend.application;

import com.recred.backend.domain.models.*;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.services.ProductChangeAnalyzer;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GenerateProductChangeRecommendation {

    private final ProductRepository productRepository;
    private final ProductChangeAnalyzer productChangeAnalyzer;
    private final AIEventRepository aiEventRepository;
    public GenerateProductChangeRecommendation(
            ProductRepository productRepository,
            ProductChangeAnalyzer productChangeAnalyzer,
            AIEventRepository aiEventRepository

    ) {
        this.productRepository = productRepository;
        this.productChangeAnalyzer = productChangeAnalyzer;
        this.aiEventRepository = aiEventRepository;
    }
    @Transactional
    public ProductChangeAnalysis execute(UUID buffetId) {
        ProductChangeRecommendation leastSoldProduct = productRepository.findBuffetLeastSoldProducts(buffetId);
        Optional<ProductChangeAnalysis> aiAnalysis = productChangeAnalyzer
                .analyze(leastSoldProduct);
        ProductRecommendationResult result = new ProductRecommendationResult(
                leastSoldProduct,
                aiAnalysis
        );
        aiEventRepository.registerProductChangeRecommendation(result);

        return aiAnalysis.get();
    }

}
