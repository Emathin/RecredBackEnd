package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetBuffetByAlumnoResponse;
import java.util.UUID;

public record AlumnoBuffetResponseDTO(
        UUID id,
        String nombre,
        UUID colegioId
) {
    public static AlumnoBuffetResponseDTO fromResponse(GetBuffetByAlumnoResponse response) {
        return new AlumnoBuffetResponseDTO(
                response.id(),
                response.nombre(),
                response.colegioId()
        );
    }
}
