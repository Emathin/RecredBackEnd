package com.recred.backend.application;

import com.recred.backend.domain.models.TipoSugerenciaIA;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PurchaseSuggestIAResponse(
        SugerenciaIADTO sugerencia
) {
    public record SugerenciaIADTO(
            UUID id,
            UUID alumnoId,
            UUID buffetId,
            UUID productoId,
            ProductoDTO producto,
            TipoSugerenciaIA tipo,
            String titulo,
            String mensaje,
            Map<String, Object> metadata
    ) {}

    public record ProductoDTO(
            UUID id,
            String nombre,
            String urlImagen,
            BigDecimal precio,
            String descripcion,
            UUID categoriaId,
            String categoriaDescripcion,
            List<ClasificacionSaludDTO> clasificacionesSalud
    ) {}

    public record ClasificacionSaludDTO(
            UUID id,
            String descripcion
    ) {}
}
