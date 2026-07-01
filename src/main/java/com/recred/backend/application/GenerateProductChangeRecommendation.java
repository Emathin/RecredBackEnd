package com.recred.backend.application;

import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.services.ProductChangeAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GenerateProductChangeRecommendation {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GenerateProductChangeRecommendation.class);
    private final ProductRepository productRepository;
    private final ProductChangeAnalyzer productChangeAnalyzer;
    private final AIEventRepository aiEventRepository;
    private final KiosqueroRepository kiosqueroRepository;
    private final GetProductStatistics getProductStatistics;

    public GenerateProductChangeRecommendation(
            ProductRepository productRepository,
            ProductChangeAnalyzer productChangeAnalyzer,
            AIEventRepository aiEventRepository,
            KiosqueroRepository kiosqueroRepository,
            GetProductStatistics getProductStatistics
    ) {
        this.productRepository = productRepository;
        this.productChangeAnalyzer = productChangeAnalyzer;
        this.aiEventRepository = aiEventRepository;
        this.kiosqueroRepository = kiosqueroRepository;
        this.getProductStatistics = getProductStatistics;
    }

    public void execute() {
        List<UUID> kiosqueroIds = kiosqueroRepository.findAllIds();
        kiosqueroIds.forEach(this::generateForKiosquero);
    }

    private void generateForKiosquero(UUID sellerId) {
        try {
            UUID buffetId = findBuffet(sellerId);
            UUID usuarioId = kiosqueroRepository.findUsuarioIdById(sellerId);
            generateForBuffet(buffetId, usuarioId);
        } catch (Exception e) {
            LOGGER.error("Error generating product change recommendation for kiosquero {}: {}",
                    sellerId, e.getMessage());
        }
    }

    private UUID findBuffet(UUID sellerId) {
        UUID buffetId = kiosqueroRepository.findBuffetById(sellerId);
        if (buffetId == null) {
            throw new KiosqueroNotFoundException(sellerId);
        }
        return buffetId;
    }

    private void generateForBuffet(UUID buffetId, UUID actorId) {
        List<ProductChangeRecommendation> products =
                productRepository.findBuffetTop5LeastSoldProducts(buffetId);
        products.forEach(product -> analyzeAndRegister(product, actorId));
    }

    private void analyzeAndRegister(ProductChangeRecommendation product, UUID actorId) {
        product.setSaleStatistics(getProductStatistics.execute(product.getId(), actorId));
        Optional<ProductChangeAnalysis> aiAnalysis = productChangeAnalyzer.analyze(product);
        ProductRecommendationResult result = new ProductRecommendationResult(product, aiAnalysis);
        aiEventRepository.registerProductChangeRecommendation(result);
    }
}
