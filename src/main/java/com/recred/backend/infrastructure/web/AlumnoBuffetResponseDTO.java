package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Buffet;
import java.util.UUID;

public record AlumnoBuffetResponseDTO(
        UUID id,
        String nombre,
        UUID colegioId
) {
    public static AlumnoBuffetResponseDTO fromDomain(Buffet buffet) {
        return new AlumnoBuffetResponseDTO(
                buffet.getId(),
                buffet.getNombre(),
                buffet.getColegio() != null ? buffet.getColegio().getId() : null
        );
    }
}
