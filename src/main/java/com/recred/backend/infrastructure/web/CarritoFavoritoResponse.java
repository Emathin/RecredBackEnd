package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.CarritoFavorito;

import java.util.List;
import java.util.UUID;

public record CarritoFavoritoResponse(
        UUID id,
        String nombre,
        UUID alumnoId,
        String alumnoNombre,
        String alumnoApellido,
        List<CarritoFavoritoItemResponse> items
) {
    public static CarritoFavoritoResponse fromDomain(CarritoFavorito domain) {
        List<CarritoFavoritoItemResponse> itemResponses = domain.getItems() != null
                ? domain.getItems().entrySet().stream()
                        .map(entry -> new CarritoFavoritoItemResponse(
                                entry.getKey().getId(),
                                entry.getKey().getNombre(),
                                entry.getKey().getPrecio(),
                                entry.getValue()
                        ))
                        .toList()
                : List.of();

        return new CarritoFavoritoResponse(
                domain.getId(),
                domain.getNombre(),
                domain.getAlumno() != null ? domain.getAlumno().getId() : null,
                domain.getAlumno() != null ? domain.getAlumno().getNombre() : null,
                domain.getAlumno() != null ? domain.getAlumno().getApellido() : null,
                itemResponses
        );
    }
}
