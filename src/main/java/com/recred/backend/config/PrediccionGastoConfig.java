package com.recred.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.services.PrediccionGastoCalculator;
import com.recred.backend.domain.services.RangoPrediccionGastoService;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class PrediccionGastoConfig {

    @Bean
    public Clock applicationClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RangoPrediccionGastoService rangoPrediccionGastoService() {
        return new RangoPrediccionGastoService();
    }

    @Bean
    public PrediccionGastoCalculator prediccionGastoCalculator() {
        return new PrediccionGastoCalculator();
    }
}
