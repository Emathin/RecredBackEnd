package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.services.ProductChangeAnalyzer;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public class GenerateProductChangeRecommendation {

    private final ProductRepository productRepository;
    private final ProductChangeAnalyzer productChangeAnalyzer;
    private final AIEventRepository aiEventRepository;
    private final KiosqueroRepository kiosqueroRepository;
    public GenerateProductChangeRecommendation(
            ProductRepository productRepository,
            ProductChangeAnalyzer productChangeAnalyzer,
            AIEventRepository aiEventRepository,
            KiosqueroRepository kiosqueroRepository
    ) {
        this.productRepository = productRepository;
        this.productChangeAnalyzer = productChangeAnalyzer;
        this.aiEventRepository = aiEventRepository;
        this.kiosqueroRepository = kiosqueroRepository;
    }
    @Transactional
    public ProductChangeAnalysis execute(UUID sellerId) {
        UUID buffetId = kiosqueroRepository.findBuffetById(sellerId);
        if (buffetId == null) {
            throw new KiosqueroNotFoundException(sellerId);
        }
        ProductChangeRecommendation leastSoldProduct =
                productRepository.findBuffetLeastSoldProducts(buffetId);
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
