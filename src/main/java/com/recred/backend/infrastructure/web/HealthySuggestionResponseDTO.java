package com.recred.backend.infrastructure.web;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthySuggestionResponseDTO {
    private UUID sugerenciaId;
    private String titulo;
    private String mensaje;
    private UUID productoId;
    private String productoNombre;
    private BigDecimal productoPrecio;
    private MenuProductoDTO producto;
    private String type;
    private String rol;
}
