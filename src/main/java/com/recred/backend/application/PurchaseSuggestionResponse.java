package com.recred.backend.application;

import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import java.util.Map;
import java.util.UUID;

public record PurchaseSuggestionResponse(
        UUID id,
        UUID alumnoId,
        UUID buffetId,
        UUID productoId,
        ProductResponse producto,
        TipoSugerenciaIA tipo,
        String titulo,
        String mensaje,
        Map<String, Object> metadata
) {

    public static PurchaseSuggestionResponse fromDomain(SugerenciaIA suggestion) {
        return new PurchaseSuggestionResponse(
                suggestion.getId(),
                suggestion.getAlumnoId(),
                suggestion.getBuffetId(),
                suggestion.getProductoId(),
                suggestion.getProducto() == null
                        ? null
                        : ProductResponse.fromDomain(suggestion.getProducto()),
                suggestion.getTipo(),
                suggestion.getTitulo(),
                suggestion.getMensaje(),
                suggestion.getMetadata()
        );
    }
}
