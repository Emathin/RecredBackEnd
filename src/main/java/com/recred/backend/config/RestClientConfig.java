package com.recred.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public OpenMeteoWeatherClient openMeteoWeatherClient(RestClient.Builder builder) {
        return new OpenMeteoWeatherClient(builder);
    }

}
