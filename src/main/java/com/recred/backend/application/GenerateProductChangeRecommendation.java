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
import org.springframework.transaction.annotation.Transactional;

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
    public void execute() {
        List<UUID> kiosqueroIds = kiosqueroRepository.findAllIds();
        kiosqueroIds.forEach(id -> {
            try {
                this.execute(id);
            } catch (Exception e) {
                LOGGER.error("Error generating product change recommendation for kiosquero {}: {}",
                        id, e.getMessage());
            }
        });
    }

    @Transactional
    public List<ProductRecommendationResult> execute(UUID sellerId) {
        UUID buffetId = kiosqueroRepository.findBuffetById(sellerId);
        if (buffetId == null) {
            throw new KiosqueroNotFoundException(sellerId);
        }
        return generateForBuffet(buffetId);
    }

    @Transactional
    public List<ProductRecommendationResult> executeByUsuarioId(UUID usuarioId) {
        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(usuarioId);
        if (buffetId == null) {
            throw new KiosqueroNotFoundException(usuarioId);
        }
        return generateForBuffet(buffetId);
    }

    private List<ProductRecommendationResult> generateForBuffet(UUID buffetId) {
        List<ProductChangeRecommendation> leastSoldProducts =
                productRepository.findBuffetTop5LeastSoldProducts(buffetId);
        
        return leastSoldProducts.stream()
                .map(product -> {
                    Optional<ProductChangeAnalysis> aiAnalysis = productChangeAnalyzer
                            .analyze(product);
                    ProductRecommendationResult result = new ProductRecommendationResult(
                            product,
                            aiAnalysis
                    );
                    aiEventRepository.registerProductChangeRecommendation(result);
                    return result;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProductRecommendationResult> findLatestResult(UUID sellerId) {
        UUID buffetId = kiosqueroRepository.findBuffetById(sellerId);
        if (buffetId == null) {
            throw new KiosqueroNotFoundException(sellerId);
        }
        return aiEventRepository.findLatestProductChangeRecommendation(buffetId);
    }

    @Transactional(readOnly = true)
    public Optional<ProductRecommendationResult> findLatestResultByUsuarioId(UUID usuarioId) {
        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(usuarioId);
        if (buffetId == null) {
            throw new KiosqueroNotFoundException(usuarioId);
        }
        return aiEventRepository.findLatestProductChangeRecommendation(buffetId);
    }

    @Transactional(readOnly = true)
    public List<ProductRecommendationResult> findAllResultsByUsuarioId(UUID usuarioId) {
        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(usuarioId);
        if (buffetId == null) {
            throw new KiosqueroNotFoundException(usuarioId);
        }
        return aiEventRepository.findAllProductChangeRecommendations(buffetId);
    }

}
