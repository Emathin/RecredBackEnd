package com.recred.backend.infrastructure.openMeteo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import com.recred.backend.infrastructure.web.GeocodingResponseDTO;

public class NominatimGeocodingClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(NominatimGeocodingClient.class);
    private static final String BASE_URL = "https://nominatim.openstreetmap.org";

    private final RestClient restClient;

    public NominatimGeocodingClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(BASE_URL)
                .defaultHeader("User-Agent", 
                        "RecredBackend/1.0 (contact@recred.com)") // Nominatim requires User-Agent
                .build();
    }

    public GeocodingResponseDTO getReverseGeocoding(double latitude, double longitude) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/reverse")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(GeocodingResponseDTO.class);
        } catch (Exception exception) {
            LOGGER.error("Failed to fetch geocoding data for lat: {}, lon: {}", 
                    latitude, longitude, exception);
            // Si falla, retornamos nulo o un dto vacío para que no rompa el flujo de sugerencias
            return null;
        }
    }
}
