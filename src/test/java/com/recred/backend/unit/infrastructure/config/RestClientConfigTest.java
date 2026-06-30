package com.recred.backend.unit.infrastructure.config;

import com.recred.backend.config.RestClientConfig;
import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RestClientConfigTest {

    @Test
    void shouldCreateRestClientBuilderAndWeatherClient() {
        RestClientConfig config = new RestClientConfig();

        RestClient.Builder builder = config.restClientBuilder();
        OpenMeteoWeatherClient client = config.openMeteoWeatherClient(builder);

        assertNotNull(builder);
        assertNotNull(client);
    }
}

