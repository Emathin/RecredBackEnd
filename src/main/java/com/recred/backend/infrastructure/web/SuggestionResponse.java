package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.SugerenciaIA;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionResponse {
    private UUID id;
    private UUID alumnoId;
    private UUID buffetId;
    private UUID productoId;
    private String titulo;
    private String mensaje;
    private Map<String, Object> metadata;

    public static SuggestionResponse fromDomain(SugerenciaIA suggestion) {
        return SuggestionResponse.builder()
                .id(suggestion.getId())
                .alumnoId(suggestion.getAlumnoId())
                .buffetId(suggestion.getBuffetId())
                .productoId(suggestion.getProductoId())
                .titulo(suggestion.getTitulo())
                .mensaje(suggestion.getMensaje())
                .metadata(suggestion.getMetadata())
                .build();
    }
}
