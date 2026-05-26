package com.recred.backend.config;

import com.recred.backend.application.GenerateProductChangeRecommendation;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseSpendingRepository;
import com.recred.backend.domain.services.ProductChangeAnalyzer;
import com.recred.backend.domain.services.SpendingPredictionRangeService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class ProductChangeRecommendationConfig {

    @Bean
    public GenerateProductChangeRecommendation generateProductChangeRecommendation(
            ProductRepository productRepository, ProductChangeAnalyzer productChangeAnalyzer,
            AIEventRepository aiEventRepository) {
        return new GenerateProductChangeRecommendation(
                productRepository,productChangeAnalyzer,aiEventRepository);
    }
}
