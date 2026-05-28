package com.recred.backend.infrastructure.openMeteo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.recred.backend.infrastructure.web.WeatherDTO;
import com.recred.backend.infrastructure.web.OpenMeteoResponseDTO;

@Service
public class OpenMeteoWeatherClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenMeteoWeatherClient.class);
    private static final String BASE_URL = "https://api.open-meteo.com/v1";

    private final RestClient restClient;

    public OpenMeteoWeatherClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(BASE_URL)
                .build();
    }

    public WeatherDTO getCurrentWeather(double latitude, double longitude) {
        try {
            OpenMeteoResponseDTO response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("current", "temperature_2m,weather_code")
                            .build())
                    .retrieve()
                    .body(OpenMeteoResponseDTO.class);

            if (response == null || response.getCurrent() == null) {
                throw new RuntimeException("Received empty response from Open-Meteo API.");
            }

            String condition = translateWeatherCode(response.getCurrent().getWeatherCode());
            Double temperature = response.getCurrent().getTemperature();

            return new WeatherDTO(temperature, condition);

        } catch (Exception exception) {
            LOGGER.error("Failed to fetch weather data for lat: {}, lon: {}",
                    latitude, longitude, exception);
            throw new RuntimeException("Weather service is currently unavailable", exception);
        }
    }

    private String translateWeatherCode(Integer code) {
        if (code == 0) {
            return "Despejado";
        }
        if (code > 0 && code <= 3) {
            return "Nublado";
        }
        if (code >= 51 && code <= 67) {
            return "Lluvia";
        }
        if (code >= 71 && code <= 77) {
            return "Frío Extremo";
        }
        if (code >= 95) {
            return "Tormenta";
        }
        return "Indeterminado";
    }
}
