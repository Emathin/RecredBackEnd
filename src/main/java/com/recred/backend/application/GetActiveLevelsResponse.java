package com.recred.backend.application;

import com.recred.backend.domain.models.Nivel;

import java.util.List;
import java.util.UUID;

public record GetActiveLevelsResponse(
        List<Level> levels
) {

    public static GetActiveLevelsResponse fromDomain(List<Nivel> levels) {
        return new GetActiveLevelsResponse(levels.stream()
                .map(Level::fromDomain)
                .toList());
    }

    public record Level(
            UUID id,
            String descripcion,
            Boolean activo
    ) {

        private static Level fromDomain(Nivel level) {
            return new Level(
                    level.getId(),
                    level.getDescripcion(),
                    level.getActivo() != null ? level.getActivo() : true);
        }
    }
}
