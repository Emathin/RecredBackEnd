package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetActiveLevelsResponse;

import java.util.UUID;

public record LevelResponseDTO(
        UUID id,
        String descripcion,
        Boolean activo
) {

    public static LevelResponseDTO fromApplication(GetActiveLevelsResponse.Level level) {
        return new LevelResponseDTO(
                level.id(),
                level.descripcion(),
                level.activo()
        );
    }
}
