package com.recred.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.services.SpendingPredictionCalculator;
import com.recred.backend.domain.services.SpendingPredictionRangeService;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class SpendingPredictionConfig {

    @Bean
    public Clock applicationClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public SpendingPredictionRangeService spendingPredictionRangeService() {
        return new SpendingPredictionRangeService();
    }

    @Bean
    public SpendingPredictionCalculator spendingPredictionCalculator() {
        return new SpendingPredictionCalculator();
    }
}
