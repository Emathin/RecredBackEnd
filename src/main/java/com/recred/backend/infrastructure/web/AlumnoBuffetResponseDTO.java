package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetBuffetByStudentResponse;
import java.util.UUID;

public record AlumnoBuffetResponseDTO(
        UUID id,
        String nombre,
        UUID colegioId
) {
    public static AlumnoBuffetResponseDTO fromResponse(GetBuffetByStudentResponse response) {
        return new AlumnoBuffetResponseDTO(
                response.id(),
                response.nombre(),
                response.colegioId()
        );
    }
}
