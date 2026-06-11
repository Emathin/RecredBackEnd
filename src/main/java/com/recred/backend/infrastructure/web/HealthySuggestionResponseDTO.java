package com.recred.backend.infrastructure.web;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import com.recred.backend.domain.models.Producto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthySuggestionResponseDTO {
    private UUID sugerenciaId;
    private String titulo;
    private String mensaje;
    private Producto producto;
    private UUID alumnoId;
    private String type;
    private String rol;
}
