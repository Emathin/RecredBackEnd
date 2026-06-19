package com.recred.backend.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.services.SpendingPredictionCalculator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

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
    public SpendingPredictionCalculator spendingPredictionCalculator() {
        return new SpendingPredictionCalculator();
    }
}
