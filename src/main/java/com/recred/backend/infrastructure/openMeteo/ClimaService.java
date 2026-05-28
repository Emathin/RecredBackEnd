package com.recred.backend.dominio;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.recred.backend.infrastructure.web.ClimaDTO;
import com.recred.backend.infrastructure.web.OpenMeteoResponseDTO;

@Service
public class ClimaService {
    private final RestClient restClient;

    public ClimaService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.open-meteo.com/v1")
                .build();
    }

    public ClimaDTO obtenerClimaActual(double latitud, double longitud) {
        OpenMeteoResponseDTO response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/forecast")
                        .queryParam("latitude", latitud)
                        .queryParam("longitude", longitud)
                        .queryParam("current", "temperature_2m,weather_code")
                        .build())
                .retrieve()
                .body(OpenMeteoResponseDTO.class);

        if (response == null || response.getCurrent() == null) {
            throw new RuntimeException("No se pudo obtener el clima de la API externa.");
        }

        String condicion = traducirCodigoClima(response.getCurrent().getWeatherCode());
        Double temperatura = response.getCurrent().getTemperature();

        return new ClimaDTO(temperatura, condicion);
    }

    private String traducirCodigoClima(Integer code) {
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
